package com.echoflow.chat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 漂流瓶的持久化。
 *
 * 三类瓶子：
 *   1. 捡到的（陌生人的）—— 从内置文案库随机取
 *   2. 角色扔的 —— 让她现写一句给陌生人，这是这个功能最有意思的部分
 *   3. 自己扔的 —— 用户写的，扔出去后捡不回来
 *
 * 存储：
 *   files/bottle/collected.json   收藏的
 *   files/bottle/mine.json        自己扔的
 */
public class BottleStore {

    public static class Item {
        public String text = "";
        public String tag = "blessing";
        public String from = "";
        public String fromCardId = "";
        public long time = 0;

        public Item() {}

        public Item(String text, String tag, String from) {
            this.text = text;
            this.tag = tag;
            this.from = from;
            this.time = System.currentTimeMillis();
        }
    }

    private static File dir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "bottle");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    private static File file(Context ctx, String name) {
        return new File(dir(ctx), name + ".json");
    }

    private static List<Item> load(Context ctx, String name) {
        List<Item> out = new ArrayList<>();
        File f = file(ctx, name);
        if (!f.exists()) {
            return out;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (FileReader fr = new FileReader(f)) {
                char[] buf = new char[4096];
                int n;
                while ((n = fr.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                Item it = new Item();
                it.text = o.optString("text", "");
                it.tag = o.optString("tag", "blessing");
                it.from = o.optString("from", "");
                it.fromCardId = o.optString("fromCardId", "");
                it.time = o.optLong("time", 0);
                out.add(it);
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    private static void save(Context ctx, String name, List<Item> items) {
        JSONArray arr = new JSONArray();
        for (Item it : items) {
            try {
                JSONObject o = new JSONObject();
                o.put("text", it.text);
                o.put("tag", it.tag);
                o.put("from", it.from == null ? "" : it.from);
                o.put("fromCardId", it.fromCardId == null ? "" : it.fromCardId);
                o.put("time", it.time);
                arr.put(o);
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(file(ctx, name))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    // ==================================================================
    // 收藏
    // ==================================================================

    public static List<Item> collected(Context ctx) {
        return load(ctx, "collected");
    }

    public static void collect(Context ctx, Item it) {
        List<Item> list = load(ctx, "collected");
        for (Item e : list) {
            if (e.text != null && e.text.equals(it.text)) {
                return;   // 同一段文案只留一条
            }
        }
        it.time = System.currentTimeMillis();
        list.add(0, it);
        save(ctx, "collected", list);
    }

    public static void uncollect(Context ctx, String text) {
        List<Item> next = new ArrayList<>();
        for (Item e : load(ctx, "collected")) {
            if (e.text == null || !e.text.equals(text)) {
                next.add(e);
            }
        }
        save(ctx, "collected", next);
    }

    public static boolean isCollected(Context ctx, String text) {
        for (Item e : load(ctx, "collected")) {
            if (e.text != null && e.text.equals(text)) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // 自己扔的
    // ==================================================================

    public static List<Item> mine(Context ctx) {
        return load(ctx, "mine");
    }

    public static void throwBottle(Context ctx, String text, String tag) {
        List<Item> list = load(ctx, "mine");
        list.add(0, new Item(text, tag, null));
        save(ctx, "mine", list);
    }

    public static void deleteMine(Context ctx, int index) {
        List<Item> list = load(ctx, "mine");
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save(ctx, "mine", list);
        }
    }

    // ==================================================================
    // 捡瓶子
    // ==================================================================

    /**
     * 捡一个瓶子。
     *
     * 约 1/4 概率捡到“角色扔的”（如果已有角色）；其余是内置文案。
     * 角色瓶子的正文由调用方用 LLM 生成 —— 这里只给出谁是扔瓶人。
     */
    public static Item pick(Context ctx) {
        if (Math.random() < 0.25) {
            List<CharacterCard> cards = CardStore.listCards(ctx);
            if (!cards.isEmpty()) {
                CharacterCard c = cards.get((int) (Math.random() * cards.size()));
                BottleText.Bottle b = BottleText.random();
                Item it = new Item(b.text, b.tag, c.name);
                it.fromCardId = c.id;
                return it;
            }
        }
        BottleText.Bottle b = BottleText.random();
        return new Item(b.text, b.tag, null);
    }

    public static int countAll(Context ctx) {
        return collected(ctx).size() + mine(ctx).size();
    }
}
