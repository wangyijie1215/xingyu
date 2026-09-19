package com.echoflow.chat;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 内置角色引导。
 *
 * 为什么需要：装完 App 打开是空的，用户得自己造角色才能用 —— 这是产品缺陷。
 * 首次启动时把 assets 里的角色卡与头像装进本地，用户一进来就有内容。
 *
 * 设计约束：
 *  · 只装一次（filesDir/.builtin_v1 标记），不覆盖用户后续的修改
 *  · 用户删掉内置角色后不会复活（标记已存在）
 *  · 不写 SharedPreferences，避免与用户配置混在一起
 */
public class BuiltinCharacters {

    private static final String TAG = "BuiltinCharacters";
    /** 版本标记：将来加新角色时递增，可只装增量 */
    private static final String MARKER = ".builtin_v1";

    /** 内置角色清单（顺序即角色库里的展示顺序） */
    private static final String[] CARD_FILES = {
            "cards/builtin_alice.json",
            "cards/builtin_rin.json",
            "cards/builtin_hakuyo.json",
            "cards/builtin_rocco.json",
            "cards/builtin_elian.json",
            "cards/builtin_yueling.json",
    };

    /**
     * 首次启动时安装内置角色。可安全重复调用。
     *
     * @return 本次实际安装的数量
     */
    public static int installIfNeeded(Context ctx) {
        File marker = new File(ctx.getFilesDir(), MARKER);
        if (marker.exists()) {
            return 0;
        }
        int n = 0;
        AssetManager am = ctx.getAssets();
        for (String path : CARD_FILES) {
            try {
                String json = readAsset(am, path);
                if (json == null || json.trim().isEmpty()) {
                    continue;
                }
                JSONObject root = new JSONObject(json);
                CharacterCard card = CharacterCard.fromLocalJson(root);
                if (card.id == null || card.id.isEmpty()) {
                    continue;
                }
                // 已存在同 id 就跳过（不覆盖用户改动）
                if (CardStore.getCard(ctx, card.id) != null) {
                    continue;
                }
                CardStore.saveCard(ctx, card);

                // 头像：从 assets 读出来，按 PNG 存到 cards/{id}.png
                String avatar = root.optString("_avatar", "");
                if (!avatar.isEmpty()) {
                    Bitmap bmp = decodeAsset(am, avatar);
                    if (bmp != null) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                        CardStore.saveAvatar(ctx, card.id, bos.toByteArray());
                        bmp.recycle();
                    }
                }
                n++;
            } catch (Exception e) {
                Log.w(TAG, "内置角色安装失败: " + path, e);
            }
        }
        // 无论成功几个都打标记，避免每次启动重试同一个坏文件
        try {
            if (!marker.exists()) {
                if (!marker.createNewFile()) {
                    Log.w(TAG, "无法创建内置角色标记文件");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "写标记失败", e);
        }
        Log.i(TAG, "已安装内置角色 " + n + " 个");
        return n;
    }

    /** 供「我的 → 恢复内置角色"使用：清掉标记再装一遍 */
    public static int reinstall(Context ctx) {
        File marker = new File(ctx.getFilesDir(), MARKER);
        marker.delete();
        return installIfNeeded(ctx);
    }

    /** 内置角色的 id 列表，用于界面标注「内置" */
    public static List<String> builtinIds() {
        List<String> out = new ArrayList<>();
        out.add("builtin_alice");
        out.add("builtin_rin");
        out.add("builtin_hakuyo");
        out.add("builtin_rocco");
        out.add("builtin_elian");
        out.add("builtin_yueling");
        return out;
    }

    public static boolean isBuiltin(String cardId) {
        return cardId != null && cardId.startsWith("builtin_");
    }

    // ------------------------------------------------------------------

    private static String readAsset(AssetManager am, String path) {
        try (InputStream is = am.open(path)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decodeAsset(AssetManager am, String path) {
        try (InputStream is = am.open(path)) {
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            return null;
        }
    }
}