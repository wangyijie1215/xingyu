package com.echoflow.chat;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 「已读游标"——修未读角标的根本方案。
 *
 * 之前的实现是错的：角标数量 = 「有聊天记录的角色数"，
 * 只要你和她聊过话，这个数字就永远 ≥1，点进去也不会清零。
 *
 * 正确的做法是记录**每个角色你读到第几条**：
 *   未读数 = 她发的消息总数 - 已读数
 *
 * 进入聊天页时把游标推到最新，回到列表角标就没了。
 */
public class ReadCursor {

    private static final String FILE = "_read_cursor.json";

    private static File file(Context ctx) {
        return new File(ctx.getFilesDir(), FILE);
    }

    private static JSONObject loadAll(Context ctx) {
        File f = file(ctx);
        if (!f.exists()) {
            return new JSONObject();
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (FileReader fr = new FileReader(f)) {
                char[] buf = new char[4096];
                int n;
                while ((n = fr.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
            }
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /** 某角色已读到哪里（消息条数） */
    public static int readCount(Context ctx, String cardId) {
        return loadAll(ctx).optInt(cardId, 0);
    }

    /** 标记为已读到最新 */
    public static void markRead(Context ctx, String cardId) {
        try {
            JSONObject all = loadAll(ctx);
            all.put(cardId, PhoneStore.count(ctx, cardId));
            try (FileWriter fw = new FileWriter(file(ctx))) {
                fw.write(all.toString());
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 未读数 = 从末尾往前数，连续的、她发的、且超出已读游标的消息条数。
     *
     * 只数**结尾处连续的**她发的消息：
     * 如果最后一条是用户发的，说明你已经回过了，不该有未读。
     */
    public static int unread(Context ctx, String cardId) {
        java.util.List<Message> msgs = PhoneStore.list(ctx, cardId);
        int total = msgs.size();
        int read = readCount(ctx, cardId);
        if (total <= read) {
            return 0;
        }
        // 从末尾往前数连续的她发的消息
        int n = 0;
        for (int i = total - 1; i >= read; i--) {
            if ("assistant".equals(msgs.get(i).role)) {
                n++;
            } else {
                break;   // 遇到用户发的就停 —— 后面的都算已读
            }
        }
        return n;
    }

    /** 全部角色的未读总和 */
    public static int totalUnread(Context ctx) {
        int sum = 0;
        for (CharacterCard c : CardStore.listCards(ctx)) {
            sum += unread(ctx, c.id);
        }
        return sum;
    }
}