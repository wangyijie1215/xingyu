package com.echoflow.chat;

/* loaded from: classes.dex */
public class PromptBuilder {
    public static java.lang.String applyVars(java.lang.String str, com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona) {
        if (str == null) {
            return "";
        }
        java.lang.String str2 = (characterCard == null || characterCard.name.isEmpty()) ? "角色" : characterCard.name;
        java.lang.String str3 = (persona == null || persona.name.isEmpty()) ? "用户" : persona.name;
        return str.replace("{{char}}", str2).replace("{{user}}", str3).replace("<CHAR>", str2).replace("<USER>", str3);
    }

    public static java.util.List<com.echoflow.chat.Message> buildMessages(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list) {
        return buildMessages(characterCard, persona, list, null, null, null);
    }

    public static java.util.List<com.echoflow.chat.Message> buildMessages(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState) {
        return buildMessages(characterCard, persona, list, list2, relationshipState, emotionState, null);
    }

    public static java.util.List<com.echoflow.chat.Message> buildMessages(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, com.echoflow.chat.Fortune fortune) {
        return buildMessages(characterCard, persona, list, list2, relationshipState, emotionState, fortune, null);
    }

    public static java.util.List<com.echoflow.chat.Message> buildMessages(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, com.echoflow.chat.Fortune fortune, com.echoflow.chat.PromptBuilder.LifeContext lifeContext) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append(applyVars("你现在正在扮演 {{char}}。请完全代入角色，用第一人称回复，不要跳出角色，不要以旁白或作者身份说话。", characterCard, persona));
        sb.append("\n\n【当前时间】\n").append(currentTimeString());
        if (characterCard != null) {
            appendSection(sb, "角色名称", characterCard.name);
            appendSection(sb, "角色描述", characterCard.description);
            appendSection(sb, "性格", characterCard.personality);
            appendSection(sb, "场景", characterCard.scenario);
            if (persona != null && !persona.description.isEmpty()) {
                appendSection(sb, "用户设定", persona.description);
            }
            appendSection(sb, "示例对话", characterCard.mesExample);
            appendSection(sb, "系统提示", characterCard.systemPrompt);
        }
        if (list2 != null && !list2.isEmpty()) {
            java.lang.StringBuilder sb2 = new java.lang.StringBuilder();
            for (com.echoflow.chat.Memory memory : list2) {
                if (memory.enabled) {
                    sb2.append("- ").append(memory.content).append("\n");
                }
            }
            if (sb2.length() > 0) {
                sb.append("\n\n【关于用户和你们之间的长期记忆】\n").append(sb2.toString().trim());
                sb.append("\n（这些是你应该记住的事实，自然地融入对话，不要逐条复述。）");
            }
        }
        if (relationshipState != null) {
            sb.append("\n\n【你们的关系】\n").append("关系等级：").append(relationshipState.levelName()).append("（亲密度 ").append(relationshipState.points).append("/100）").append("\n已聊天 ").append(relationshipState.chatCount).append(" 次").append("\n（根据关系亲密度调整你的语气和亲近程度，不要机械提及数字。）");
        }
        if (emotionState != null && emotionState.mood != null && !emotionState.mood.isEmpty()) {
            sb.append("\n\n【你当前的情绪】\n").append(emotionState.mood);
            if (emotionState.intensity > 1) {
                sb.append("（强度 ").append(emotionState.intensity).append("/5）");
            }
            if (emotionState.reason != null && !emotionState.reason.isEmpty()) {
                sb.append("，原因：").append(emotionState.reason);
            }
            sb.append("\n（让你的语气、回复长度、标点符合当前情绪，但不要每句都描述自己的情绪。）");
        }
        if (fortune != null) {
            sb.append("\n\n【今天在银月神社求的签】\n").append("第 ").append(fortune.slipNo).append(" 番 · ").append(fortune.rank).append(" · 综合 ").append(fortune.score).append("\n签文：").append(fortune.summary);
            java.lang.StringBuilder sb3 = new java.lang.StringBuilder();
            for (com.echoflow.chat.Fortune.Dim dim : fortune.dimensions) {
                if (sb3.length() > 0) {
                    sb3.append("\u3000");
                }
                sb3.append(dim.key).append(" ").append(dim.value);
            }
            if (sb3.length() > 0) {
                sb.append("\n分项：").append((java.lang.CharSequence) sb3);
            }
            if (!fortune.good.isEmpty()) {
                sb.append("\n宜：").append(java.lang.String.join("、", fortune.good));
            }
            if (!fortune.bad.isEmpty()) {
                sb.append("\n忌：").append(java.lang.String.join("、", fortune.bad));
            }
            sb.append("\n（这是你们今天一起求的签。自然地融入对话，只在相关时提起，不要每句都念叨签文。）");
        }
        if (lifeContext != null) {
            if (lifeContext.lastDiaryUnsaid != null && !lifeContext.lastDiaryUnsaid.isEmpty()) {
                sb.append("\n\n【你昨天的日记】\n").append("你昨天在日记里写：「").append(lifeContext.lastDiaryUnsaid).append("」");
                sb.append("\n（这是你私下写的、对方并没有看过。今天可以顺着这句话往下说，但不要说「我日记里写了」——日记是只给未来的自己看的。）");
            }
            if (lifeContext.lastLetterHint != null && !lifeContext.lastLetterHint.isEmpty()) {
                sb.append("\n\n【你们最近的一封信】\n").append(lifeContext.lastLetterHint);
                sb.append("\n（可以在合适的时候提起「我上次信里写的」，但不要复述全文。）");
            }
            if (lifeContext.sealedCapsuleTeasers != null && !lifeContext.sealedCapsuleTeasers.isEmpty()) {
                sb.append("\n\n【时间胶囊】\n");
                java.util.Iterator<java.lang.String> it = lifeContext.sealedCapsuleTeasers.iterator();
                while (it.hasNext()) {
                    sb.append("- ").append(it.next()).append("\n");
                }
                sb.append("（你知道有一个胶囊存在，但不知道里面写了什么，对方也不知道你的。可以提到「到时候再给你看」，但不要试图猜内容。）");
            }
        }
        arrayList.add(new com.echoflow.chat.Message("system", sb.toString()));
        if (characterCard != null && !characterCard.postHistoryInstructions.isEmpty()) {
            arrayList.add(new com.echoflow.chat.Message("system", applyVars(characterCard.postHistoryInstructions, characterCard, persona)));
        }
        if (list != null) {
            for (com.echoflow.chat.Message message : list) {
                if (!"system".equals(message.role)) {
                    arrayList.add(new com.echoflow.chat.Message(message.role, message.content));
                }
            }
        }
        return arrayList;
    }

    /* loaded from: classes.dex */
    public static class LifeContext {
        public java.lang.String lastDiaryUnsaid;
        public java.lang.String lastLetterHint;
        public java.util.List<java.lang.String> sealedCapsuleTeasers;

        public static com.echoflow.chat.PromptBuilder.LifeContext empty() {
            return new com.echoflow.chat.PromptBuilder.LifeContext();
        }
    }

    private static java.lang.String currentTimeString() {
        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm", java.util.Locale.getDefault());
        java.text.SimpleDateFormat simpleDateFormat2 = new java.text.SimpleDateFormat("EEEE", java.util.Locale.CHINA);
        java.util.Date date = new java.util.Date();
        return simpleDateFormat.format(date) + " " + simpleDateFormat2.format(date);
    }

    private static void appendSection(java.lang.StringBuilder sb, java.lang.String str, java.lang.String str2) {
        if (str2 == null || str2.trim().isEmpty()) {
            return;
        }
        sb.append("\n\n【").append(str).append("】\n").append(str2.trim());
    }
}
