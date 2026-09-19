package com.echoflow.chat;

/* loaded from: classes.dex */
public class CapsuleService {
    public static com.echoflow.chat.TimeCapsule create(java.lang.String str, java.lang.String str2, java.lang.String str3, java.lang.String str4, java.lang.String str5) {
        com.echoflow.chat.TimeCapsule timeCapsule = new com.echoflow.chat.TimeCapsule();
        timeCapsule.id = "cap_" + java.lang.System.currentTimeMillis();
        timeCapsule.cardId = str2;
        timeCapsule.author = str;
        timeCapsule.title = str3;
        timeCapsule.content = str4;
        timeCapsule.openAt = str5;
        timeCapsule.sealedAt = java.lang.System.currentTimeMillis();
        timeCapsule.opened = false;
        timeCapsule.deliver = "her_to_user".equals(str) ? "proactive" : "morning_first";
        timeCapsule.teaser = "her_to_user".equals(str);
        return timeCapsule;
    }

    public static com.echoflow.chat.TimeCapsule createForHer(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, java.lang.String str, java.lang.String str2, java.lang.String str3) {
        com.echoflow.chat.TimeCapsule create = create("user_to_her", characterCard.id, str, str2, str3);
        com.echoflow.chat.CapsuleStore.save(context, characterCard.id, create);
        return create;
    }

    public static void herCapsuleAsync(final android.content.Context context, final com.echoflow.chat.CharacterCard characterCard, java.lang.String str, java.lang.String str2, java.lang.String str3, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, java.util.List<com.echoflow.chat.StoryEvent> list, java.lang.String str4, final java.lang.Runnable runnable) {
        final com.echoflow.chat.TimeCapsule create = create("her_to_user", characterCard.id, characterCard.name + "写给你的胶囊", "到那天，你还在这条街上吗？\n如果不在也没关系，我只是想问问。", str4);
        com.echoflow.chat.CapsuleStore.save(context, characterCard.id, create);
        if (str2 == null || str2.isEmpty()) {
            if (runnable != null) {
                runnable.run();
                return;
            }
            return;
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder("你现在是 ");
        sb.append(characterCard.name).append("。");
        if (characterCard.personality != null && !characterCard.personality.isEmpty()) {
            sb.append("性格：").append(characterCard.personality);
        }
        sb.append("\n【你们的关系】").append(relationshipState.levelName()).append("，亲密度 ").append(relationshipState.points).append("/100\n【你当前的情绪】");
        sb.append(emotionState.mood == null ? "平静" : emotionState.mood);
        if (list != null && !list.isEmpty()) {
            sb.append("\n【最近发生的事】").append(list.get(0).title);
        }
        sb.append("\n\n【任务】你要写一个时间胶囊，给对方在未来（").append(str4).append("）打开。\n写 2~4 句。可以是那时候想问对方的话、那时候希望对方知道的事、或者你现在不敢直接说的一句。\n要符合你的性格：话少、不太会直说，但写在只会被未来打开的地方，可以稍微坦白一点。\n直接输出内容，不要引号、不要标题、不要解释。");
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add(new com.echoflow.chat.Message("system", sb.toString()));
        arrayList.add(new com.echoflow.chat.Message("user", "请输出胶囊内容。"));
        com.echoflow.chat.ApiClient.streamChat(str, str2, str3, arrayList, new com.echoflow.chat.ApiClient.Callback() { // from class: com.echoflow.chat.CapsuleService.1
            private java.lang.String buf = "";

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onChunk(java.lang.String str5) {
                this.buf = str5;
            }

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onDone(boolean z, java.lang.String str5) {
                java.lang.String str6;
                if (!z && (str6 = this.buf) != null && !str6.trim().isEmpty()) {
                    create.content = this.buf.trim().replaceAll("^[\"「『“]|[\"」』”]$", "");
                    com.echoflow.chat.CapsuleStore.save(context, characterCard.id, create);
                }
                java.lang.Runnable runnable2 = runnable;
                if (runnable2 != null) {
                    runnable2.run();
                }
            }
        });
    }

    public static int deliverDue(android.content.Context context, com.echoflow.chat.CharacterCard characterCard) {
        int i = 0;
        for (com.echoflow.chat.TimeCapsule timeCapsule : com.echoflow.chat.CapsuleStore.list(context, characterCard.id)) {
            if (timeCapsule.isDue()) {
                deliver(context, characterCard, timeCapsule);
                timeCapsule.opened = true;
                timeCapsule.openedAt = java.lang.System.currentTimeMillis();
                com.echoflow.chat.CapsuleStore.save(context, characterCard.id, timeCapsule);
                i++;
            }
        }
        return i;
    }

    private static void deliver(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.TimeCapsule timeCapsule) {
        java.lang.String str;
        com.echoflow.chat.Letter letter = new com.echoflow.chat.Letter();
        letter.id = "ltr_cap_" + timeCapsule.id;
        letter.cardId = characterCard.id;
        letter.direction = timeCapsule.fromHer() ? "from_her" : "to_her";
        letter.to = timeCapsule.fromHer() ? "旅人" : characterCard.name;
        letter.greet = timeCapsule.fromHer() ? "这封信，是几天前的我写的。" : "这是你留给那一天的话。";
        letter.body = timeCapsule.content;
        letter.sign = timeCapsule.fromHer() ? characterCard.name : "那天的你";
        letter.dateLabel = timeCapsule.openAt;
        letter.ps = "";
        letter.createdAt = java.lang.System.currentTimeMillis();
        letter.read = false;
        com.echoflow.chat.LetterStore.save(context, characterCard.id, letter);
        com.echoflow.chat.Memory memory = new com.echoflow.chat.Memory();
        memory.id = "mem_cap_" + timeCapsule.id;
        memory.cardId = characterCard.id;
        if (timeCapsule.fromHer()) {
            str = "你打开了她在 " + timeCapsule.openAt + " 留给你的时间胶囊：「" + excerpt(timeCapsule.content, 40) + "」";
        } else {
            str = "你在 " + timeCapsule.openAt + " 写给未来的话被打开了：「" + excerpt(timeCapsule.content, 40) + "」";
        }
        memory.content = str;
        memory.importance = 5;
        memory.category = androidx.core.app.NotificationCompat.CATEGORY_EVENT;
        memory.source = "时间胶囊 · 开启日 " + timeCapsule.openAt;
        memory.time = java.lang.System.currentTimeMillis();
        com.echoflow.chat.MemoryStore.add(context, characterCard.id, memory);
        com.echoflow.chat.StoryEvent storyEvent = new com.echoflow.chat.StoryEvent();
        storyEvent.id = "evt_cap_" + timeCapsule.id;
        storyEvent.cardId = characterCard.id;
        storyEvent.title = "时间胶囊到期：" + timeCapsule.title;
        storyEvent.description = excerpt(timeCapsule.content, 80);
        storyEvent.date = java.lang.System.currentTimeMillis();
        storyEvent.autoGenerated = true;
        com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent);
    }

    private static java.lang.String excerpt(java.lang.String str, int i) {
        if (str == null) {
            return "";
        }
        java.lang.String trim = str.replace("\n", " ").trim();
        return trim.length() > i ? trim.substring(0, i) + "…" : trim;
    }
}
