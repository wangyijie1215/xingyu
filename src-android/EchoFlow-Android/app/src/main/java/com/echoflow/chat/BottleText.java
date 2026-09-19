package com.echoflow.chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 漂流瓶文案库。
 *
 * 内容取向：祝福 / 治愈 / 温暖。三条硬规矩：
 *
 *  1. 不说教。不出现「你应该」「你要相信」这类。
 *     被祝福的人不需要被指导，只需要被看见。
 *  2. 不空洞。要有具体的物、具体的时刻 —— 末班车、晒过的被子、楼下的猫。
 *  3. 不强行正能量。允许承认难过。
 *     「今天很难，那就难着」比「一切都会好起来」更让人松一口气。
 *
 * 所有文案都是为这个应用原创的。
 */
public class BottleText {

    public static class Bottle {
        public String id;
        public String text;
        public String tag;
        public String from;

        public Bottle(String id, String tag, String text, String from) {
            this.id = id;
            this.tag = tag;
            this.text = text;
            this.from = from;
        }
    }

    public static final Map<String, String> TAG_NAMES =
            new LinkedHashMap<String, String>() {{
                put("blessing", "祝福");
                put("heal", "治愈");
                put("warm", "温暖");
                put("night", "深夜");
                put("courage", "勇气");
            }};

    private static List<Bottle> ALL;

    public static List<Bottle> all() {
        if (ALL != null) {
            return ALL;
        }
        List<Bottle> l = new ArrayList<>();

        // ---------------- 祝福 ----------------
        l.add(new Bottle("b01", "blessing",
                "愿你明天醒得比闹钟早一点，\n躺在床上听一会儿窗外的声音，\n不用急着起。", null));
        l.add(new Bottle("b02", "blessing",
                "愿你等的那班车，\n在你跑到站台的那一刻正好停下。", null));
        l.add(new Bottle("b03", "blessing",
                "愿你点的外卖到得比预计早，\n汤还是烫的。", null));
        l.add(new Bottle("b04", "blessing",
                "愿你今天的会都按时结束，\n没有人说再补充一点。", null));
        l.add(new Bottle("b05", "blessing",
                "愿你洗完澡出来的时候，\n房间是暖的，头发能被风慢慢吹干。", null));
        l.add(new Bottle("b06", "blessing",
                "愿你今年的体检报告上，\n每一个箭头都是朝下的。", null));
        l.add(new Bottle("b07", "blessing",
                "愿你想见的人，\n也正在想办法见你。", null));
        l.add(new Bottle("b08", "blessing",
                "愿你手机里的余额，\n比你以为的多一点。", null));
        l.add(new Bottle("b09", "blessing",
                "愿你今晚做的梦，\n是你小时候最喜欢的那种。", null));
        l.add(new Bottle("b10", "blessing",
                "愿你在某个普通的下午，\n突然想起一件很久以前的好事，\n然后一个人笑了一下。", null));

        // ---------------- 治愈 ----------------
        l.add(new Bottle("h01", "heal",
                "你不必今天就振作起来。\n难过是有权利慢慢走的，\n它想待多久就待多久。", null));
        l.add(new Bottle("h02", "heal",
                "有些日子就是只能拿来熬的，\n熬过去了也不必感谢它。", null));
        l.add(new Bottle("h03", "heal",
                "你已经做得很好了。\n这句话不是安慰，\n是我看了很久才说的。", null));
        l.add(new Bottle("h04", "heal",
                "谁都会有那种时候：\n明明什么都没做错，\n但就是觉得自己很糟糕。\n那不是真的。", null));
        l.add(new Bottle("h05", "heal",
                "如果你今天只完成了一件事，\n那也算数。\n吃饭也算。", null));
        l.add(new Bottle("h06", "heal",
                "不用把自己修好了才去见人。\n大家都是带着裂缝在过的。", null));
        l.add(new Bottle("h07", "heal",
                "你不需要对得起谁的努力。\n你只要对得起你自己想过的生活。", null));
        l.add(new Bottle("h08", "heal",
                "休息不是奖励，\n是必需品。\n不是做完了才能休息。", null));
        l.add(new Bottle("h09", "heal",
                "有些事你确实做不到。\n这不是你的问题，\n是这件事本来就难。", null));
        l.add(new Bottle("h10", "heal",
                "你可以不喜欢现在的自己，\n但请先别放弃她。\n她也在很努力地撑。", null));
        l.add(new Bottle("h11", "heal",
                "哭出来不丢人。\n丢人的是那些让你觉得哭丢人的人。", null));
        l.add(new Bottle("h12", "heal",
                "如果今天很难，\n那就先难着。\n明天再说。", null));

        // ---------------- 温暖 ----------------
        l.add(new Bottle("w01", "warm",
                "想起小时候，\n冬天的被子晒过之后有太阳的味道。\n愿你今晚也能睡在那种味道里。", null));
        l.add(new Bottle("w02", "warm",
                "楼下那只猫今天又在同一个位置晒太阳。\n它不认识你，\n但它过得挺好。", null));
        l.add(new Bottle("w03", "warm",
                "有个人刚才在超市里，\n很认真地挑了一个最大的橘子。\n那个人可能是任何一个陌生人。\n我们都在认真地过日子。", null));
        l.add(new Bottle("w04", "warm",
                "你小时候一定有过这样的时刻：\n玩到天黑被叫回家，\n饭已经做好了。\n那个感觉一直都在，\n只是长大以后少有机会再碰到。", null));
        l.add(new Bottle("w05", "warm",
                "有人在雨天把伞往旁边挪了挪，\n让一个陌生人躲进来。\n这件事上个月真的发生过。", null));
        l.add(new Bottle("w06", "warm",
                "便利店的热柜里永远有包子。\n无论多晚，\n总有一个是热的。", null));
        l.add(new Bottle("w07", "warm",
                "如果你现在很累，\n就当这瓶子是一个人在路边坐下，\n拍拍旁边的位置说：\n坐会儿吧。", null));
        l.add(new Bottle("w08", "warm",
                "今天有风吹过你的窗子，\n你没注意。\n但它确实来过。", null));

        // ---------------- 深夜 ----------------
        l.add(new Bottle("n01", "night",
                "凌晨三点醒过来的时候，\n世界安静得像是只剩你一个。\n但其实不是。\n还有很多人和你一样醒着。", null));
        l.add(new Bottle("n02", "night",
                "熬夜的人有一种共同的温柔：\n明明自己也很累了，\n还是舍不得结束这一天。", null));
        l.add(new Bottle("n03", "night",
                "如果现在睡不着，\n就别硬躺了。\n起来喝口水，\n看看窗外。\n城市比你想象的更热闹。", null));
        l.add(new Bottle("n04", "night",
                "深夜里想的那些事，\n有一半第二天就不重要了。\n剩下一半，\n也未必有你当时觉得的那么严重。", null));
        l.add(new Bottle("n05", "night",
                "半夜还在忙的人，\n我祝你早点忙完。\n不忙完也行，\n先去睡。", null));

        // ---------------- 勇气 ----------------
        l.add(new Bottle("c01", "courage",
                "你有过那种瞬间吗：\n想说的话已经到嘴边了，\n又咽了回去。\n下一次试试说出来。\n哪怕只有一次。", null));
        l.add(new Bottle("c02", "courage",
                "辞职、搬家、分手、重新开始——\n这些事没有一件是轻松的。\n但你已经想过很多遍了，\n那说明它值得。", null));
        l.add(new Bottle("c03", "courage",
                "不必等到准备好了才开始。\n大多数人是边做边准备好的。", null));
        l.add(new Bottle("c04", "courage",
                "你不需要向任何人证明这个选择是对的。\n你只需要自己知道为什么。", null));
        l.add(new Bottle("c05", "courage",
                "有些门关上了不是坏事。\n是你站在那儿太久，\n而里面一直没有灯。", null));

        ALL = l;
        return ALL;
    }

    public static Bottle random() {
        List<Bottle> all = all();
        int i = (int) (Math.random() * all.size());
        return all.get(Math.max(0, Math.min(all.size() - 1, i)));
    }

    public static Bottle byId(String id) {
        for (Bottle b : all()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    public static String tagName(String tag) {
        String n = TAG_NAMES.get(tag);
        return n == null ? "祝福" : n;
    }
}
