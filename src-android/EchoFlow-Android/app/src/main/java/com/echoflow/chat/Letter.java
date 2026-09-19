package com.echoflow.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 一封信。
 *
 * 与聊天消息的关键差异：
 *   greet     开头一句短问候（禁止「亲爱的"这类套话）
 *   body      正文，3~5 段，段落要短，必须具体引用一件记忆或事件
 *   ps        ★ 附言，常常是最动人的一句
 */
public class Letter {

    public String id;
    public String cardId;
    /** to_her（你写的）| from_her（她写的） */
    public String direction = "from_her";
    public String to = "";
    public String greet = "";
    public String body = "";
    public String sign = "";
    public String dateLabel = "";
    public String ps = "";
    public long createdAt;
    public boolean read;
    public boolean savedAsCard;

    /** 这封信是怎么写出来的 —— 用于「生成依据"卡面的可解释性 */
    public List<String> basisMemoryIds = new ArrayList<>();
    public List<String> basisEventIds = new ArrayList<>();
    public String basisWeather = "";
    public String basisEmotion = "";
    public int basisBond;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("direction", direction == null ? "from_her" : direction);
        o.put("to", to == null ? "" : to);
        o.put("greet", greet == null ? "" : greet);
        o.put("body", body == null ? "" : body);
        o.put("sign", sign == null ? "" : sign);
        o.put("dateLabel", dateLabel == null ? "" : dateLabel);
        o.put("ps", ps == null ? "" : ps);
        o.put("createdAt", createdAt);
        o.put("read", read);
        o.put("savedAsCard", savedAsCard);
        JSONArray m = new JSONArray();
        for (String s : basisMemoryIds) {
            m.put(s);
        }
        o.put("basisMemoryIds", m);
        JSONArray e = new JSONArray();
        for (String s : basisEventIds) {
            e.put(s);
        }
        o.put("basisEventIds", e);
        o.put("basisWeather", basisWeather == null ? "" : basisWeather);
        o.put("basisEmotion", basisEmotion == null ? "" : basisEmotion);
        o.put("basisBond", basisBond);
        return o;
    }

    public static Letter fromJson(JSONObject o) {
        Letter l = new Letter();
        l.id = o.optString("id", "");
        l.cardId = o.optString("cardId", "");
        l.direction = o.optString("direction", "from_her");
        l.to = o.optString("to", "");
        l.greet = o.optString("greet", "");
        l.body = o.optString("body", "");
        l.sign = o.optString("sign", "");
        l.dateLabel = o.optString("dateLabel", "");
        l.ps = o.optString("ps", "");
        l.createdAt = o.optLong("createdAt", 0);
        l.read = o.optBoolean("read", false);
        l.savedAsCard = o.optBoolean("savedAsCard", false);
        JSONArray m = o.optJSONArray("basisMemoryIds");
        if (m != null) {
            for (int i = 0; i < m.length(); i++) {
                l.basisMemoryIds.add(m.optString(i, ""));
            }
        }
        JSONArray e = o.optJSONArray("basisEventIds");
        if (e != null) {
            for (int i = 0; i < e.length(); i++) {
                l.basisEventIds.add(e.optString(i, ""));
            }
        }
        l.basisWeather = o.optString("basisWeather", "");
        l.basisEmotion = o.optString("basisEmotion", "");
        l.basisBond = o.optInt("basisBond", 0);
        return l;
    }

    public boolean fromHer() {
        return "from_her".equals(direction);
    }

    /** 列表里的一行摘要 */
    public String preview() {
        String s = body == null ? "" : body.replace("\n", " ").trim();
        if (s.isEmpty()) {
            s = greet == null ? "" : greet;
        }
        return s.length() > 42 ? s.substring(0, 42) + "…" : s;
    }
}