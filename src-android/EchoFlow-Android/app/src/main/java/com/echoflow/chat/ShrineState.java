package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 神社状态：连续参拜、御守、七日签运。
 * 存储：filesDir/shrine/{cardId}.json
 */
public class ShrineState {

    public String cardId;
    /** 连续参拜天数 */
    public int streak;
    /** 上次参拜日期 yyyy-MM-dd */
    public String lastVisit = "";
    /** 累计参拜 */
    public int totalVisits;
    /** 已佩戴的御守 id，空表示未佩戴 */
    public String equipped = "";
    /** 已解锁的御守 id */
    public java.util.List<String> unlocked = new java.util.ArrayList<>();
    /** 最近签运，用于七日柱状图。元素形如 "2026-09-17|吉" */
    public java.util.List<String> history = new java.util.ArrayList<>();

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("streak", streak);
        o.put("lastVisit", lastVisit == null ? "" : lastVisit);
        o.put("totalVisits", totalVisits);
        o.put("equipped", equipped == null ? "" : equipped);
        org.json.JSONArray u = new org.json.JSONArray();
        for (String s : unlocked) {
            u.put(s);
        }
        o.put("unlocked", u);
        org.json.JSONArray h = new org.json.JSONArray();
        for (String s : history) {
            h.put(s);
        }
        o.put("history", h);
        return o;
    }

    public static ShrineState fromJson(JSONObject o) {
        ShrineState s = new ShrineState();
        s.cardId = o.optString("cardId", "");
        s.streak = o.optInt("streak", 0);
        s.lastVisit = o.optString("lastVisit", "");
        s.totalVisits = o.optInt("totalVisits", 0);
        s.equipped = o.optString("equipped", "");
        org.json.JSONArray u = o.optJSONArray("unlocked");
        if (u != null) {
            for (int i = 0; i < u.length(); i++) {
                s.unlocked.add(u.optString(i, ""));
            }
        }
        org.json.JSONArray h = o.optJSONArray("history");
        if (h != null) {
            for (int i = 0; i < h.length(); i++) {
                s.history.add(h.optString(i, ""));
            }
        }
        return s;
    }
}
