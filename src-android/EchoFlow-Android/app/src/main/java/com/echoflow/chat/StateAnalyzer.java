package com.echoflow.chat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话后状态分析器：调用 AI 提取记忆、判断情绪、更新关系、生成事件。
 * 异步执行，不阻塞聊天 UI。
 */
public class StateAnalyzer {

    /**
     * 分析一轮对话。messages 包含最近的 user+assistant 消息（可含更多历史）。
     */
    public static void analyze(Context ctx, CharacterCard card, Persona user,
                               String baseUrl, String apiKey, String model,
                               List<Message> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) return;

        StringBuilder sys = new StringBuilder();
        sys.append("你是角色记忆与状态分析器。基于以下对话，输出严格的 JSON，不要其他文字。\n\n");
        sys.append("角色：").append(card.name).append("\n");
        if (card.personality != null && !card.personality.isEmpty()) {
            sys.append("角色性格：").append(card.personality).append("\n");
        }
        sys.append("\n输出格式：\n");
        sys.append("{\n");
        sys.append("  \"memories\": [{\"content\":\"...\",\"importance\":1-5,\"category\":\"user_info|preference|relationship|event|fact\"}],\n");
        sys.append("  \"emotion\": {\"mood\":\"平静|开心|悲伤|期待|孤独|害羞|紧张|惊讶|生气|疲惫\",\"intensity\":1-5,\"reason\":\"...\"},\n");
        sys.append("  \"relationship_delta\": 0-5,\n");
        sys.append("  \"event\": {\"title\":\"...\",\"description\":\"...\"} 或 null\n");
        sys.append("}\n\n");
        sys.append("规则：\n");
        sys.append("1. 只提取真正重要、值得长期记住的信息。普通寒暄、日常琐事不要记忆。\n");
        sys.append("2. importance: 5=核心身份/强烈偏好, 4=重要事实, 3=有意义, 2=一般, 1=琐碎\n");
        sys.append("3. emotion 基于角色性格和对话内容，判断角色当前的情绪状态。\n");
        sys.append("4. relationship_delta: 这次对话让关系亲近了多少（0-5），普通对话1，深入对话2-3，特殊时刻4-5。\n");
        sys.append("5. event: 只有当这次对话发生了值得记录的重要事情时才填，否则填null。\n");
        sys.append("6. 只输出 JSON，绝对不要输出解释、markdown代码块或其他文字。");

        List<Message> msgs = new ArrayList<>();
        msgs.add(new Message("system", sys.toString()));
        // 把最近对话作为 user 消息发过去
        StringBuilder conv = new StringBuilder();
        for (Message m : recentMessages) {
            String role = "user".equals(m.role) ? (user != null && !user.name.isEmpty() ? user.name : "用户") : card.name;
            conv.append(role).append("：").append(m.content).append("\n");
        }
        msgs.add(new Message("user", conv.toString()));

        final String[] lastText = {""};
        ApiClient.streamChat(baseUrl, apiKey, model, msgs, new ApiClient.Callback() {
            @Override
            public void onChunk(String fullText) {
                lastText[0] = fullText;
            }

            @Override
            public void onDone(boolean error, String errMsg) {
                if (error || lastText[0].isEmpty()) return;
                try {
                    String raw = lastText[0].trim();
                    // 去掉可能的 markdown 代码块包裹
                    if (raw.startsWith("```")) {
                        raw = raw.replaceAll("^```[a-zA-Z]*\\n", "").replaceAll("```$", "").trim();
                    }
                    JSONObject result = new JSONObject(raw);
                    applyResult(ctx, card, result);
                } catch (Exception ignore) {
                    // 分析失败静默，不影响聊天
                }
            }
        });
    }

    private static void applyResult(Context ctx, CharacterCard card, JSONObject result) {
        // 1. 记忆
        try {
            JSONArray mems = result.optJSONArray("memories");
            if (mems != null) {
                for (int i = 0; i < mems.length(); i++) {
                    JSONObject mo = mems.getJSONObject(i);
                    String content = mo.optString("content", "").trim();
                    if (content.isEmpty()) continue;
                    int imp = mo.optInt("importance", 3);
                    if (imp < 2) continue; // 不重要的不存
                    Memory m = new Memory();
                    m.id = "m" + System.currentTimeMillis() + "_" + i;
                    m.cardId = card.id;
                    m.content = content;
                    m.importance = Math.max(1, Math.min(5, imp));
                    m.category = mo.optString("category", "fact");
                    m.time = System.currentTimeMillis();
                    MemoryStore.add(ctx, card.id, m);
                }
            }
        } catch (Exception ignore) {
        }

        // 2. 情绪
        try {
            JSONObject emo = result.optJSONObject("emotion");
            if (emo != null) {
                EmotionState e = new EmotionState();
                e.cardId = card.id;
                e.mood = emo.optString("mood", "平静");
                e.intensity = Math.max(1, Math.min(5, emo.optInt("intensity", 1)));
                e.reason = emo.optString("reason", "");
                e.time = System.currentTimeMillis();
                CharacterStateStore.saveEmotion(ctx, e);
            }
        } catch (Exception ignore) {
        }

        // 3. 关系
        try {
            int delta = result.optInt("relationship_delta", 1);
            RelationshipState r = CharacterStateStore.loadRelationship(ctx, card.id);
            r.chatCount++;
            r.lastInteraction = System.currentTimeMillis();
            r.points = Math.min(100, r.points + delta);
            // 每 20 点升一级
            int newLevel = Math.min(5, r.points / 20);
            if (newLevel > r.level) {
                r.level = newLevel;
            }
            CharacterStateStore.saveRelationship(ctx, r);
        } catch (Exception ignore) {
        }

        // 4. 事件
        try {
            JSONObject ev = result.optJSONObject("event");
            if (ev != null) {
                String title = ev.optString("title", "").trim();
                if (!title.isEmpty()) {
                    StoryEvent e = new StoryEvent();
                    e.id = "e" + System.currentTimeMillis();
                    e.cardId = card.id;
                    e.title = title;
                    e.description = ev.optString("description", "");
                    e.date = System.currentTimeMillis();
                    e.autoGenerated = true;
                    EventStore.add(ctx, card.id, e);
                }
            }
        } catch (Exception ignore) {
        }
    }
}
