package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 角色关系/羁绊状态。
 */
public class RelationshipState {
    public String cardId;
    public int level;          // 0陌生 1初识 2熟悉 3信赖 4亲密 5特殊羁绊
    public int points;         // 0-100
    public int chatCount;
    public long lastInteraction;
    public String customLevel; // 作者自定义等级名，可空

    public static final String[] LEVEL_NAMES = {"陌生", "初识", "熟悉", "信赖", "亲密", "特殊羁绊"};

    public String levelName() {
        if (customLevel != null && !customLevel.isEmpty()) {
            return customLevel;
        }
        int idx = Math.max(0, Math.min(5, level));
        return LEVEL_NAMES[idx];
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("level", level);
        o.put("points", points);
        o.put("chatCount", chatCount);
        o.put("lastInteraction", lastInteraction);
        o.put("customLevel", customLevel == null ? "" : customLevel);
        return o;
    }

    public static RelationshipState fromJson(JSONObject o) {
        RelationshipState r = new RelationshipState();
        r.cardId = o.optString("cardId", "");
        r.level = o.optInt("level", 0);
        r.points = o.optInt("points", 0);
        r.chatCount = o.optInt("chatCount", 0);
        r.lastInteraction = o.optLong("lastInteraction", 0);
        r.customLevel = o.optString("customLevel", "");
        return r;
    }
}
