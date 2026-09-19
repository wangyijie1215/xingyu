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
 * 世界书（Lorebook）。
 *
 * 这是角色扮演里最实用的一类设定载体：**一堆按关键词触发的小设定条目**。
 * 聊天里说到相关词，对应条目才被注入 Prompt；没说到就不占上下文。
 *
 * 与角色卡的 `system_prompt` 的分工：
 *   · 角色卡写"她是谁"（长期不变的人设）
 *   · 世界书写"她所在的世界是什么样的"（可切换、按需注入）
 *
 * 一本世界书挂在一个角色上，也可以多个角色共用同一本。
 */
public class Lorebook {

    public static class Entry {
        /** 触发关键词（命中任一即注入） */
        public List<String> keys = new ArrayList<>();
        /** 注入的正文 */
        public String content = "";
        /** 备注（给自己看的） */
        public String comment = "";
        /** 是否常驻（不靠关键词，永远注入） */
        public boolean constant;
        /** 概率加权：数字越大越容易被选中（当前实现：全部命中的都注入） */
        public int order = 100;

        public Entry() {}

        public Entry(String comment, String[] keys, String content, boolean constant) {
            this.comment = comment;
            this.content = content;
            this.constant = constant;
            for (String k : keys) {
                this.keys.add(k);
            }
        }
    }

    public String id = "";
    public String name = "";
    public String description = "";
    /** 是否所有角色共用（全局世界书） */
    public boolean global = false;
    public List<Entry> entries = new ArrayList<>();

    // ==================================================================
    // 触发
    // ==================================================================

    /**
     * 从最近对话里挑出该注入的条目。
     *
     * 规则：
     *  · constant 条目永远注入
     *  · 其余条目：最近 scanDepth 条消息里出现任一关键词就注入
     *  · 每次最多注入 maxEntries 条，按 order 降序（保证重要的先进）
     */
    public List<Entry> triggered(List<Message> recent, int scanDepth, int maxEntries) {
        List<Entry> out = new ArrayList<>();
        StringBuilder hay = new StringBuilder();
        int start = Math.max(0, recent.size() - scanDepth);
        for (int i = start; i < recent.size(); i++) {
            Message m = recent.get(i);
            if (m.content != null) {
                hay.append(m.content).append('\n');
            }
        }
        String text = hay.toString().toLowerCase();

        List<Entry> hits = new ArrayList<>();
        for (Entry e : entries) {
            if (e.content == null || e.content.trim().isEmpty()) {
                continue;
            }
            if (e.constant) {
                hits.add(e);
                continue;
            }
            for (String k : e.keys) {
                if (k == null || k.trim().isEmpty()) {
                    continue;
                }
                if (text.contains(k.toLowerCase())) {
                    hits.add(e);
                    break;
                }
            }
        }

        // order 大的优先
        hits.sort((a, b) -> Integer.compare(b.order, a.order));
        for (int i = 0; i < Math.min(maxEntries, hits.size()); i++) {
            out.add(hits.get(i));
        }
        return out;
    }

    /** 组装成一段可直接塞进 system 的文本 */
    public String toPromptSection(List<Entry> triggered) {
        if (triggered == null || triggered.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【世界设定 · ").append(name).append("】\n");
        for (Entry e : triggered) {
            sb.append("- ").append(e.content.trim()).append('\n');
        }
        return sb.toString();
    }

    // ==================================================================
    // 持久化
    // ==================================================================

    private static File dir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "lorebook");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    private static File file(Context ctx, String id) {
        return new File(dir(ctx), id + ".json");
    }

    public static void save(Context ctx, Lorebook lb) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", lb.id);
            o.put("name", lb.name);
            o.put("description", lb.description);
            o.put("global", lb.global);
            JSONArray arr = new JSONArray();
            for (Entry e : lb.entries) {
                JSONObject eo = new JSONObject();
                eo.put("comment", e.comment);
                eo.put("content", e.content);
                eo.put("constant", e.constant);
                eo.put("order", e.order);
                JSONArray ks = new JSONArray();
                for (String k : e.keys) {
                    ks.put(k);
                }
                eo.put("keys", ks);
                arr.put(eo);
            }
            o.put("entries", arr);
        } catch (Exception ignore) {
        }
        try (FileWriter fw = new FileWriter(file(ctx, lb.id))) {
            fw.write(o.toString());
        } catch (Exception ignore) {
        }
    }

    public static Lorebook load(Context ctx, String id) {
        File f = file(ctx, id);
        if (!f.exists()) {
            return null;
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
            return fromJson(new JSONObject(sb.toString()));
        } catch (Exception e) {
            return null;
        }
    }

    static Lorebook fromJson(JSONObject o) {
        Lorebook lb = new Lorebook();
        lb.id = o.optString("id", "");
        lb.name = o.optString("name", "");
        lb.description = o.optString("description", "");
        lb.global = o.optBoolean("global", false);
        JSONArray arr = o.optJSONArray("entries");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject eo = arr.optJSONObject(i);
                if (eo == null) {
                    continue;
                }
                Entry e = new Entry();
                e.comment = eo.optString("comment", "");
                e.content = eo.optString("content", "");
                e.constant = eo.optBoolean("constant", false);
                e.order = eo.optInt("order", 100);
                JSONArray ks = eo.optJSONArray("keys");
                if (ks != null) {
                    for (int j = 0; j < ks.length(); j++) {
                        e.keys.add(ks.optString(j, ""));
                    }
                }
                lb.entries.add(e);
            }
        }
        return lb;
    }

    /** 列出全部世界书 */
    public static List<Lorebook> list(Context ctx) {
        List<Lorebook> out = new ArrayList<>();
        File[] files = dir(ctx).listFiles();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            if (!f.getName().endsWith(".json")) {
                continue;
            }
            Lorebook lb = load(ctx, f.getName().replace(".json", ""));
            if (lb != null) {
                out.add(lb);
            }
        }
        return out;
    }

    public static void delete(Context ctx, String id) {
        File f = file(ctx, id);
        if (f.exists()) {
            f.delete();
        }
    }

    // ==================================================================
    // 角色 ↔ 世界书 关联
    // ==================================================================

    private static File bindFile(Context ctx) {
        return new File(dir(ctx), "_bindings.json");
    }

    /** 取某角色启用的世界书 id 列表（含全局的） */
    public static List<String> bindingsFor(Context ctx, String cardId) {
        List<String> out = new ArrayList<>();
        for (Lorebook lb : list(ctx)) {
            if (lb.global) {
                out.add(lb.id);
            }
        }
        try {
            File f = bindFile(ctx);
            if (f.exists()) {
                StringBuilder sb = new StringBuilder();
                try (FileReader fr = new FileReader(f)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = fr.read(buf)) > 0) {
                        sb.append(buf, 0, n);
                    }
                }
                JSONObject root = new JSONObject(sb.toString());
                JSONArray ids = root.optJSONArray(cardId);
                if (ids != null) {
                    for (int i = 0; i < ids.length(); i++) {
                        String id = ids.optString(i, "");
                        if (!id.isEmpty() && !out.contains(id)) {
                            out.add(id);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return out;
    }

    public static void bind(Context ctx, String cardId, String lorebookId, boolean on) {
        try {
            JSONObject root = new JSONObject();
            File f = bindFile(ctx);
            if (f.exists()) {
                StringBuilder sb = new StringBuilder();
                try (FileReader fr = new FileReader(f)) {
                    char[] buf = new char[4096];
                    int n;
                    while ((n = fr.read(buf)) > 0) {
                        sb.append(buf, 0, n);
                    }
                }
                root = new JSONObject(sb.toString());
            }
            List<String> ids = new ArrayList<>();
            JSONArray cur = root.optJSONArray(cardId);
            if (cur != null) {
                for (int i = 0; i < cur.length(); i++) {
                    ids.add(cur.optString(i, ""));
                }
            }
            if (on) {
                if (!ids.contains(lorebookId)) {
                    ids.add(lorebookId);
                }
            } else {
                ids.remove(lorebookId);
            }
            JSONArray next = new JSONArray();
            for (String s : ids) {
                next.put(s);
            }
            root.put(cardId, next);
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(root.toString());
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 给 Prompt 用的完整世界书段落（把该角色绑定的所有世界书的命中条目合起来）。
     */
    public static String buildPrompt(Context ctx, String cardId, List<Message> recent) {
        List<String> ids = bindingsFor(ctx, cardId);
        if (ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            Lorebook lb = load(ctx, id);
            if (lb == null) {
                continue;
            }
            List<Entry> hit = lb.triggered(recent, 8, 6);
            sb.append(lb.toPromptSection(hit));
        }
        return sb.toString();
    }

    /**
     * 取"当前世界"——用于天气等需要世界背景的功能。
     * 优先返回角色绑定的第一本非全局世界书。
     */
    public static Lorebook currentWorld(Context ctx, String cardId) {
        for (String id : bindingsFor(ctx, cardId)) {
            Lorebook lb = load(ctx, id);
            if (lb != null && !lb.global) {
                return lb;
            }
        }
        List<Lorebook> all = list(ctx);
        for (Lorebook lb : all) {
            if (!lb.global) {
                return lb;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }
}
