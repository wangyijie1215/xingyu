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
 * 信笺存储。filesDir/letters/{cardId}.json
 */
public class LetterStore {

    private static File letterFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "letters");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + ".json");
    }

    public static List<Letter> list(Context ctx, String cardId) {
        List<Letter> out = new ArrayList<>();
        File f = letterFile(ctx, cardId);
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
                    out.add(Letter.fromJson(o));
                }
            }
        } catch (Exception ignore) {
        }
        Collections.sort(out, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        return out;
    }

    public static void save(Context ctx, String cardId, Letter letter) {
        List<Letter> all = list(ctx, cardId);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(letter.id)) {
                all.set(i, letter);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(letter);
        }
        if (all.size() > 200) {
            Collections.sort(all, (a, b) -> Long.compare(b.createdAt, a.createdAt));
            all = new ArrayList<>(all.subList(0, 200));
        }
        JSONArray arr = new JSONArray();
        for (Letter l : all) {
            try {
                arr.put(l.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(letterFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    public static void markRead(Context ctx, String cardId, String letterId) {
        List<Letter> all = list(ctx, cardId);
        for (Letter l : all) {
            if (l.id.equals(letterId)) {
                l.read = true;
                break;
            }
        }
        JSONArray arr = new JSONArray();
        for (Letter l : all) {
            try {
                arr.put(l.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(letterFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    /** 未拆信数 —— 首页与「我的"用红点提示 */
    public static int unreadCount(Context ctx, String cardId) {
        int n = 0;
        for (Letter l : list(ctx, cardId)) {
            if (l.fromHer() && !l.read) {
                n++;
            }
        }
        return n;
    }

    public static Letter get(Context ctx, String cardId, String id) {
        for (Letter l : list(ctx, cardId)) {
            if (l.id.equals(id)) {
                return l;
            }
        }
        return null;
    }
}