package com.echoflow.chat;

/* loaded from: classes.dex */
public class DiaryService {
    public static com.echoflow.chat.DiaryEntry todayOrGenerate(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.lang.String str, java.lang.String str2, java.lang.String str3, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, java.util.List<com.echoflow.chat.Memory> list, java.util.List<com.echoflow.chat.StoryEvent> list2, com.echoflow.chat.ShrineState shrineState, java.lang.Runnable runnable) {
        java.lang.String str4 = com.echoflow.chat.Fortune.today();
        com.echoflow.chat.DiaryEntry diaryEntry = com.echoflow.chat.DiaryStore.get(context, characterCard.id, str4);
        if (diaryEntry != null) {
            return diaryEntry;
        }
        com.echoflow.chat.DiaryEntry fallback = fallback(characterCard, relationshipState, emotionState, str4);
        com.echoflow.chat.DiaryStore.save(context, characterCard.id, fallback);
        if (str2 != null && !str2.isEmpty()) {
            generateAsync(context, characterCard, persona, str, str2, str3, relationshipState, emotionState, list, list2, shrineState, str4, fallback, runnable);
        }
        return fallback;
    }

    private static void generateAsync(final android.content.Context context, final com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.lang.String str, java.lang.String str2, java.lang.String str3, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState, java.util.List<com.echoflow.chat.Memory> list, java.util.List<com.echoflow.chat.StoryEvent> list2, com.echoflow.chat.ShrineState shrineState, java.lang.String str4, final com.echoflow.chat.DiaryEntry diaryEntry, final java.lang.Runnable runnable) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder("你现在是 ");
        sb.append(characterCard.name).append("。");
        if (characterCard.personality != null && !characterCard.personality.isEmpty()) {
            sb.append("角色性格：").append(characterCard.personality);
        }
        if (characterCard.scenario != null && !characterCard.scenario.isEmpty()) {
            sb.append("\n【场景设定】").append(characterCard.scenario);
        }
        sb.append("\n【你与对方的关系】").append(relationshipState.levelName()).append("，亲密度 ").append(relationshipState.points).append("/100\n【你今天的情绪】");
        sb.append(emotionState.mood == null ? "平静" : emotionState.mood);
        sb.append("\n【今天的时间】").append(str4);
        if (shrineState != null && shrineState.streak > 0) {
            sb.append("\n【最近的生活】你已经连续第 ").append(shrineState.streak).append(" 天陪对方去神社参拜了。");
        }
        int i = 0;
        if (list2 != null && !list2.isEmpty()) {
            sb.append("\n【今天发生的事】\n");
            int i2 = 0;
            for (com.echoflow.chat.StoryEvent storyEvent : list2) {
                sb.append("- ").append(storyEvent.title);
                if (storyEvent.description != null && !storyEvent.description.isEmpty()) {
                    sb.append("：").append(storyEvent.description);
                }
                sb.append("\n");
                i2++;
                if (i2 >= 4) {
                    break;
                }
            }
        } else {
            sb.append("\n【今天发生的事】\n- （今天没有特别的事发生）\n");
        }
        if (list != null && !list.isEmpty()) {
            sb.append("\n【你记得的事】\n");
            for (com.echoflow.chat.Memory memory : list) {
                if (memory.enabled) {
                    sb.append("- ").append(memory.content).append("\n");
                    i++;
                    if (i >= 5) {
                        break;
                    }
                }
            }
        }
        sb.append("\n\n【任务】写今天的日记。用第一人称，像真的在写给自己看。\n严格输出以下四行（每行以字段名开头，不要多余的解释、不要 markdown）：\nLINE: 今天的开场，一句具体有画面的话（不要写「今天天气很好」这种空话）\nBODY: 发生了什么，以及你当时的反应。2~3 句。要有细节，不要总结。\nUNSAID: 你有一句话没有对对方说出口。只有日记里才写。一句。\nTOMORROW: 明天打算做的一件小事。一句。\n\n要求：\n- 完全符合角色性格与说话习惯，不要变成通用的温柔 AI\n- 允许语句不完整、允许省略号，人写日记不会文绉绉\n- 不要提到「AI」「模型」「用户」「系统」\n- 不要写「亲爱的日记」这类套路开头");
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add(new com.echoflow.chat.Message("system", sb.toString()));
        arrayList.add(new com.echoflow.chat.Message("user", "请写今天的日记。"));
        com.echoflow.chat.ApiClient.streamChat(str, str2, str3, arrayList, new com.echoflow.chat.ApiClient.Callback() { // from class: com.echoflow.chat.DiaryService.1
            private java.lang.String buf = "";

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onChunk(java.lang.String str5) {
                this.buf = str5;
            }

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onDone(boolean z, java.lang.String str5) {
                java.lang.String str6;
                if (z || (str6 = this.buf) == null || str6.trim().isEmpty()) {
                    return;
                }
                com.echoflow.chat.DiaryService.parse(this.buf, diaryEntry, characterCard.name);
                diaryEntry.author = "auto";
                com.echoflow.chat.DiaryStore.save(context, characterCard.id, diaryEntry);
                java.lang.Runnable runnable2 = runnable;
                if (runnable2 != null) {
                    runnable2.run();
                }
            }
        });
    }

    public static void parse(java.lang.String str, com.echoflow.chat.DiaryEntry diaryEntry, java.lang.String str2) {
        java.lang.String pick = pick(str, "LINE");
        java.lang.String pick2 = pick(str, "BODY");
        java.lang.String pick3 = pick(str, "UNSAID");
        java.lang.String pick4 = pick(str, "TOMORROW");
        if (!pick.isEmpty()) {
            diaryEntry.line = clean(pick);
        }
        if (!pick2.isEmpty()) {
            diaryEntry.body = clean(pick2);
        }
        if (!pick3.isEmpty()) {
            diaryEntry.unsaid = clean(pick3);
        }
        if (pick4.isEmpty()) {
            return;
        }
        diaryEntry.tomorrow = clean(pick4);
    }

    private static java.lang.String pick(java.lang.String str, java.lang.String str2) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(str2 + "\\s*[:：]\\s*([\\s\\S]*?)(?=\\n\\s*(?:LINE|BODY|UNSAID|TOMORROW)\\s*[:：]|$)", 2).matcher(str);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return "";
        } catch (java.lang.Exception unused) {
            return "";
        }
    }

    private static java.lang.String clean(java.lang.String str) {
        return str.trim().replaceAll("^[\"「『“]|[\"」』”]$", "").replaceAll("\\*\\*(.+?)\\*\\*", "$1").replaceAll("^\\*|\\*$", "").trim();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x008a, code lost:
    
        if (r4.equals("孤独") == false) goto L15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static com.echoflow.chat.DiaryEntry fallback(com.echoflow.chat.CharacterCard r3, com.echoflow.chat.RelationshipState r4, com.echoflow.chat.EmotionState r5, java.lang.String r6) {
        /*
            Method dump skipped, instructions count: 282
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.echoflow.chat.DiaryService.fallback(com.echoflow.chat.CharacterCard, com.echoflow.chat.RelationshipState, com.echoflow.chat.EmotionState, java.lang.String):com.echoflow.chat.DiaryEntry");
    }

    public static int approxTokens(java.lang.String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < str.length(); i3++) {
            char charAt = str.charAt(i3);
            if (charAt < 19968 || charAt > 40959) {
                i2++;
            } else {
                i++;
            }
        }
        return i + java.lang.Math.max(1, i2 / 4);
    }
}
