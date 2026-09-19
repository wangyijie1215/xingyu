package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 桌宠的开关状态。
 * 放在 SharedPreferences 而不是文件 —— 就一个布尔值，用不着建文件。
 */
public class PetStore {

    private static final String PREFS = "echoflow_pet";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_CARD = "card_id";

    private static SharedPreferences p(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isRunning(Context ctx) {
        return p(ctx).getBoolean(KEY_RUNNING, false);
    }

    public static void setRunning(Context ctx, boolean running) {
        p(ctx).edit().putBoolean(KEY_RUNNING, running).apply();
    }

    public static String cardId(Context ctx) {
        return p(ctx).getString(KEY_CARD, null);
    }

    public static void setCardId(Context ctx, String id) {
        p(ctx).edit().putString(KEY_CARD, id).apply();
    }

    public static boolean hasCard(Context ctx) {
        return cardId(ctx) != null && !cardId(ctx).isEmpty();
    }
}
