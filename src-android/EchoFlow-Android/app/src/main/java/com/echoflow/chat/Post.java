package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 一条角色朋友圈动态。关联 cardId，内容由 AI 生成。
 */
public class Post {
    public String id;
    public String cardId;   // 哪个角色发的
    public String author;    // 角色名（冗余存一份，角色改名后历史不变）
    public String content;
    public long time;
    public int likes;

    public Post() {
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("author", author == null ? "" : author);
        o.put("content", content == null ? "" : content);
        o.put("time", time);
        o.put("likes", likes);
        return o;
    }

    public static Post fromJson(JSONObject o) {
        Post p = new Post();
        p.id = o.optString("id");
        p.cardId = o.optString("cardId", "");
        p.author = o.optString("author", "");
        p.content = o.optString("content", "");
        p.time = o.optLong("time", 0);
        p.likes = o.optInt("likes", 0);
        return p;
    }
}
