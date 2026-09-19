package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 角色日记（一篇）。
 *
 * 为什么需要它：聊天是「你找她"，日记是「她在你没看的时候也在生活"。
 * 这是陪伴类产品里性价比最高的一件「生命感"装置 —— 成本低，情感密度极高。
 *
 * 四段固定结构，其中 UNSAID 是题眼：
 *   line      今天的开场（具体、有画面）
 *   body      发生了什么 + 她当时的反应（要有细节，不要总结）
 *   unsaid    ★ 她没说出口的那句话（只有日记里才写）
 *   tomorrow  明天的打算（可被次日的主动消息引用）
 */
public class DiaryEntry {

    public String id;
    public String cardId;
    /** yyyy-MM-dd */
    public String date;
    /** 生成时的情境快照，用于「按心情筛选" */
    public String emotion = "平静";
    public String weather = "";
    public int bond;

    public String line = "";
    public String body = "";
    public String unsaid = "";
    public String tomorrow = "";

    /** auto = 系统按当天记录生成；user = 用户代写 */
    public String author = "auto";
    /** 当天是否有里程碑事件 */
    public boolean milestone;
    public long createdAt;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("date", date == null ? "" : date);
        o.put("emotion", emotion == null ? "平静" : emotion);
        o.put("weather", weather == null ? "" : weather);
        o.put("bond", bond);
        o.put("line", line == null ? "" : line);
        o.put("body", body == null ? "" : body);
        o.put("unsaid", unsaid == null ? "" : unsaid);
        o.put("tomorrow", tomorrow == null ? "" : tomorrow);
        o.put("author", author == null ? "auto" : author);
        o.put("milestone", milestone);
        o.put("createdAt", createdAt);
        return o;
    }

    public static DiaryEntry fromJson(JSONObject o) {
        DiaryEntry d = new DiaryEntry();
        d.id = o.optString("id", "");
        d.cardId = o.optString("cardId", "");
        d.date = o.optString("date", "");
        d.emotion = o.optString("emotion", "平静");
        d.weather = o.optString("weather", "");
        d.bond = o.optInt("bond", 0);
        d.line = o.optString("line", "");
        d.body = o.optString("body", "");
        d.unsaid = o.optString("unsaid", "");
        d.tomorrow = o.optString("tomorrow", "");
        d.author = o.optString("author", "auto");
        d.milestone = o.optBoolean("milestone", false);
        d.createdAt = o.optLong("createdAt", 0);
        return d;
    }

    /** 「2026.09.17" */
    public String dateLabel() {
        return date == null ? "" : date.replace("-", ".");
    }
}