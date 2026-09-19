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
 * 长期记忆存储。filesDir/memories/{cardId}.json
 */
public class MemoryStore {

    private static File memFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "memories");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, cardId + ".json");
    }

    public static List<Memory> list(Context ctx, String cardId) {
        List<Memory> out = new ArrayList<>();
        File f = memFile(ctx, cardId);
        if (!f.exists()) return out;
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) sb.append(buf, 0, n);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                out.add(Memory.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignore) {
        }
        // 重要度高的在前
        Collections.sort(out, (a, b) -> Integer.compare(b.importance, a.importance));
        return out;
    }

    /** 取启用的、最重要的 N 条，用于注入 Prompt */
    public static List<Memory> topForPrompt(Context ctx, String cardId, int n) {
        List<Memory> all = list(ctx, cardId);
        List<Memory> out = new ArrayList<>();
        for (Memory m : all) {
            if (!m.enabled) continue;
            out.add(m);
            if (out.size() >= n) break;
        }
        return out;
    }

    public static void save(Context ctx, String cardId, List<Memory> all) {
        JSONArray arr = new JSONArray();
        for (Memory m : all) {
            try {
                arr.put(m.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(memFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    public static void add(Context ctx, String cardId, Memory m) {
        List<Memory> all = list(ctx, cardId);
        // 去重：内容高度相似则不重复加
        for (Memory existing : all) {
            if (existing.content != null && existing.content.equals(m.content)) {
                return;
            }
        }
        all.add(m);
        // 限制最多 200 条，超过删最不重要的
        if (all.size() > 200) {
            Collections.sort(all, (a, b) -> Integer.compare(b.importance, a.importance));
            all = new ArrayList<>(all.subList(0, 200));
        }
        save(ctx, cardId, all);
    }

    public static void delete(Context ctx, String cardId, String id) {
        List<Memory> all = list(ctx, cardId);
        all.removeIf(m -> m.id.equals(id));
        save(ctx, cardId, all);
    }
}
