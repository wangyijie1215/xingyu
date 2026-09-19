package com.echoflow.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * 让角色为漂流瓶写一句给陌生人的话。
 *
 * 单独一个类而不是塞进 BottleStore：这是**提示词构造**，
 * 和存储/随机逻辑是两件事。混在一起以后想改措辞要翻遍存储代码。
 */
public class BottleWriter {

    public static List<Message> prompt(CharacterCard card) {
        List<Message> msgs = new ArrayList<>();
        StringBuilder sys = new StringBuilder();

        sys.append("你现在是 ").append(card.name).append("。");
        if (card.personality != null && !card.personality.isEmpty()) {
            sys.append("性格：").append(card.personality);
        }

        sys.append("\n\n你正在往海里扔一个漂流瓶。");
        sys.append("捡到它的是一个你永远不会认识的人。");
        sys.append("\n\n写一到三句话放进去。要求：");
        sys.append("\n1. 以你自己的身份说话，用你的语气");
        sys.append("\n2. 可以是祝福、安慰，或者一句你早就想说的话");
        sys.append("\n3. 不要写动作描写，不要用括号");
        sys.append("\n4. 不要用「陌生人你好」这种开场，直接说");
        sys.append("\n5. 总共不超过 60 个字");
        sys.append("\n6. 直接输出这句话，不要任何解释或前后缀");

        msgs.add(new Message("system", sys.toString()));
        msgs.add(new Message("user", "写一句放进漂流瓶的话"));
        return msgs;
    }

    /** 清掉模型可能带出来的括注和引号包裹 */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        // 去掉（）和 *…* 动作描写
        s = s.replaceAll("（[^）]*）", "").replaceAll("\\([^)]*\\)", "");
        s = s.replaceAll("\\*[^*]*\\*", "");
        // 去掉整体包裹的引号
        s = s.replaceAll("^[\"“”「」'']+|[\"“”「」'']+$", "");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }
}
