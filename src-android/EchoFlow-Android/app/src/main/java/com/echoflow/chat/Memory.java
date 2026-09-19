package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 一条长期记忆。按重要度分级，可锁定/禁用。
 */
public class Memory {
    public String id;
    public String cardId;
    public String content;
    public int importance;   // 1-5
    public String category;  // user_info / preference / relationship / event / fact
    public String source;    // 来源消息摘要
    public long time;
    public boolean locked;
    public boolean enabled;

    public Memory() {
        enabled = true;
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("content", content == null ? "" : content);
        o.put("importance", importance);
        o.put("category", category == null ? "fact" : category);
        o.put("source", source == null ? "" : source);
        o.put("time", time);
        o.put("locked", locked);
        o.put("enabled", enabled);
        return o;
    }

    public static Memory fromJson(JSONObject o) {
        Memory m = new Memory();
        m.id = o.optString("id");
        m.cardId = o.optString("cardId", "");
        m.content = o.optString("content", "");
        m.importance = o.optInt("importance", 3);
        m.category = o.optString("category", "fact");
        m.source = o.optString("source", "");
        m.time = o.optLong("time", 0);
        m.locked = o.optBoolean("locked", false);
        m.enabled = o.optBoolean("enabled", true);
        return m;
    }
}
