package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话历史持久化：SharedPreferences 存 JSON。
 */
public class ChatStore {

    private static final String PREFS = "echoflow_chat";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_CFG = "echoflow_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_MODEL = "model";
    private static final String KEY_PERSONA = "persona";

    public static final String DEFAULT_BASE_URL = "https://aibridgea.com";
    public static final String DEFAULT_MODEL = "grok-2";

    public static List<Message> loadMessages(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SESSIONS, "[]");
        List<Message> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Message m = new Message(o.optString("role", "assistant"), o.optString("content", ""));
                list.add(m);
            }
        } catch (Exception ignore) {
        }
        return list;
    }

    public static void saveMessages(Context ctx, List<Message> messages) {
        JSONArray arr = new JSONArray();
        for (Message m : messages) {
            JSONObject o = new JSONObject();
            try {
                o.put("role", m.role);
                o.put("content", m.content);
            } catch (Exception ignore) {
            }
            arr.put(o);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_SESSIONS, arr.toString()).apply();
    }

    public static void clearMessages(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_SESSIONS).apply();
    }

    public static String getBaseUrl(Context ctx) {
        return ctx.getSharedPreferences(KEY_CFG, Context.MODE_PRIVATE)
                .getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    public static String getModel(Context ctx) {
        return ctx.getSharedPreferences(KEY_CFG, Context.MODE_PRIVATE)
                .getString(KEY_MODEL, DEFAULT_MODEL);
    }

    public static void saveConfig(Context ctx, String baseUrl, String model) {
        ctx.getSharedPreferences(KEY_CFG, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, baseUrl)
                .putString(KEY_MODEL, model)
                .apply();
    }

    public static String getPersona(Context ctx) {
        return ctx.getSharedPreferences(KEY_CFG, Context.MODE_PRIVATE)
                .getString(KEY_PERSONA, "");
    }

    public static void savePersona(Context ctx, String persona) {
        ctx.getSharedPreferences(KEY_CFG, Context.MODE_PRIVATE)
                .edit().putString(KEY_PERSONA, persona).apply();
    }
}
