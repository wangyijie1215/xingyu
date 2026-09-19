package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 角色卡与 Persona 的本地文件存储。
 * 存储位置: filesDir/cards/{id}.json + {id}.png; personas.json
 */
public class CardStore {

    private static File cardsDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "cards");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    private static File personaFile(Context ctx) {
        return new File(ctx.getFilesDir(), "personas.json");
    }

    public static List<CharacterCard> listCards(Context ctx) {
        List<CharacterCard> list = new ArrayList<>();
        File[] files = cardsDir(ctx).listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    list.add(fromFile(f));
                } catch (Exception ignore) {
                }
            }
        }
        Collections.sort(list, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return list;
    }

    public static CharacterCard getCard(Context ctx, String id) {
        File f = new File(cardsDir(ctx), id + ".json");
        if (!f.exists()) {
            return null;
        }
        try {
            return fromFile(f);
        } catch (Exception e) {
            return null;
        }
    }

    private static CharacterCard fromFile(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        }
        return CharacterCard.fromLocalJson(new JSONObject(sb.toString()));
    }

    public static void saveCard(Context ctx, CharacterCard card) throws Exception {
        if (card.id == null || card.id.isEmpty()) {
            card.id = "c" + System.currentTimeMillis();
        }
        card.updatedAt = System.currentTimeMillis();
        File f = new File(cardsDir(ctx), card.id + ".json");
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(card.toLocalJson().toString());
        }
    }

    public static void deleteCard(Context ctx, String id) {
        File json = new File(cardsDir(ctx), id + ".json");
        File img = new File(cardsDir(ctx), id + ".png");
        json.delete();
        img.delete();
        // 聊天记录也清掉
        File chat = new File(ctx.getFilesDir(), "chats/" + id + ".json");
        chat.delete();
    }

    public static void saveAvatar(Context ctx, String id, byte[] pngBytes) {
        try {
            File f = new File(cardsDir(ctx), id + ".png");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(pngBytes);
                fos.flush();
            }
        } catch (Exception ignore) {
        }
    }

    public static Bitmap loadAvatar(Context ctx, String id) {
        File f = new File(cardsDir(ctx), id + ".png");
        if (f.exists()) {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        }
        return null;
    }

    /** 导出角色卡为 V2 JSON 字节 */
    public static byte[] exportCardJson(CharacterCard card) {
        try {
            return card.toV2Json().toString(2).getBytes("UTF-8");
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ---- Persona ----

    public static List<Persona> listPersonas(Context ctx) {
        File f = personaFile(ctx);
        if (!f.exists()) {
            // 默认 persona
            Persona def = new Persona();
            def.id = "default";
            def.name = "用户";
            def.description = "";
            List<Persona> list = new ArrayList<>();
            list.add(def);
            return list;
        }
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        } catch (Exception ignore) {
        }
        return Persona.listFromJson(sb.toString());
    }

    public static void savePersonas(Context ctx, List<Persona> list) {
        try (FileWriter fw = new FileWriter(personaFile(ctx))) {
            fw.write(Persona.listToJson(list));
        } catch (Exception ignore) {
        }
    }
}
