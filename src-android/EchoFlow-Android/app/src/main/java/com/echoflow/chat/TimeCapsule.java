package com.echoflow.chat;

import org.json.JSONObject;

/**
 * 时间胶囊 —— 写给未来的话。
 *
 * ★ 硬约束：**封存期间 content 不参与任何检索**。
 *   如果她能在到期前引用胶囊内容，那它就不叫胶囊了，用户也不会真的写真正想说的话。
 *   因此任何把胶囊喂给模型的地方，都必须先过 {@link TimeCapsule#isSealed}。
 */
public class TimeCapsule {

    /** user_to_her（你写给她）| her_to_user（她写给你）| user_to_self（写给自己） */
    public String author = "user_to_her";
    public String id;
    public String cardId;
    /** 开启日 yyyy-MM-dd */
    public String openAt;
    public String title = "";
    /** ★ 封存内容：到期前不得进入记忆检索、Prompt 组装、朋友圈生成 */
    public String content = "";
    public long sealedAt;
    public boolean opened;
    public long openedAt;
    /** 到期时的送达方式 */
    public String deliver = "morning_first";
    /** 她是否可以提前说「我也有一个胶囊要给你" */
    public boolean teaser;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId == null ? "" : cardId);
        o.put("author", author == null ? "user_to_her" : author);
        o.put("openAt", openAt == null ? "" : openAt);
        o.put("title", title == null ? "" : title);
        o.put("content", content == null ? "" : content);
        o.put("sealedAt", sealedAt);
        o.put("opened", opened);
        o.put("openedAt", openedAt);
        o.put("deliver", deliver == null ? "morning_first" : deliver);
        o.put("teaser", teaser);
        return o;
    }

    public static TimeCapsule fromJson(JSONObject o) {
        TimeCapsule c = new TimeCapsule();
        c.id = o.optString("id", "");
        c.cardId = o.optString("cardId", "");
        c.author = o.optString("author", "user_to_her");
        c.openAt = o.optString("openAt", "");
        c.title = o.optString("title", "");
        c.content = o.optString("content", "");
        c.sealedAt = o.optLong("sealedAt", 0);
        c.opened = o.optBoolean("opened", false);
        c.openedAt = o.optLong("openedAt", 0);
        c.deliver = o.optString("deliver", "morning_first");
        c.teaser = o.optBoolean("teaser", false);
        return c;
    }

    /**
     * 是否仍在封存期。
     *
     * 这个方法的名字应该被每一个「把数据喂给模型"的地方调用一遍。
     * 到期日当天视为已到期（可以打开）。
     */
    public boolean isSealed() {
        if (opened) {
            return false;
        }
        String today = Fortune.today();
        return openAt != null && today.compareTo(openAt) < 0;
    }

    public boolean isDue() {
        if (opened) {
            return false;
        }
        String today = Fortune.today();
        return openAt != null && today.compareTo(openAt) >= 0;
    }

    public boolean fromHer() {
        return "her_to_user".equals(author);
    }

    /** 剩余天数；已到期返回 0 或负数 */
    public int daysLeft() {
        try {
            String today = Fortune.today();
            String[] a = today.split("-");
            String[] b = openAt.split("-");
            java.util.Calendar ca = java.util.Calendar.getInstance();
            ca.set(Integer.parseInt(a[0]), Integer.parseInt(a[1]) - 1, Integer.parseInt(a[2]));
            java.util.Calendar cb = java.util.Calendar.getInstance();
            cb.set(Integer.parseInt(b[0]), Integer.parseInt(b[1]) - 1, Integer.parseInt(b[2]));
            long diff = cb.getTimeInMillis() - ca.getTimeInMillis();
            return (int) Math.round(diff / 86400000.0);
        } catch (Exception e) {
            return 0;
        }
    }

    public String countdownLabel() {
        int d = daysLeft();
        if (d > 0) {
            return d + " 天";
        }
        if (d == 0) {
            return "今天";
        }
        return "已到期";
    }

    /**
     * 给用户看的摘要。★ 封存期只给标题，绝不给内容。
     */
    public String previewForUi() {
        return isSealed() ? "封存中 · 到期才能打开" : content;
    }
}