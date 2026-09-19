package com.echoflow.chat;

/* loaded from: classes.dex */
public class PhonePrompt {
    private static final java.lang.String PHONE_RULES = "\n\n【当前场景：手机聊天】\n对方正在用手机和你聊天，就像发微信一样。\n重要规则（必须严格遵守）：\n1. 只输出你会打出来的文字。不要写任何动作、神态、心理描写。\n2. 不要用（）或*号写旁白，例如「（她顿了顿）」是绝对禁止的。\n3. 不要用第三人称描述自己。\n4. 回复要短。通常一到两句，最多不超过三句。\n5. 可以只回一个字或一个词，真人发消息就是这样。\n6. 可以省略标点，可以用省略号，可以用语气词，但不许客服腔。\n7. 不要每句都加表情符号。偶尔用一次即可。\n8. 如果有人问你在做什么，就直说在做什么，不要描述动作。";

    public static java.util.List<com.echoflow.chat.Message> build(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, com.echoflow.chat.Fortune fortune, com.echoflow.chat.PromptBuilder.LifeContext lifeContext) {
        return build(characterCard, persona, list, list2, relationshipState, emotionState, fortune, lifeContext, null);
    }

    public static java.util.List<com.echoflow.chat.Message> build(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, com.echoflow.chat.Fortune fortune, com.echoflow.chat.PromptBuilder.LifeContext lifeContext, java.lang.String str) {
        return build(characterCard, persona, list, list2, relationshipState, emotionState, fortune, lifeContext, str, null);
    }

    public static java.util.List<com.echoflow.chat.Message> build(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.util.List<com.echoflow.chat.Message> list, java.util.List<com.echoflow.chat.Memory> list2, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, com.echoflow.chat.Fortune fortune, com.echoflow.chat.PromptBuilder.LifeContext lifeContext, java.lang.String str, java.lang.String str2) {
        java.util.List<com.echoflow.chat.Message> buildMessages = com.echoflow.chat.PromptBuilder.buildMessages(characterCard, persona, list, list2, relationshipState, emotionState, fortune, lifeContext);
        java.lang.StringBuilder sb = new java.lang.StringBuilder(PHONE_RULES);
        if (str != null && !str.isEmpty()) {
            sb.append("\n\n【你此刻的日程】\n").append(str);
        }
        if (str2 != null && !str2.isEmpty()) {
            sb.append(str2);
        }
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (com.echoflow.chat.Message message : buildMessages) {
            if (message.role.equals("system")) {
                arrayList.add(new com.echoflow.chat.Message("system", message.content + ((java.lang.Object) sb)));
            } else {
                arrayList.add(message);
            }
        }
        return arrayList;
    }
}
