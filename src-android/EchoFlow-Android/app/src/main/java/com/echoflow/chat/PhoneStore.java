package com.echoflow.chat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 手机模式的会话存储。
 *
 * 与主聊天（CardChatActivity）的关系：
 *  · **完全独立的历史**。手机模式是一条独立时间线，互不干扰
 *  · 共用角色卡、关系、情绪、记忆 —— 也就是说，手机上聊的内容
 *    同样会被 StateAnalyzer 提取成长期记忆，主聊天里她也记得
 *
 * ── 存储格式 ─────────────────────────────────────
 *
 * 从「一个 JSON 数组」改成 **JSONL（一行一条消息）**。
 *
 * 原因：原来的 save() 每次都把整个对话重新序列化再整文件覆盖 ——
 * 聊到 2000 条时，每发一条新消息都要重写 2000 条，真机上会开始卡。
 * JSONL 只要 append 一行就够，是 O(1) 而不是 O(n)。
 *
 * 兼容：老格式（整个文件是一个 JSON 数组）仍然能读，
 * 读到之后下次写入会自动迁移成 JSONL。
 *
 * filesDir/phone/{cardId}.jsonl   ← 新格式
 * filesDir/phone/{cardId}.json    ← 老格式，只读不改
 */
public class PhoneStore {

    private static File phoneFile(Context ctx, String cardId) {
        return newFile(ctx, cardId);
    }

    /** 新格式：每行一条 */
    private static File newFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "phone");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + ".jsonl");
    }

    /** 老格式：整个文件一个 JSON 数组 */
    private static File oldFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "phone");
        return new File(dir, cardId + ".json");
    }

    public static List<Message> list(Context ctx, String cardId) {
        List<Message> out = new ArrayList<>();
        File nf = newFile(ctx, cardId);
        if (nf.exists()) {
            readJsonl(nf, out);
            return out;
        }
        // 回退到老格式
        File of = oldFile(ctx, cardId);
        if (of.exists()) {
            readLegacyJson(of, out);
        }
        return out;
    }

    /** 逐行读。某一行坏了就跳过那一行，不影响整个文件 */
    private static void readJsonl(File f, List<Message> out) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(
                        new java.io.FileInputStream(f), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    JSONObject o = new JSONObject(line);
                    out.add(new Message(o.optString("role", "user"),
                            o.optString("content", "")));
                } catch (Exception skip) {
                    // 单行损坏不影响其余
                }
            }
        } catch (Exception ignore) {
        }
    }

    /** 老格式：整个文件是一个 JSON 数组 */
    private static void readLegacyJson(File f, List<Message> out) {
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    out.add(new Message(o.optString("role", "user"),
                            o.optString("content", "")));
                }
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 全量写入。
     *
     * 保留这个方法是因为调用方（VoiceCallActivity 等）习惯用它。
     * 但它现在是**整文件重写**，消息多的时候会慢 ——
     * 新代码建议用 {@link #append}。
     *
     * 会顺手把老格式迁移掉：写完 jsonl 之后删掉老的 .json。
     */
    public static void save(Context ctx, String cardId, List<Message> messages) {
        File nf = newFile(ctx, cardId);
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(nf),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            for (Message m : messages) {
                String line = lineOf(m);
                if (line != null) {
                    bw.write(line);
                    bw.write('\n');
                }
            }
        } catch (Exception ignore) {
        }
        // 迁移：老文件已经读过了，删掉避免下次又走兼容分支
        File of = oldFile(ctx, cardId);
        if (of.exists()) {
            of.delete();
        }
    }

    /**
     * 追加一条消息 —— 这是推荐的写入方式。
     *
     * O(1)：只往文件末尾写一行，不碰已有内容。
     * 聊天这种"只往后长"的场景非常适合。
     */
    public static void append(Context ctx, String cardId, Message m) {
        String line = lineOf(m);
        if (line == null) {
            return;
        }
        File nf = newFile(ctx, cardId);
        // 首次写入前，如果存在老格式文件，先整体迁移过来
        if (!nf.exists()) {
            File of = oldFile(ctx, cardId);
            if (of.exists()) {
                List<Message> old = new ArrayList<>();
                readLegacyJson(of, old);
                if (!old.isEmpty()) {
                    save(ctx, cardId, old);
                }
                if (of.exists()) {
                    of.delete();
                }
            }
        }
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(nf, true),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write(line);
            bw.write('\n');
        } catch (Exception ignore) {
        }
    }

    /** 批量追加（比如一次保存一问一答，少开一次文件） */
    public static void appendAll(Context ctx, String cardId, List<Message> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            return;
        }
        File nf = newFile(ctx, cardId);
        if (!nf.exists()) {
            File of = oldFile(ctx, cardId);
            if (of.exists()) {
                List<Message> old = new ArrayList<>();
                readLegacyJson(of, old);
                if (!old.isEmpty()) {
                    save(ctx, cardId, old);
                }
                if (of.exists()) {
                    of.delete();
                }
            }
        }
        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(nf, true),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            for (Message m : msgs) {
                String line = lineOf(m);
                if (line != null) {
                    bw.write(line);
                    bw.write('\n');
                }
            }
        } catch (Exception ignore) {
        }
    }

    /** 消息序列化成一行 JSON；失败返回 null */
    private static String lineOf(Message m) {
        if (m == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("role", m.role == null ? "user" : m.role);
            o.put("content", m.content == null ? "" : m.content);
            return o.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear(Context ctx, String cardId) {
        File f = newFile(ctx, cardId);
        if (f.exists()) {
            f.delete();
        }
        File of = oldFile(ctx, cardId);
        if (of.exists()) {
            of.delete();
        }
    }

    /** 最后一条消息，用于会话列表的预览 */
    public static String lastPreview(Context ctx, String cardId, int maxLen) {
        List<Message> list = list(ctx, cardId);
        if (list.isEmpty()) {
            return "";
        }
        String s = list.get(list.size() - 1).content;
        if (s == null) {
            return "";
        }
        s = s.replace("\n", " ").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) + "…" : s;
    }

    public static int count(Context ctx, String cardId) {
        return list(ctx, cardId).size();
    }

    /**
     * 会话列表排序用的时间戳。
     * 用文件最后修改时间 —— 简单且够用，避免再维护一份元数据。
     */
    public static long lastActive(Context ctx, String cardId) {
        File f = phoneFile(ctx, cardId);
        return f.exists() ? f.lastModified() : 0L;
    }

    /** 按最近活跃排序的角色列表 */
    public static List<CharacterCard> recentCards(Context ctx) {
        List<CharacterCard> cards = new ArrayList<>(CardStore.listCards(ctx));
        Collections.sort(cards, (a, b) ->
                Long.compare(lastActive(ctx, b.id), lastActive(ctx, a.id)));
        return cards;
    }
}
