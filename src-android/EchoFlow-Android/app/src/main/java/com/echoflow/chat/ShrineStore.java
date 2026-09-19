package com.echoflow.chat;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 神社状态存储。filesDir/shrine/{cardId}.json
 */
public class ShrineStore {

    private static File shrineFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "shrine");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + ".json");
    }

    public static ShrineState load(Context ctx, String cardId) {
        File f = shrineFile(ctx, cardId);
        if (!f.exists()) {
            ShrineState s = new ShrineState();
            s.cardId = cardId;
            // 三种御守为初始持有
            s.unlocked.add("enmusubi");
            s.unlocked.add("shizune");
            s.unlocked.add("yumemi");
            s.equipped = "enmusubi";
            return s;
        }
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            return ShrineState.fromJson(new JSONObject(sb.toString()));
        } catch (Exception e) {
            ShrineState s = new ShrineState();
            s.cardId = cardId;
            s.unlocked.add("enmusubi");
            s.unlocked.add("shizune");
            s.unlocked.add("yumemi");
            s.equipped = "enmusubi";
            return s;
        }
    }

    public static void save(Context ctx, ShrineState s) {
        try (FileWriter fw = new FileWriter(shrineFile(ctx, s.cardId))) {
            fw.write(s.toJson().toString());
        } catch (Exception ignore) {
        }
    }

    /** 佩戴 / 卸下御守 */
    public static void equip(Context ctx, ShrineState s, String omamoriId) {
        s.equipped = omamoriId == null ? "" : omamoriId;
        save(ctx, s);
    }
}
