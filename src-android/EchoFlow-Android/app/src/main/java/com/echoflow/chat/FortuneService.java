package com.echoflow.chat;

/* loaded from: classes.dex */
public class FortuneService {
    private static final java.lang.String[] BAD_POOL;
    private static final java.util.Map<java.lang.String, java.lang.String[]> DIM_NOTES;
    private static final java.lang.String[] DIRECTIONS;
    private static final java.lang.String[] GOOD_POOL;
    private static final java.lang.String[][] LUCKY_COLORS;
    private static final java.lang.String[] LUCKY_ITEMS;
    public static final com.echoflow.chat.FortuneService.Omamori[] OMAMORI;
    private static final java.util.Map<java.lang.String, java.lang.String[]> SUMMARY;
    static android.content.Context currentCtx;
    public static final java.lang.String[] RANKS = {"大吉", "吉", "中吉", "小吉", "末吉", "凶"};
    private static final double[] BASE = {0.05d, 0.23d, 0.5d, 0.75d, 0.93d, 1.0d};
    private static final int[][] RANGE = {new int[]{84, 100}, new int[]{70, 92}, new int[]{60, 86}, new int[]{50, 78}, new int[]{40, 68}, new int[]{26, 58}};
    private static final java.lang.String[] DIMENSIONS = {"恋爱", "学业", "健康", "财运", "人际", "灵感"};

    private static double clamp(double d, double d2, double d3) {
        return d < d2 ? d2 : d > d3 ? d3 : d;
    }

    private static int clamp(int i, int i2, int i3) {
        return i < i2 ? i2 : i > i3 ? i3 : i;
    }

    static {
        java.util.HashMap hashMap = new java.util.HashMap();
        DIM_NOTES = hashMap;
        hashMap.put("恋爱", new java.lang.String[]{"良缘渐近", "宜坦率", "静待", "有人一直在看你", "不必急于确认"});
        hashMap.put("学业", new java.lang.String[]{"稳步", "宜复习", "瓶颈将破", "有新的方法", "记得留余地"});
        hashMap.put("健康", new java.lang.String[]{"稍倦", "宜早睡", "状态回升", "注意肩颈", "多喝热水"});
        hashMap.put("财运", new java.lang.String[]{"平", "守成", "有小惊喜", "不宜冲动消费", "宜囤积"});
        hashMap.put("人际", new java.lang.String[]{"有贵人", "宜主动", "旧识重逢", "小事化了", "多听少说"});
        hashMap.put("灵感", new java.lang.String[]{"泉涌", "宜记录", "卡住了就出门", "换个角度", "半夜最清醒"});
        LUCKY_ITEMS = new java.lang.String[]{"银白的小物件", "一枚旧硬币", "带在身上的书", "温热的饮品", "一块手帕", "雨伞", "写了字的纸条", "小铃铛", "玻璃珠"};
        LUCKY_COLORS = new java.lang.String[][]{new java.lang.String[]{"紫藤", "#9B7BFF"}, new java.lang.String[]{"灯火橙", "#FFA657"}, new java.lang.String[]{"蔷薇", "#FF9BB0"}, new java.lang.String[]{"薄荷", "#7FE0C4"}, new java.lang.String[]{"天青", "#79D3F5"}, new java.lang.String[]{"新芽", "#B9E07F"}, new java.lang.String[]{"鸟居朱", "#E5544B"}, new java.lang.String[]{"御守金", "#E8C87A"}, new java.lang.String[]{"银白", "#D8D8E8"}};
        DIRECTIONS = new java.lang.String[]{"东", "东南", "南", "西南", "西", "西北", "北", "东北"};
        GOOD_POOL = new java.lang.String[]{"与人同行", "说出一直没说出口的话", "把想法写下来", "早一点休息", "接受别人的好意", "重新走一遍熟悉的路", "做一件很久没做的小事"};
        BAD_POOL = new java.lang.String[]{"独自淋雨", "深夜做决定", "把话憋着", "勉强自己撑到最后", "翻旧账", "在很累的时候答应别人", "一个人走没走过的小路"};
        java.util.HashMap hashMap2 = new java.util.HashMap();
        SUMMARY = hashMap2;
        hashMap2.put("大吉", new java.lang.String[]{"云开月明 · 凡事皆宜", "风和日暖 · 心想事成"});
        hashMap2.put("吉", new java.lang.String[]{"雨过天青 · 宜与人共行", "渐入佳境 · 顺其自然"});
        hashMap2.put("中吉", new java.lang.String[]{"不急不缓 · 稳中向好", "有得有失 · 好在有得"});
        hashMap2.put("小吉", new java.lang.String[]{"小有顺遂 · 守之则安", "细微之处 · 藏着好意"});
        hashMap2.put("末吉", new java.lang.String[]{"稍显阻滞 · 后必转晴", "先难后易 · 忍一时"});
        hashMap2.put("凶", new java.lang.String[]{"今日宜静 · 明日必转", "风大 · 但有人在旁边"});
        OMAMORI = new com.echoflow.chat.FortuneService.Omamori[]{new com.echoflow.chat.FortuneService.Omamori("enmusubi", "缘结守", "羁绊增长 +20%", "初始持有"), new com.echoflow.chat.FortuneService.Omamori("shizune", "心宁守", "她不容易转负面情绪", "初始持有"), new com.echoflow.chat.FortuneService.Omamori("yumemi", "梦见守", "主动消息概率提升", "初始持有"), new com.echoflow.chat.FortuneService.Omamori("gakushi", "学思守", "每次多记一条", "连续参拜 3 天"), new com.echoflow.chat.FortuneService.Omamori("koe", "声守", "更常发语音", "连续参拜 7 天"), new com.echoflow.chat.FortuneService.Omamori("kage", "影守", "更常发照片", "关系达「信赖」"), new com.echoflow.chat.FortuneService.Omamori("toki", "时守", "更记得节日与纪念日", "连续参拜 14 天"), new com.echoflow.chat.FortuneService.Omamori("tabi", "旅守", "剧情分支更丰富", "完成一次剧情总结")};
    }

    /* loaded from: classes.dex */
    public static class Omamori {
        public final java.lang.String effect;
        public final java.lang.String hint;
        public final java.lang.String id;
        public final java.lang.String name;

        Omamori(java.lang.String str, java.lang.String str2, java.lang.String str3, java.lang.String str4) {
            this.id = str;
            this.name = str2;
            this.effect = str3;
            this.hint = str4;
        }
    }

    public static com.echoflow.chat.FortuneService.Omamori findOmamori(java.lang.String str) {
        if (str == null) {
            return null;
        }
        for (com.echoflow.chat.FortuneService.Omamori omamori : OMAMORI) {
            if (omamori.id.equals(str)) {
                return omamori;
            }
        }
        return null;
    }

    public static com.echoflow.chat.Fortune todayOrDraw(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Persona persona, java.lang.String str, java.lang.String str2, java.lang.String str3, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.ShrineState shrineState, com.echoflow.chat.EmotionState emotionState, java.lang.Runnable runnable) {
        currentCtx = context;
        java.lang.String str4 = com.echoflow.chat.Fortune.today();
        com.echoflow.chat.Fortune load = load(context, characterCard.id, str4);
        if (load != null) {
            return load;
        }
        com.echoflow.chat.Fortune draw = draw(characterCard, relationshipState, shrineState, str4);
        save(context, characterCard.id, draw);
        applyConsequences(context, characterCard, draw, shrineState, relationshipState);
        if (runnable != null) {
            runnable.run();
        }
        generateReading(str, str2, str3, characterCard, relationshipState, draw, emotionState);
        return draw;
    }

    public static boolean visitedToday(android.content.Context context, java.lang.String str) {
        return load(context, str, com.echoflow.chat.Fortune.today()) != null;
    }

    private static com.echoflow.chat.Fortune draw(com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.ShrineState shrineState, java.lang.String str) {
        java.lang.String str2 = str + "|" + characterCard.id + "|silvermoon";
        double hash01 = hash01(str2);
        double d = 0.0d;
        double d2 = (relationshipState == null ? 0.0d : relationshipState.points / 100.0d) * 0.15d;
        com.echoflow.chat.FortuneService.Omamori findOmamori = shrineState == null ? null : findOmamori(shrineState.equipped);
        if (findOmamori != null && "enmusubi".equals(findOmamori.id)) {
            d = 0.06d;
        }
        int pickRankIndex = pickRankIndex(clamp(hash01 + d2 + d, 0.0d, 0.999d));
        java.lang.String str3 = RANKS[pickRankIndex];
        int[] iArr = RANGE[pickRankIndex];
        char c = 0;
        int round = (int) java.lang.Math.round(iArr[0] + ((iArr[1] - iArr[0]) * hash01(str2 + "|score")));
        com.echoflow.chat.Fortune fortune = new com.echoflow.chat.Fortune();
        fortune.id = "frt_" + str + "_" + characterCard.id;
        fortune.cardId = characterCard.id;
        fortune.date = str;
        fortune.slipNo = ((int) (hash01(str2 + "|no") * 108.0d)) + 1;
        fortune.rank = str3;
        fortune.score = round;
        fortune.drawnAt = java.lang.System.currentTimeMillis();
        java.lang.String[] strArr = SUMMARY.get(str3);
        fortune.summary = strArr[pickIndex(str2 + "|s", strArr.length)];
        int i = relationshipState == null ? 0 : relationshipState.points;
        java.lang.String[] strArr2 = DIMENSIONS;
        int length = strArr2.length;
        int i2 = 0;
        while (i2 < length) {
            java.lang.String str4 = strArr2[i2];
            double hash012 = hash01(str2 + "|dim|" + str4);
            int i3 = round;
            double d3 = iArr[0] + ((iArr[1] - iArr[0]) * hash012);
            if ("恋爱".equals(str4)) {
                d3 += (i - 50) * 0.16d;
            }
            int round2 = (int) java.lang.Math.round(clamp(d3, 20.0d, 99.0d));
            java.lang.String[] strArr3 = DIM_NOTES.get(str4);
            fortune.dimensions.add(new com.echoflow.chat.Fortune.Dim(str4, round2, strArr3[pickIndex(str2 + "|dim|" + str4, strArr3.length)]));
            i2++;
            round = i3;
            c = 0;
        }
        java.lang.String[][] strArr4 = LUCKY_COLORS;
        java.lang.String[] strArr5 = strArr4[pickIndex(str2 + "|c", strArr4.length)];
        fortune.luckyColorName = strArr5[0];
        fortune.luckyColorHex = strArr5[1];
        java.lang.String[] strArr6 = LUCKY_ITEMS;
        fortune.luckyItem = strArr6[pickIndex(str2 + "|i", strArr6.length)];
        java.lang.String[] strArr7 = DIRECTIONS;
        fortune.luckyDirection = strArr7[pickIndex(str2 + "|d", strArr7.length)];
        fortune.luckyNumber = ((int) (hash01(str2 + "|n") * 9.0d)) + 1;
        fortune.good.addAll(pickN(GOOD_POOL, str2 + "|good", 2));
        fortune.bad.addAll(pickN(BAD_POOL, str2 + "|bad", 2));
        fortune.bondScore = (int) java.lang.Math.round(clamp((i * 0.7d) + (round * 0.3d), 0.0d, 99.0d));
        fortune.bondRank = RANKS[pickRankIndex(fortune.bondScore / 100.0d)];
        return fortune;
    }

    private static void generateReading(java.lang.String str, java.lang.String str2, java.lang.String str3, final com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.RelationshipState relationshipState, final com.echoflow.chat.Fortune fortune, com.echoflow.chat.EmotionState emotionState) {
        if (str2 == null || str2.isEmpty()) {
            fortune.reading = fallbackReading(fortune);
            return;
        }
        boolean z = "凶".equals(fortune.rank) || "末吉".equals(fortune.rank);
        java.lang.StringBuilder sb = new java.lang.StringBuilder("你现在是 ");
        sb.append(characterCard.name).append("。");
        if (characterCard.personality != null && !characterCard.personality.isEmpty()) {
            sb.append("角色性格：").append(characterCard.personality);
        }
        if (relationshipState != null) {
            sb.append("\n【你们的关系】").append(relationshipState.levelName()).append("，亲密度 ").append(relationshipState.points).append("/100");
        }
        if (emotionState != null && emotionState.mood != null) {
            sb.append("\n【你当前的情绪】").append(emotionState.mood);
        }
        sb.append("\n【背景】你陪对方在银月神社求了一支签。\n签文：");
        sb.append(fortune.rank).append("\u3000综合 ").append(fortune.score).append("\u3000\"").append(fortune.summary).append("\"\n分项：");
        for (com.echoflow.chat.Fortune.Dim dim : fortune.dimensions) {
            sb.append(dim.key).append(" ").append(dim.value).append("\u3000");
        }
        sb.append("\n宜：").append(join(fortune.good)).append("\u3000忌：").append(join(fortune.bad));
        sb.append("\n\n【任务】用你的口吻，为对方解读这张签。\n要求：\n1. 两三句话，口语，带一处动作或神态的描写（用（）括起来）\n2. 必须提到签文里的一两个具体点（某个分项或某条宜忌），不要泛泛而谈");
        if (z) {
            sb.append("\n3. 这张签偏弱。不要用「没关系的」这类安慰套话 —— 你是个不太会说漂亮话的骑士。用你自己的方式表达「今天我在」，比如提出陪对方做点什么。");
        } else {
            sb.append("\n3. 这张签不错。可以带一点克制的高兴，但不要夸张。");
        }
        sb.append("\n4. 不要出戏，不要提「AI」「签文系统」「模型」\n5. 直接输出解读正文，不要引号、不要前缀");
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add(new com.echoflow.chat.Message("system", sb.toString()));
        arrayList.add(new com.echoflow.chat.Message("user", "请输出你的解读。"));
        com.echoflow.chat.ApiClient.streamChat(str, str2, str3, arrayList, new com.echoflow.chat.ApiClient.Callback() { // from class: com.echoflow.chat.FortuneService.1
            private java.lang.String buf = "";

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onChunk(java.lang.String str4) {
                this.buf = str4;
            }

            @Override // com.echoflow.chat.ApiClient.Callback
            public void onDone(boolean z2, java.lang.String str4) {
                java.lang.String str5 = this.buf;
                if (!z2 && str5 != null && !str5.trim().isEmpty()) {
                    fortune.reading = str5.trim().replaceAll("^[\"「『]|[\"」』]$", "");
                } else {
                    fortune.reading = com.echoflow.chat.FortuneService.fallbackReading(fortune);
                }
                android.content.Context context = com.echoflow.chat.FortuneService.currentCtx;
                if (context != null) {
                    com.echoflow.chat.FortuneService.save(context, characterCard.id, fortune);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static java.lang.String fallbackReading(com.echoflow.chat.Fortune fortune) {
        if ("凶".equals(fortune.rank) || "末吉".equals(fortune.rank)) {
            return "（她把签纸收进袖口）……今天风大。要走哪条路，我陪你。";
        }
        return "（她把签纸折好递回给你）签不错。那就按签上说的走。";
    }

    private static void applyConsequences(android.content.Context context, com.echoflow.chat.CharacterCard characterCard, com.echoflow.chat.Fortune fortune, com.echoflow.chat.ShrineState shrineState, com.echoflow.chat.RelationshipState relationshipState) {
        java.lang.String str = com.echoflow.chat.Fortune.today();
        java.lang.String shiftDate = shiftDate(str, -1);
        shrineState.cardId = characterCard.id;
        shrineState.streak = shiftDate.equals(shrineState.lastVisit) ? shrineState.streak + 1 : 1;
        shrineState.lastVisit = str;
        shrineState.totalVisits++;
        com.echoflow.chat.FortuneService.Omamori[] omamoriArr = OMAMORI;
        int length = omamoriArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            com.echoflow.chat.FortuneService.Omamori omamori = omamoriArr[i];
            if (!shrineState.unlocked.contains(omamori.id)) {
                if ("gakushi".equals(omamori.id)) {
                    if (shrineState.streak < 3) {
                    }
                    shrineState.unlocked.add(omamori.id);
                    com.echoflow.chat.StoryEvent storyEvent = new com.echoflow.chat.StoryEvent();
                    storyEvent.id = "evt_omamori_" + omamori.id + "_" + str;
                    storyEvent.cardId = characterCard.id;
                    storyEvent.title = "获得御守「" + omamori.name + "」";
                    storyEvent.description = omamori.effect + "。连参拜第 " + shrineState.streak + " 天，她在神社替你求来的。";
                    storyEvent.date = java.lang.System.currentTimeMillis();
                    storyEvent.autoGenerated = true;
                    com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent);
                } else if ("koe".equals(omamori.id)) {
                    if (shrineState.streak < 7) {
                    }
                    shrineState.unlocked.add(omamori.id);
                    com.echoflow.chat.StoryEvent storyEvent2 = new com.echoflow.chat.StoryEvent();
                    storyEvent2.id = "evt_omamori_" + omamori.id + "_" + str;
                    storyEvent2.cardId = characterCard.id;
                    storyEvent2.title = "获得御守「" + omamori.name + "」";
                    storyEvent2.description = omamori.effect + "。连参拜第 " + shrineState.streak + " 天，她在神社替你求来的。";
                    storyEvent2.date = java.lang.System.currentTimeMillis();
                    storyEvent2.autoGenerated = true;
                    com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent2);
                } else if ("toki".equals(omamori.id)) {
                    if (shrineState.streak < 14) {
                    }
                    shrineState.unlocked.add(omamori.id);
                    com.echoflow.chat.StoryEvent storyEvent22 = new com.echoflow.chat.StoryEvent();
                    storyEvent22.id = "evt_omamori_" + omamori.id + "_" + str;
                    storyEvent22.cardId = characterCard.id;
                    storyEvent22.title = "获得御守「" + omamori.name + "」";
                    storyEvent22.description = omamori.effect + "。连参拜第 " + shrineState.streak + " 天，她在神社替你求来的。";
                    storyEvent22.date = java.lang.System.currentTimeMillis();
                    storyEvent22.autoGenerated = true;
                    com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent22);
                } else if ("kage".equals(omamori.id)) {
                    if (relationshipState != null) {
                        if (relationshipState.level < 3) {
                        }
                        shrineState.unlocked.add(omamori.id);
                        com.echoflow.chat.StoryEvent storyEvent222 = new com.echoflow.chat.StoryEvent();
                        storyEvent222.id = "evt_omamori_" + omamori.id + "_" + str;
                        storyEvent222.cardId = characterCard.id;
                        storyEvent222.title = "获得御守「" + omamori.name + "」";
                        storyEvent222.description = omamori.effect + "。连参拜第 " + shrineState.streak + " 天，她在神社替你求来的。";
                        storyEvent222.date = java.lang.System.currentTimeMillis();
                        storyEvent222.autoGenerated = true;
                        com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent222);
                    }
                }
            }
            i++;
        }
        shrineState.history.add(str + "|" + fortune.rank);
        while (shrineState.history.size() > 7) {
            shrineState.history.remove(0);
        }
        com.echoflow.chat.ShrineStore.save(context, shrineState);
        com.echoflow.chat.Memory memory = new com.echoflow.chat.Memory();
        memory.id = "mem_frt_" + fortune.date;
        memory.cardId = characterCard.id;
        memory.content = fortune.date + " 在银月神社求了一支签：「" + fortune.rank + "」，签文是「" + fortune.summary + "」。";
        memory.importance = ("大吉".equals(fortune.rank) || "凶".equals(fortune.rank)) ? 4 : 3;
        memory.category = ("大吉".equals(fortune.rank) || "凶".equals(fortune.rank)) ? androidx.core.app.NotificationCompat.CATEGORY_EVENT : "fact";
        memory.source = "银月神社 · 第" + fortune.slipNo + "番";
        memory.time = java.lang.System.currentTimeMillis();
        com.echoflow.chat.MemoryStore.add(context, characterCard.id, memory);
        if (relationshipState != null) {
            com.echoflow.chat.FortuneService.Omamori findOmamori = findOmamori(shrineState.equipped);
            if (findOmamori != null) {
                "enmusubi".equals(findOmamori.id);
            }
            relationshipState.points = java.lang.Math.min(100, relationshipState.points + 1);
            relationshipState.lastInteraction = java.lang.System.currentTimeMillis();
            com.echoflow.chat.CharacterStateStore.saveRelationship(context, relationshipState);
        }
        if (shrineState.streak <= 0 || shrineState.streak % 7 != 0) {
            return;
        }
        com.echoflow.chat.StoryEvent storyEvent3 = new com.echoflow.chat.StoryEvent();
        storyEvent3.id = "evt_streak_" + shrineState.streak;
        storyEvent3.cardId = characterCard.id;
        storyEvent3.title = "连续参拜 " + shrineState.streak + " 天";
        storyEvent3.description = "她说：「……你居然真的每天都来。」（说完自己先转过身去了）";
        storyEvent3.date = java.lang.System.currentTimeMillis();
        storyEvent3.autoGenerated = true;
        com.echoflow.chat.EventStore.add(context, characterCard.id, storyEvent3);
    }

    private static java.io.File fortuneFile(android.content.Context context, java.lang.String str) {
        java.io.File file = new java.io.File(context.getFilesDir(), "fortunes");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new java.io.File(file, str + ".json");
    }

    public static com.echoflow.chat.Fortune load(android.content.Context context, java.lang.String str, java.lang.String str2) {
        for (com.echoflow.chat.Fortune fortune : list(context, str)) {
            if (str2.equals(fortune.date)) {
                return fortune;
            }
        }
        return null;
    }

    public static java.util.List<com.echoflow.chat.Fortune> list(android.content.Context context, java.lang.String str) {
        int i;
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.io.File fortuneFile = fortuneFile(context, str);
        if (!fortuneFile.exists()) {
            return arrayList;
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        try {
            java.io.FileReader fileReader = new java.io.FileReader(fortuneFile);
            try {
                char[] cArr = new char[4096];
                while (true) {
                    int read = fileReader.read(cArr);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(cArr, 0, read);
                }
                org.json.JSONArray jSONArray = new org.json.JSONArray(sb.toString());
                for (i = 0; i < jSONArray.length(); i++) {
                    org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        arrayList.add(com.echoflow.chat.Fortune.fromJson(optJSONObject));
                    }
                }
                fileReader.close();
            } finally {
            }
        } catch (java.lang.Exception unused) {
        }
        return arrayList;
    }

    public static void save(android.content.Context context, java.lang.String str, com.echoflow.chat.Fortune fortune) {
        java.util.List<com.echoflow.chat.Fortune> list = list(context, str);
        int i = 0;
        while (true) {
            if (i < list.size()) {
                if (list.get(i).date.equals(fortune.date)) {
                    list.set(i, fortune);
                    break;
                }
                i++;
            } else {
                list.add(fortune);
                break;
            }
        }
        if (list.size() > 90) {
            list = new java.util.ArrayList(list.subList(list.size() - 90, list.size()));
        }
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        java.util.Iterator<com.echoflow.chat.Fortune> it = list.iterator();
        while (it.hasNext()) {
            try {
                jSONArray.put(it.next().toJson());
            } catch (java.lang.Exception unused) {
            }
        }
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(fortuneFile(context, str));
            try {
                fileWriter.write(jSONArray.toString());
                fileWriter.close();
            } finally {
            }
        } catch (java.lang.Exception unused2) {
        }
    }

    public static double hash01(java.lang.String str) {
        int i = -2128831035;
        for (int i2 = 0; i2 < str.length(); i2++) {
            i = (i ^ str.charAt(i2)) * 16777619;
        }
        return ((i & 4294967295L) % 1000000) / 1000000.0d;
    }

    private static int pickIndex(java.lang.String str, int i) {
        if (i <= 0) {
            return 0;
        }
        int hash01 = (int) (hash01(str) * i);
        int i2 = hash01 >= 0 ? hash01 : 0;
        return i2 >= i ? i - 1 : i2;
    }

    static int pickRankIndex(double d) {
        int i = 0;
        while (true) {
            double[] dArr = BASE;
            if (i >= dArr.length) {
                return RANKS.length - 1;
            }
            if (d < dArr[i]) {
                return i;
            }
            i++;
        }
    }

    private static java.util.List<java.lang.String> pickN(java.lang.String[] strArr, java.lang.String str, int i) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (int i2 = 0; arrayList.size() < i && i2 < 32; i2++) {
            java.lang.String str2 = strArr[pickIndex(str + "|" + i2, strArr.length)];
            if (!arrayList.contains(str2)) {
                arrayList.add(str2);
            }
        }
        return arrayList;
    }

    public static java.lang.String shiftDate(java.lang.String str, int i) {
        try {
            java.lang.String[] split = str.split("-");
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.lang.Integer.parseInt(split[0]), java.lang.Integer.parseInt(split[1]) - 1, java.lang.Integer.parseInt(split[2]));
            calendar.add(5, i);
            return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.getTime());
        } catch (java.lang.Exception unused) {
            return str;
        }
    }

    private static java.lang.String join(java.util.List<java.lang.String> list) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
