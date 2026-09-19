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
 * 事件簿存储。filesDir/events/{cardId}.json
 */
public class EventStore {

    private static File eventFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "events");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, cardId + ".json");
    }

    public static List<StoryEvent> list(Context ctx, String cardId) {
        List<StoryEvent> out = new ArrayList<>();
        File f = eventFile(ctx, cardId);
        if (!f.exists()) return out;
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) sb.append(buf, 0, n);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                out.add(StoryEvent.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignore) {
        }
        Collections.sort(out, (a, b) -> Long.compare(b.date, a.date));
        return out;
    }

    public static void add(Context ctx, String cardId, StoryEvent e) {
        List<StoryEvent> all = list(ctx, cardId);
        all.add(e);
        save(ctx, cardId, all);
    }

    public static void save(Context ctx, String cardId, List<StoryEvent> all) {
        JSONArray arr = new JSONArray();
        for (StoryEvent e : all) {
            try {
                arr.put(e.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(eventFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    public static void delete(Context ctx, String cardId, String id) {
        List<StoryEvent> all = list(ctx, cardId);
        all.removeIf(e -> e.id.equals(id));
        save(ctx, cardId, all);
    }
}
