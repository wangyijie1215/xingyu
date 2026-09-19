package com.echoflow.chat;

/* loaded from: classes.dex */
public class LetterService {
    public static com.echoflow.chat.Letter sendFromUser(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, java.lang.String str) {
        com.echoflow.chat.Letter letter = new com.echoflow.chat.Letter();
        letter.id = "ltr_u_" + java.lang.System.currentTimeMillis();
        letter.cardId = characterCard.id;
        letter.direction = "to_her";
        letter.to = characterCard.name;
        letter.greet = "";
        letter.body = str;
        letter.sign = "你";
        letter.dateLabel = todayLabel();
        letter.ps = "";
        letter.createdAt = java.lang.System.currentTimeMillis();
        letter.read = true;
        com.echoflow.chat.LetterStore.save(context, characterCard.id, letter);
        return letter;
    }

    public static void replyFromHer(final android.content.Context context, final com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.lang.String str, java.lang.String str2, java.lang.String str3, final com.echoflow.chat.RelationshipState relationshipState, final com.echoflow.chat.EmotionState emotionState, java.util.List<com.echoflow.chat.Memory> list, java.util.List<com.echoflow.chat.StoryEvent> list2, java.lang.String str4, final java.lang.Runnable runnable) {
        final com.echoflow.chat.Letter fallback = fallback(characterCard, relationshipState, emotionState);
        com.echoflow.chat.LetterStore.save(context, characterCard.id, fallback);
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
            sb.append("角色性格：").append(characterCard.personality);
        }
        if (characterCard.scenario != null && !characterCard.scenario.isEmpty()) {
            sb.append("\n【场景设定】").append(characterCard.scenario);
        }
        sb.append("\n【你们的关系】").append(relationshipState.levelName()).append("，亲密度 ").append(relationshipState.points).append("/100\n【你当前的情绪】");
        sb.append(emotionState.mood == null ? "平静" : emotionState.mood);
        sb.append("\n【现在的时间】").append(todayLabel());
        java.util.ArrayList arrayList = new java.util.ArrayList();
        if (list != null) {
            for (com.echoflow.chat.Memory memory : list) {
                if (memory.enabled && arrayList.size() < 5) {
                    arrayList.add(memory.content);
                }
            }
        }
        if (!arrayList.isEmpty()) {
            sb.append("\n【你记得的事】\n");
            java.util.Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                sb.append("- ").append((java.lang.String) it.next()).append("\n");
            }
        }
        java.util.ArrayList arrayList2 = new java.util.ArrayList();
        if (list2 != null) {
            for (com.echoflow.chat.StoryEvent storyEvent : list2) {
                if (arrayList2.size() >= 3) {
                    break;
                } else {
                    arrayList2.add("- " + storyEvent.title + "：" + (storyEvent.description == null ? "" : storyEvent.description));
                }
            }
        }
        if (!arrayList2.isEmpty()) {
            sb.append("\n【你们经历过的事】\n");
            java.util.Iterator it2 = arrayList2.iterator();
            while (it2.hasNext()) {
                sb.append((java.lang.String) it2.next()).append("\n");
            }
        }
        if (str4 != null && !str4.trim().isEmpty()) {
            sb.append("\n【你刚收到的信】\n").append(str4.trim());
        }
        sb.append("\n\n【任务】写一封回信。\n严格输出以下四行（每行以字段名开头，不要多余解释、不要 markdown）：\nGREET: 开头的一句短问候（不要写「亲爱的」这类套话）\nBODY: 信的正文。3~5 段，段与段之间空一行。\nSIGN: 落款（只有名字）\nPS: 一句附言，以「P.S.」开头\n\n要求：\n- 用写字的语气，不是说话的语气：句子可以更长，可以停顿，可以用省略号\n- 必须至少具体引用一件【你记得的事】或【你们经历过的事】，不要泛泛而谈\n- 这是你比较少有的能好好说话的机会，因此可以说出平时不会当面说的话\n- 允许写不下去：「……就写到这吧。」是好的结尾\n- 段落要短，人写信不会写一堵墙\n- 不要提到「AI」「模型」「用户」「系统」");
        java.util.ArrayList arrayList3 = new java.util.ArrayList();
        arrayList3.add(new com.echoflow.chat.Message("system", sb.toString()));
        arrayList3.add(new com.echoflow.chat.Message("user", "请写这封回信。"));
        com.echoflow.chat.ApiClient.streamChat(str, str2, str3, arrayList3, new com.echoflow.chat.ApiClient.Callback() { // from class: com.echoflow.chat.LetterService.1
            private java.lang.String buf = "";

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onChunk(java.lang.String str5) {
                this.buf = str5;
            }

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onDone(boolean z, java.lang.String str5) {
                java.lang.String str6;
                if (!z && (str6 = this.buf) != null && !str6.trim().isEmpty()) {
                    com.echoflow.chat.LetterService.parseInto(fallback, this.buf, characterCard.name);
                    fallback.basisEmotion = emotionState.mood == null ? "平静" : emotionState.mood;
                    fallback.basisBond = relationshipState.points;
                    com.echoflow.chat.LetterStore.save(context, characterCard.id, fallback);
                }
                java.lang.Runnable runnable2 = runnable;
                if (runnable2 != null) {
                    runnable2.run();
                }
            }
        });
    }

    public static void parseInto(com.echoflow.chat.Letter letter, java.lang.String str, java.lang.String str2) {
        java.lang.String pick = pick(str, "GREET");
        java.lang.String pick2 = pick(str, "BODY");
        java.lang.String pick3 = pick(str, "SIGN");
        java.lang.String pick4 = pick(str, "PS");
        if (!pick.isEmpty()) {
            letter.greet = clean(pick);
        }
        if (!pick2.isEmpty()) {
            letter.body = clean(pick2).replaceAll("\\n{3,}", "\n\n");
        }
        if (!pick3.isEmpty()) {
            str2 = clean(pick3);
        }
        letter.sign = str2;
        if (pick4.isEmpty()) {
            return;
        }
        java.lang.String clean = clean(pick4);
        if (!clean.matches("(?i)^p\\.?s\\.?.*")) {
            clean = "P.S. " + clean;
        }
        letter.ps = clean;
    }

    private static java.lang.String pick(java.lang.String str, java.lang.String str2) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(str2 + "\\s*[:：]\\s*([\\s\\S]*?)(?=\\n\\s*(?:GREET|BODY|SIGN|PS)\\s*[:：]|$)", 2).matcher(str);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return "";
        } catch (java.lang.Exception unused) {
            return "";
        }
    }

    private static java.lang.String clean(java.lang.String str) {
        return str.trim().replaceAll("^[\"「『“]|[\"」』”]$", "").trim();
    }

    public static com.echoflow.chat.Letter fallback(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState) {
        com.echoflow.chat.Letter letter = new com.echoflow.chat.Letter();
        letter.id = "ltr_h_" + java.lang.System.currentTimeMillis();
        letter.cardId = characterCard.id;
        letter.direction = "from_her";
        letter.to = "旅人";
        letter.greet = "见字如面。";
        letter.body = "今天没发生什么特别的事。\n\n只是坐下来的时候，忽然想给你写点什么。\n\n写了两行，又把纸揉了。再来一次。\n\n……就写到这吧。";
        letter.sign = characterCard.name;
        letter.dateLabel = todayLabel();
        letter.ps = "P.S. 明天见。";
        letter.createdAt = java.lang.System.currentTimeMillis();
        letter.read = false;
        letter.basisEmotion = (emotionState == null || emotionState.mood == null) ? "平静" : emotionState.mood;
        letter.basisBond = relationshipState != null ? relationshipState.points : 0;
        return letter;
    }

    private static java.lang.String todayLabel() {
        return new java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(new java.util.Date());
    }

    public static java.lang.String excerptOf(java.lang.String str, int i) {
        if (str == null) {
            return "";
        }
        java.lang.String trim = str.replace("\n", " ").trim();
        return trim.length() > i ? trim.substring(0, i) + "…" : trim;
    }
}
