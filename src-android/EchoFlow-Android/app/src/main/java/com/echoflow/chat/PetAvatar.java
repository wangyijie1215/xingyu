package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 角色 Q 版头像的生成与缓存。
 *
 * 之前桌宠是一颗手绘的球 —— 能弹，但和角色没关系，你说「太丑"是对的。
 * 现在改成：**用角色的 Q 版形象做桌宠**。
 *
 * 生成方式：Pollinations 免费生图（复用 ImageGen），提示词写死成
 * "chibi / 三头身 / 大眼 / 透明感浅色背景"，保证是 Q 版而不是写实。
 *
 * 缓存优先：第一次生成后存盘，之后直接读。
 * 如果还没生成过，绘制时先用角色头像裁圆兜底，不会出现「桌宠是空的"。
 */
public class PetAvatar {

    private static final String SUFFIX = "_q";

    /**
     * 取 Q 版头像；没有则返回 null（调用方用圆形头像兜底）。
     */
    public static Bitmap load(Context ctx, String cardId) {
        File f = cacheFile(ctx, cardId);
        if (!f.exists()) {
            return null;
        }
        return android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
    }

    public static boolean has(Context ctx, String cardId) {
        return cacheFile(ctx, cardId).exists();
    }

    public static File cacheFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "pet");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + SUFFIX + ".png");
    }

    /**
     * 生成 Q 版头像。生成完存成 PNG（透明底不是必须的，圆形裁切由绘制端做）。
     */
    public static void generate(Context ctx, CharacterCard card,
                                final GenerateCallback cb) {
        if (card == null) {
            cb.onError("没有角色");
            return;
        }
        String prompt = buildPrompt(card);
        // 用 cardId 做 key，保证同一角色每次生成的是同一张
        //
        // 走 ImageProvider（而不是老的 ImageGen）：
        // 后者是接入多渠道之前留下的重复实现，只支持 Pollinations。
        // 统一之后，桌宠的这个 Q 版生成也能用上本地 ComfyUI 了。
        final ImageProvider.Config cfg = ImageProvider.load(ctx);
        final File cache = ImageProvider.cacheFile(ctx, "petq_" + card.id);
        final int seed = Math.abs(("petq_" + card.id).hashCode()) % 1000000;

        new Thread(() -> {
            try {
                Bitmap bmp = ImageProvider.generate(ctx, cfg, prompt, null, seed, 384, 384);
                // 转存到 pet 目录
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cache)) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                cb.onDone(bmp);
            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? "生成失败" : e.getMessage());
            }
        }).start();
    }

    /** Q 版提示词：把角色名和性格揉进去，但风格词固定 */
    private static String buildPrompt(CharacterCard card) {
        StringBuilder sb = new StringBuilder();
        sb.append("chibi style, super deformed, 3 heads tall, big head, ");
        sb.append("cute anime character portrait, ");
        sb.append(safe(card.name)).append(", ");
        if (card.personality != null && !card.personality.isEmpty()) {
            sb.append(safe(card.personality)).append(", ");
        }
        if (card.tags != null && !card.tags.isEmpty()) {
            StringBuilder tagStr = new StringBuilder();
            for (int i = 0; i < Math.min(3, card.tags.size()); i++) {
                if (i > 0) {
                    tagStr.append(", ");
                }
                tagStr.append(safe(card.tags.get(i)));
            }
            sb.append(tagStr).append(", ");
        }
        sb.append("soft pastel colors, kawaii, ");
        sb.append("simple flat background, centered, full body, ");
        sb.append("masterpiece, best quality, cute expression");
        return sb.toString();
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("[\\r\\n\\t#]", " ").trim();
        if (t.length() > 60) {
            t = t.substring(0, 60);
        }
        return t;
    }

    public static void delete(Context ctx, String cardId) {
        File f = cacheFile(ctx, cardId);
        if (f.exists()) {
            f.delete();
        }
    }

    public interface GenerateCallback {
        void onDone(Bitmap bmp);

        void onError(String msg);
    }
}