package com.echoflow.chat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 时间胶囊存储。filesDir/capsules/{cardId}.json
 */
public class CapsuleStore {

    private static File capsuleFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "capsules");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + ".json");
    }

    public static List<TimeCapsule> list(Context ctx, String cardId) {
        List<TimeCapsule> out = new ArrayList<>();
        File f = capsuleFile(ctx, cardId);
        if (!f.exists()) {
            return out;
        }
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    out.add(TimeCapsule.fromJson(o));
                }
            }
        } catch (Exception ignore) {
        }
        Collections.sort(out, (a, b) -> (a.openAt == null || b.openAt == null)
                ? 0 : a.openAt.compareTo(b.openAt));
        return out;
    }

    public static void save(Context ctx, String cardId, TimeCapsule c) {
        List<TimeCapsule> all = list(ctx, cardId);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(c.id)) {
                all.set(i, c);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(c);
        }
        JSONArray arr = new JSONArray();
        for (TimeCapsule x : all) {
            try {
                arr.put(x.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(capsuleFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    public static void delete(Context ctx, String cardId, String id) {
        List<TimeCapsule> all = list(ctx, cardId);
        all.removeIf(c -> c.id.equals(id));
        JSONArray arr = new JSONArray();
        for (TimeCapsule x : all) {
            try {
                arr.put(x.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(capsuleFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    /**
     * ★ 给 Prompt 用的胶囊线索：只取「有一个胶囊存在"这件事本身，
     *   **绝不返回 content**。她可以说「我也有一个胶囊要给你"，
     *   但不能提前知道里面写了什么。
     */
    public static List<String> sealedTeasers(Context ctx, String cardId) {
        List<String> out = new ArrayList<>();
        for (TimeCapsule c : list(ctx, cardId)) {
            if (c.isSealed() && c.teaser) {
                out.add(c.author + " 在 " + c.openAt + " 留了一个胶囊（内容封存，你也不知道里面是什么）");
            }
        }
        return out;
    }
}