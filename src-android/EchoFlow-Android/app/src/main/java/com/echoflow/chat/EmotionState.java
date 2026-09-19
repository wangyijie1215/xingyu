package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 角色情绪状态。
 */
public class EmotionState {
    public String cardId;
    public String mood;      // 平静/开心/悲伤/期待/孤独/害羞/紧张/惊讶/生气/疲惫
    public int intensity;    // 1-5
    public String reason;
    public long time;

    public static final String[] MOODS = {"平静", "开心", "悲伤", "期待", "孤独", "害羞", "紧张", "惊讶", "生气", "疲惫"};

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("mood", mood == null ? "平静" : mood);
        o.put("intensity", intensity);
        o.put("reason", reason == null ? "" : reason);
        o.put("time", time);
        return o;
    }

    public static EmotionState fromJson(JSONObject o) {
        EmotionState e = new EmotionState();
        e.cardId = o.optString("cardId", "");
        e.mood = o.optString("mood", "平静");
        e.intensity = o.optInt("intensity", 1);
        e.reason = o.optString("reason", "");
        e.time = o.optLong("time", 0);
        return e;
    }
}
