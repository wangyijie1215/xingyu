package com.echoflow.chat;

/* loaded from: classes.dex */
public class BuiltinLorebooks {
    private static final java.lang.String MARKER = ".lorebook_v1";

    /* loaded from: classes.dex */
    public static class Weather {
        public java.lang.String cond;
        public java.lang.String detail;
        public java.lang.String[] lines;
        public java.lang.String temp;

        Weather(java.lang.String str, java.lang.String str2, java.lang.String str3, java.lang.String[] strArr) {
            this.cond = str;
            this.temp = str2;
            this.detail = str3;
            this.lines = strArr;
        }
    }

    public static com.echoflow.chat.BuiltinLorebooks.Weather[] weathersOf(java.lang.String str) {
        if (str == null) {
            str = "";
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -1478386195:
                if (str.equals("world_observatory")) {
                    c = 0;
                    break;
                }
                break;
            case -607617887:
                if (str.equals("world_silver_knights")) {
                    c = 1;
                    break;
                }
                break;
            case 1123401099:
                if (str.equals("world_mech_shop")) {
                    c = 2;
                    break;
                }
                break;
            case 1745726674:
                if (str.equals("world_rainy_city")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return new com.echoflow.chat.BuiltinLorebooks.Weather[]{new com.echoflow.chat.BuiltinLorebooks.Weather("晴", "10~18℃", "云量不到一成，视宁度极好。", new java.lang.String[]{"今晚能看得很清楚。", "云量很低，是难得的天。", "九点之后最好。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("薄云", "11~19℃", "有一层薄云，星星只剩几颗亮的。", new java.lang.String[]{"云有点多。可能看不了。", "只有最亮的那几颗还在。", "再等等，云会散。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("阴", "9~16℃", "整片天都是云，什么都看不到。", new java.lang.String[]{"今天什么都看不到。", "云太厚了。", "只能在屋里整理数据。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("大晴", "12~21℃", "能见度极高，远处的山脊线都清楚。", new java.lang.String[]{"今天的透明度很好。", "能看到很远。", "适合拍长曝光。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("流星雨期", "8~15℃", "正赶上一年里最密的那几晚。", new java.lang.String[]{"今晚有流星。要不要一起看。", "每小时大概二十颗。", "记得穿厚一点，晚上很冷。"})};
            case 1:
                return new com.echoflow.chat.BuiltinLorebooks.Weather[]{new com.echoflow.chat.BuiltinLorebooks.Weather("晴", "8~16℃", "银月城的秋日，阳光薄薄地铺在石板上。", new java.lang.String[]{"今天适合出操。", "城墙上风大，但天很好。", "巡逻路上没什么人。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("大风", "5~12℃", "北风掠过城墙，旗子一直响。", new java.lang.String[]{"披风要系紧。", "旗子响了一整晚。", "风里有雪的味道。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("初雪", "-2~4℃", "第一场雪落下来了，很细，落在盔甲上就化。", new java.lang.String[]{"下雪了。营地里的人都在看。", "你的手会冷，别在外面站太久。", "雪不大，但会积起来。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("阴", "3~9℃", "没有太阳，城墙上的影子都是灰的。", new java.lang.String[]{"今天没什么光。", "适合在营房里待着。", "巡夜会冷。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("月夜", "1~7℃", "月很亮，广场上能看到自己的影子。", new java.lang.String[]{"月亮很亮。适合巡夜。", "今晚看得清路。", "你抬头看看。"})};
            case 2:
                return new com.echoflow.chat.BuiltinLorebooks.Weather[]{new com.echoflow.chat.BuiltinLorebooks.Weather("晴", "18~26℃", "太阳晒得铁皮发烫，铺子里有点闷。", new java.lang.String[]{"今天热。门得开着。", "铁皮晒得烫手。", "适合晒零件。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("潮", "20~27℃", "空气湿得发黏，零件一天就起锈点。", new java.lang.String[]{"这潮气，我那些零件又要生锈了。", "今天别碰铁的东西。", "烦死了，又得起锈。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("雷雨", "19~25℃", "下午三点一声闷雷，雨点砸在铁皮顶上很响。", new java.lang.String[]{"打雷了，屋顶响得要命。", "等下再说，雨太大。", "这种天最适合拆东西。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("多云", "17~24℃", "云把太阳挡着，是干活最舒服的天气。", new java.lang.String[]{"今天这天正好干活。", "不冷不热。", "适合在门口坐着。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("晴夜", "15~22℃", "入夜后凉下来，铺子的灯还亮着。", new java.lang.String[]{"晚上凉快多了。", "灯还开着，你要过来吗。", "再干一会儿就睡。"})};
            case 3:
                return new com.echoflow.chat.BuiltinLorebooks.Weather[]{new com.echoflow.chat.BuiltinLorebooks.Weather("小雨", "14~19℃", "雨丝斜着落，路面反着霓虹的光。", new java.lang.String[]{"伞我多带了一把。", "这种天走路要小心，地滑。", "雨不大，但会一直下。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("阴", "15~21℃", "云压得很低，天色比平时暗得早。", new java.lang.String[]{"今天可能还会下。", "云很厚，看不到星星。", "这种天最适合待在屋里。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("雨后", "13~18℃", "雨刚停，空气里有股干净的土味。", new java.lang.String[]{"雨停了。地上还是湿的。", "空气很好闻。", "出来走两步吧。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("多云", "16~22℃", "云在走，偶尔露出一点天光。", new java.lang.String[]{"云在动。", "今天算是好天气。", "有太阳的时候抬头看看。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("夜雾", "12~17℃", "雾从港口那边漫过来，路灯只剩一团光。", new java.lang.String[]{"雾很大。看不太清路。", "灯只剩一团黄。", "这种天别走远。"})};
            default:
                return new com.echoflow.chat.BuiltinLorebooks.Weather[]{new com.echoflow.chat.BuiltinLorebooks.Weather("晴", "16~24℃", "天气不错。", new java.lang.String[]{"今天天气挺好。", "太阳不错。", "适合出门。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("阴", "14~20℃", "天有点阴。", new java.lang.String[]{"今天有点阴。", "可能要下雨。", "不带伞也行。"}), new com.echoflow.chat.BuiltinLorebooks.Weather("小雨", "13~19℃", "下着小雨。", new java.lang.String[]{"下雨了。", "记得带伞。", "雨不大。"})};
        }
    }

    public static com.echoflow.chat.BuiltinLorebooks.Weather todayWeather(java.lang.String str, java.lang.String str2) {
        com.echoflow.chat.BuiltinLorebooks.Weather[] weathersOf = weathersOf(str);
        if (weathersOf.length == 0) {
            return null;
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        if (str == null) {
            str = "";
        }
        java.lang.String sb2 = sb.append(str).append("|").append(str2).toString();
        long j = 2166136261L;
        for (int i = 0; i < sb2.length(); i++) {
            j = (j ^ sb2.charAt(i)) * 16777619;
        }
        return weathersOf[(int) ((4294967295L & j) % weathersOf.length)];
    }

    private static java.lang.String weatherEntryContent(java.lang.String str) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder("这个世界的天气自成体系，与手机所在地的天气无关。");
        com.echoflow.chat.BuiltinLorebooks.Weather weather = todayWeather(str, com.echoflow.chat.Fortune.today());
        if (weather != null) {
            sb.append("今天是").append(weather.cond).append("，").append(weather.temp).append("。").append(weather.detail);
        }
        return sb.toString();
    }

    public static void installIfNeeded(android.content.Context context) {
        java.io.File file = new java.io.File(context.getFilesDir(), MARKER);
        if (file.exists()) {
            return;
        }
        install(context);
        try {
            file.createNewFile();
        } catch (java.lang.Exception unused) {
        }
    }

    public static void reinstall(android.content.Context context) {
        java.util.Iterator<com.echoflow.chat.Lorebook> it = all(context).iterator();
        while (it.hasNext()) {
            com.echoflow.chat.Lorebook.delete(context, it.next().id);
        }
        install(context);
    }

    public static java.util.List<com.echoflow.chat.Lorebook> all(android.content.Context context) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add(rainyCity(context));
        arrayList.add(silverKnights(context));
        arrayList.add(observatory(context));
        arrayList.add(mechShop(context));
        return arrayList;
    }

    private static void install(android.content.Context context) {
        java.util.Iterator<com.echoflow.chat.Lorebook> it = all(context).iterator();
        while (it.hasNext()) {
            com.echoflow.chat.Lorebook.save(context, it.next());
        }
        bind(context, "builtin_yueling", "world_rainy_city");
        bind(context, "builtin_elian", "world_rainy_city");
        bind(context, "builtin_alice", "world_silver_knights");
        bind(context, "builtin_hakuyo", "world_observatory");
        bind(context, "builtin_rocco", "world_mech_shop");
        bind(context, "builtin_rin", "world_mech_shop");
    }

    private static void bind(android.content.Context context, java.lang.String str, java.lang.String str2) {
        if (com.echoflow.chat.Lorebook.bindingsFor(context, str).isEmpty()) {
            com.echoflow.chat.Lorebook.bind(context, str, str2, true);
        }
    }

    private static com.echoflow.chat.Lorebook rainyCity(android.content.Context context) {
        com.echoflow.chat.Lorebook lorebook = new com.echoflow.chat.Lorebook();
        lorebook.id = "world_rainy_city";
        lorebook.name = "雨夜都市";
        lorebook.description = "现代都市，多雨。霓虹、便利店、末班电车。适合温柔、克制的日常戏。";
        lorebook.global = false;
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("世界基调", new java.lang.String[0], "故事发生在一座常年多雨的现代都市。城市沿着一条河展开，老城区在河北岸，新城区在南岸。夜里霓虹灯会在湿路面上拉出很长的倒影。", true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("天气体系", new java.lang.String[0], weatherEntryContent("world_rainy_city"), true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("老城区", new java.lang.String[]{"老城区", "河北", "巷子", "旧楼"}, "老城区在河北岸，楼都不高，六七层，外墙是那种洗不干净的水泥色。巷子很窄，晾衣绳横在头顶。晚上九点以后基本没人。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("末班电车", new java.lang.String[]{"电车", "末班", "车站", "站台"}, "有一条环线电车绕城一圈，末班是 23:40。站台上有一盏总在闪的灯。雨夜里车厢几乎没人，座位是旧的绒布。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("便利店", new java.lang.String[]{"便利店", "关东煮", "热咖啡"}, "街角有一家 24 小时便利店。关东煮的汤底从早煮到晚。夜班店员是个不爱说话的年轻人，认得常客。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("这个世界的雨", new java.lang.String[]{"雨", "下雨", "伞"}, "这座城市的雨有自己的脾气：不大，但会下很久。本地人出门基本不看天气预报，包里常年放折叠伞。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("旧钟楼", new java.lang.String[]{"钟楼", "报时"}, "广场上有一座停在 3:47 的旧钟楼，没人知道它为什么停。有说法是某年停电之后就再没修过。", false));
        return lorebook;
    }

    private static com.echoflow.chat.Lorebook silverKnights(android.content.Context context) {
        com.echoflow.chat.Lorebook lorebook = new com.echoflow.chat.Lorebook();
        lorebook.id = "world_silver_knights";
        lorebook.name = "银月骑士团";
        lorebook.description = "低魔奇幻。边境城邦、见习骑士、旧誓约。适合有使命感的戏。";
        lorebook.global = false;
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("世界基调", new java.lang.String[0], "故事发生在边境城邦银月城。王国衰落后，骑士团成了唯一还在守旧誓约的组织。魔法存在但稀有，多数人一辈子没见过。", true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("天气体系", new java.lang.String[0], weatherEntryContent("world_silver_knights"), true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("银月城", new java.lang.String[]{"银月城", "城墙", "城门"}, "银月城建在一座矮丘上，城墙是灰色的石头，东段有一处修补过的痕迹。城内主街铺着青石，雨天会很滑。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("见习骑士", new java.lang.String[]{"见习骑士", "骑士团", "见习"}, "见习骑士要轮值巡夜，两人一组，从旧城区到东门。铠甲要自己保养，每周检查一次。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("誓约", new java.lang.String[]{"誓约", "誓言", "契约"}, "骑士团的誓约有七条，其中第三条是「不为荣耀而战」。新人在入团那天要抄一遍，抄错一个字重来。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("旧誓约之剑", new java.lang.String[]{"剑", "配剑", "武器"}, "标准配剑是单手直刃，剑柄缠着旧皮革。剑是发的，不是自己的，退役要交还。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("北境的风", new java.lang.String[]{"北境", "风", "雪"}, "北境在城墙之外，是冻土和针叶林。每年入冬会有商队南下，带来毛皮和一种很烈的酒。", false));
        return lorebook;
    }

    private static com.echoflow.chat.Lorebook observatory(android.content.Context context) {
        com.echoflow.chat.Lorebook lorebook = new com.echoflow.chat.Lorebook();
        lorebook.id = "world_observatory";
        lorebook.name = "星见天文社";
        lorebook.description = "近未来校园科幻。山顶观测站、数据集、安静的深夜。";
        lorebook.global = false;
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("世界基调", new java.lang.String[0], "故事发生在一所大学的山顶校区。天文系有一个旧观测站，设备不算先进，但视野极好。这里的夜晚很长，也很安静。", true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("天气体系", new java.lang.String[0], weatherEntryContent("world_observatory"), true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("山顶观测站", new java.lang.String[]{"观测站", "天文台", "穹顶"}, "观测站的穹顶是 1980 年代装的，手动旋转，转起来有金属摩擦声。里面有一台老式反射望远镜，主镜口径 40 厘米。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("视宁度", new java.lang.String[]{"视宁度", "seeing", "透明度"}, "山里的大气稳定度比城里好得多。天文系的人见面第一句常常是「今晚视宁度怎么样」。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("数据集", new java.lang.String[]{"数据", "观测记录", "录入"}, "每次观测当晚就要把数据录进系统，拖到第二天很容易出错。系里有一句老话：「记录不过夜」。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("流星雨", new java.lang.String[]{"流星", "流星雨", "彗星"}, "每年有几次值得等的流星雨。观测站那几天会通宵亮着灯，社长会煮一大壶咖啡，谁都不许先睡。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("社里的规矩", new java.lang.String[]{"社团", "社里", "规矩"}, "星见天文社只有一条正式规矩：上楼顶必须关好门，因为风会把观测记录吹得到处都是。", false));
        return lorebook;
    }

    private static com.echoflow.chat.Lorebook mechShop(android.content.Context context) {
        com.echoflow.chat.Lorebook lorebook = new com.echoflow.chat.Lorebook();
        lorebook.id = "world_mech_shop";
        lorebook.name = "旧城机械铺";
        lorebook.description = "当代日常。旧城区、修理铺、街坊邻里。适合治愈、轻松的戏。";
        lorebook.global = false;
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("世界基调", new java.lang.String[0], "故事发生在旧城区一条以五金店和修理铺为主的街上。这里的东西都很旧，但都能修。人们习惯把坏了的东西留着，而不是换新的。", true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("天气体系", new java.lang.String[0], weatherEntryContent("world_mech_shop"), true));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("这条街", new java.lang.String[]{"旧城区", "这条街", "铺子"}, "街不宽，两边都是卷帘门。中午会有卖盒饭的推车经过，下午三点后基本没什么客人。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("零件箱", new java.lang.String[]{"零件", "零件箱", "螺丝"}, "铺子角落里堆着几个铁皮零件箱，是按尺寸分的，但所有人都随手扔，只有店主会认真归位。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("街坊", new java.lang.String[]{"街坊", "邻居", "修东西"}, "街坊之间修东西基本不收钱，但会在别的地方还回来 —— 一袋橘子、一碗汤、帮忙看半天店。", false));
        lorebook.entries.add(new com.echoflow.chat.Lorebook.Entry("门上的铃铛", new java.lang.String[]{"铃铛", "门铃"}, "铺子门上挂着一个铜铃，是旧货市场淘的。开关门会响一声，声音很脆。店主靠它知道有没有人来。", false));
        return lorebook;
    }
}
