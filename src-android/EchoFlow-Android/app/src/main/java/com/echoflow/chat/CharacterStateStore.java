package com.echoflow.chat;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 角色状态存储：关系 + 情绪 + 统计。filesDir/states/{cardId}.json
 */
public class CharacterStateStore {

    private static File stateFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "states");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, cardId + ".json");
    }

    public static RelationshipState loadRelationship(Context ctx, String cardId) {
        JSONObject root = read(ctx, cardId);
        if (root != null && root.has("relationship")) {
            return RelationshipState.fromJson(root.optJSONObject("relationship"));
        }
        RelationshipState r = new RelationshipState();
        r.cardId = cardId;
        return r;
    }

    public static EmotionState loadEmotion(Context ctx, String cardId) {
        JSONObject root = read(ctx, cardId);
        if (root != null && root.has("emotion")) {
            return EmotionState.fromJson(root.optJSONObject("emotion"));
        }
        EmotionState e = new EmotionState();
        e.cardId = cardId;
        e.mood = "平静";
        e.intensity = 1;
        return e;
    }

    public static void saveRelationship(Context ctx, RelationshipState r) {
        JSONObject root = read(ctx, r.cardId);
        if (root == null) root = new JSONObject();
        try {
            root.put("relationship", r.toJson());
            write(ctx, r.cardId, root);
        } catch (Exception ignore) {
        }
    }

    public static void saveEmotion(Context ctx, EmotionState e) {
        JSONObject root = read(ctx, e.cardId);
        if (root == null) root = new JSONObject();
        try {
            root.put("emotion", e.toJson());
            write(ctx, e.cardId, root);
        } catch (Exception ignore) {
        }
    }

    private static JSONObject read(Context ctx, String cardId) {
        File f = stateFile(ctx, cardId);
        if (!f.exists()) return null;
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) sb.append(buf, 0, n);
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static void write(Context ctx, String cardId, JSONObject o) throws Exception {
        try (FileWriter fw = new FileWriter(stateFile(ctx, cardId))) {
            fw.write(o.toString());
        }
    }
}
