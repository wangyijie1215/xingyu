package com.echoflow.chat;

/* loaded from: classes.dex */
public class Schedule {

    /* loaded from: classes.dex */
    public static class Now {
        public boolean busy;
        public java.lang.String note;
        public java.lang.String what;
    }

    /* loaded from: classes.dex */
    public static class Item {
        public java.lang.String note;
        public java.lang.String time;
        public java.lang.String what;

        public Item(java.lang.String str, java.lang.String str2, java.lang.String str3) {
            this.time = str;
            this.what = str2;
            this.note = str3;
        }
    }

    public static java.util.List<com.echoflow.chat.Schedule.Item> builtin(java.lang.String str) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        if (str == null) {
            str = "";
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -307444203:
                if (str.equals("builtin_hakuyo")) {
                    c = 0;
                    break;
                }
                break;
            case -102216965:
                if (str.equals("builtin_rin")) {
                    c = 1;
                    break;
                }
                break;
            case 538132324:
                if (str.equals("builtin_alice")) {
                    c = 2;
                    break;
                }
                break;
            case 541826355:
                if (str.equals("builtin_elian")) {
                    c = 3;
                    break;
                }
                break;
            case 553915798:
                if (str.equals("builtin_rocco")) {
                    c = 4;
                    break;
                }
                break;
            case 1828583459:
                if (str.equals("builtin_yueling")) {
                    c = 5;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                arrayList.add(new com.echoflow.chat.Schedule.Item("04:00", "观测", "天快亮的那一段最好"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("09:00", "补觉", "这个时段基本不会回消息"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("14:00", "专业课", "天体力学的课，会睡着"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("17:00", "整理数据", "观测记录要当天录入"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("20:00", "上楼顶", "架望远镜，等云散"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("01:00", "还在看星星", "经常忘记时间"));
                return arrayList;
            case 1:
                arrayList.add(new com.echoflow.chat.Schedule.Item("06:00", "晨跑", "操场十圈，一周六天"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("07:30", "早自习", "会在教室后排补觉"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("12:20", "午饭", "通常是便利店饭团"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("15:40", "剑道部训练", "主将，全程盯着别人"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("19:00", "自主加练", "道场关灯前都在"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("21:30", "做作业", "一边骂一边写"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("23:00", "睡前", "一个人做拉伸"));
                return arrayList;
            case 2:
                arrayList.add(new com.echoflow.chat.Schedule.Item("05:30", "晨练", "营房后的空地，一个人"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("08:00", "换班交接", "听队长训话，通常很无聊"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("12:00", "午饭", "食堂，坐角落位置"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("14:00", "剑术训练", "这个时段回复会很慢"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("18:30", "擦洗装备", "铠甲要保养，她很在意"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("21:00", "巡夜", "旧城区到东门这一段"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("23:30", "准备休息", "睡前会把剑放在床边"));
                return arrayList;
            case 3:
                arrayList.add(new com.echoflow.chat.Schedule.Item("09:00", "开门", "先擦一遍柜台"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("11:00", "整理书架", "按她自己的逻辑排"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("15:00", "下午茶", "最闲的时候，适合聊天"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("19:30", "读样品书", "新到的书她会先看一遍"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("22:00", "打烊", "把没看完的书签好"));
                return arrayList;
            case 4:
                arrayList.add(new com.echoflow.chat.Schedule.Item("09:30", "开店", "门上的铃铛响一声"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("11:00", "拆东西", "手上永远有两个活"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("13:00", "午饭", "泡面，但会加个蛋"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("16:00", "接单", "街坊邻居都来找她修"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("19:00", "翻零件箱", "经常能找到宝贝"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("22:00", "关店", "但灯留着，继续做到困"));
                return arrayList;
            case 5:
                arrayList.add(new com.echoflow.chat.Schedule.Item("20:00", "出门", "灯是她唯一带的东西"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("22:00", "在老城区走", "沿着她说的那条路线"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("02:00", "在路口站着", "不做什么，就是在"));
                arrayList.add(new com.echoflow.chat.Schedule.Item("05:00", "天亮前离开", "白天找不到她"));
                return arrayList;
            default:
                arrayList.add(new com.echoflow.chat.Schedule.Item("08:00", "起床", ""));
                arrayList.add(new com.echoflow.chat.Schedule.Item("12:00", "午饭", ""));
                arrayList.add(new com.echoflow.chat.Schedule.Item("18:00", "傍晚", ""));
                arrayList.add(new com.echoflow.chat.Schedule.Item("23:00", "准备睡觉", ""));
                return arrayList;
        }
    }

    private static java.io.File scheduleFile(android.content.Context context, java.lang.String str) {
        java.io.File file = new java.io.File(context.getFilesDir(), "schedule");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new java.io.File(file, str + ".json");
    }

    public static java.util.List<com.echoflow.chat.Schedule.Item> load(android.content.Context context, java.lang.String str) {
        int i;
        java.io.File scheduleFile = scheduleFile(context, str);
        if (!scheduleFile.exists()) {
            return builtin(str);
        }
        try {
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            java.io.FileReader fileReader = new java.io.FileReader(scheduleFile);
            try {
                char[] cArr = new char[4096];
                while (true) {
                    int read = fileReader.read(cArr);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(cArr, 0, read);
                }
                fileReader.close();
                org.json.JSONArray jSONArray = new org.json.JSONArray(sb.toString());
                java.util.ArrayList arrayList = new java.util.ArrayList();
                for (i = 0; i < jSONArray.length(); i++) {
                    org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        arrayList.add(new com.echoflow.chat.Schedule.Item(optJSONObject.optString("time", ""), optJSONObject.optString("what", ""), optJSONObject.optString("note", "")));
                    }
                }
                return arrayList.isEmpty() ? builtin(str) : arrayList;
            } finally {
            }
        } catch (java.lang.Exception unused) {
            return builtin(str);
        }
    }

    public static void save(android.content.Context context, java.lang.String str, java.util.List<com.echoflow.chat.Schedule.Item> list) {
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        for (com.echoflow.chat.Schedule.Item item : list) {
            try {
                org.json.JSONObject jSONObject = new org.json.JSONObject();
                jSONObject.put("time", item.time);
                jSONObject.put("what", item.what);
                jSONObject.put("note", item.note == null ? "" : item.note);
                jSONArray.put(jSONObject);
            } catch (java.lang.Exception unused) {
            }
        }
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(scheduleFile(context, str));
            try {
                fileWriter.write(jSONArray.toString());
                fileWriter.close();
            } finally {
            }
        } catch (java.lang.Exception unused2) {
        }
    }

    public static com.echoflow.chat.Schedule.Now now(java.util.List<com.echoflow.chat.Schedule.Item> list) {
        com.echoflow.chat.Schedule.Now now = new com.echoflow.chat.Schedule.Now();
        if (list == null || list.isEmpty()) {
            now.what = "";
            now.busy = false;
            return now;
        }
        int minutesOf = minutesOf(new java.util.Date());
        com.echoflow.chat.Schedule.Item item = null;
        for (com.echoflow.chat.Schedule.Item item2 : list) {
            int parseMinutes = parseMinutes(item2.time);
            if (parseMinutes >= 0 && parseMinutes <= minutesOf) {
                item = item2;
            }
        }
        if (item == null) {
            item = list.get(list.size() - 1);
        }
        now.what = item.what;
        now.note = item.note;
        now.busy = isBusy(item.what);
        return now;
    }

    private static boolean isBusy(java.lang.String str) {
        if (str == null) {
            return false;
        }
        return str.contains("训练") || str.contains("课") || str.contains("观测") || str.contains("巡夜") || str.contains("接单") || str.contains("睡觉") || str.contains("补觉") || str.contains("开门") || str.contains("交接");
    }

    public static java.lang.String promptLine(java.util.List<com.echoflow.chat.Schedule.Item> list) {
        com.echoflow.chat.Schedule.Now now = now(list);
        if (now.what == null || now.what.isEmpty()) {
            return "";
        }
        java.lang.String format = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
        java.lang.StringBuilder sb = new java.lang.StringBuilder("现在是 ");
        sb.append(format).append("，你正在「").append(now.what).append("」");
        if (now.note != null && !now.note.isEmpty()) {
            sb.append("（").append(now.note).append("）");
        }
        if (now.busy) {
            sb.append("。这个时段你在忙，回复会更短、更可能让对方等一下。");
        }
        return sb.toString();
    }

    private static int parseMinutes(java.lang.String str) {
        try {
            java.lang.String[] split = str.split(":");
            return (java.lang.Integer.parseInt(split[0]) * 60) + java.lang.Integer.parseInt(split[1]);
        } catch (java.lang.Exception unused) {
            return -1;
        }
    }

    private static int minutesOf(java.util.Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        return (calendar.get(11) * 60) + calendar.get(12);
    }
}
