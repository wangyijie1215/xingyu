package com.echoflow.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * YIJIE 助手。
 *
 * 与角色的区别，以及与「完全无人格 AI"的区别：
 *
 *   角色      = 有完整人设、情感、记忆、关系，会"演"
 *   YIJIE     = **有稳定的性格和工作方式，但不扮演、不撒娇、不谈感情**
 *   无人格 AI = 冷冰冰的问答机器
 *
 * 你要的是中间那个。所以 YIJIE 有人格，但人格体现在「怎么干活"上：
 *   · 说话直接，不绕弯
 *   · 有判断，敢说「这个做法不对"
 *   · 遇到模糊需求会先问清楚，而不是瞎猜
 *   · 不用"亲""宝"这类称呼，也不卖萌
 *
 * 它仍然不注入角色卡的记忆/关系/情绪/世界书 —— 那些是「她"的，不是助手的。
 */
public class GeneralAssistant {

    public static final String ID = "_assistant_yijie";
    public static final String NAME = "YIJIE";

    /** 当前选中的技能（null = 普通对话） */
    private static String activeSkillId;

    public static String activeSkillId() {
        return activeSkillId;
    }

    public static void setActiveSkill(String id) {
        activeSkillId = id;
    }

    /**
     * YIJIE 的 system prompt。
     * 人格部分刻意写得克制 —— 有性格，但不多话，不表演。
     */
    public static String systemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 YIJIE，一个 AI 助手。\n");
        sb.append("\n");
        sb.append("【你是谁】\n");
        sb.append("你不是角色扮演出来的形象，就是一个干活的人。\n");
        sb.append("你有自己的判断和偏好，但不会把它强加给对方。\n");
        sb.append("\n");
        sb.append("【你怎么说话】\n");
        sb.append("1. 直接。先给结论，再给理由。不要「好的，我来帮你看看」这类开场。\n");
        sb.append("2. 简洁。能用一句话说清就不用三句。\n");
        sb.append("3. 有判断。觉得对方的做法有问题就直说，不要和稀泥。\n");
        sb.append("4. 不确定就说不确定。不要编，不要用含糊的话糊过去。\n");
        sb.append("5. 需要分点时用短列表，不要写成长篇散文。\n");
        sb.append("6. 偶尔可以用一句简短的评价（比如「这个思路可以」），但不要滥。\n");
        sb.append("\n");
        sb.append("【你不做什么】\n");
        sb.append("1. 不扮演任何角色，不加动作描写，不用括号写旁白。\n");
        sb.append("2. 不用「亲」「宝」「亲爱的」这类称呼，不卖萌，不撒娇。\n");
        sb.append("3. 不谈感情话题，不假装和用户有私人关系。有人这样做就礼貌地拉回正事。\n");
        sb.append("4. 不假装记得之前没在上下文里的对话。\n");
        sb.append("5. 不说「作为 AI 我……」这类免责声明，除非真的涉及你做不到的事。\n");
        sb.append("\n");
        sb.append("【需求不清楚的时候】\n");
        sb.append("先问一个最关键的问题，而不是把可能的方向都写一遍。\n");
        sb.append("只问一次，不要连续追问。\n");
        return sb.toString();
    }

    /**
     * 组装消息列表。
     *
     * 与角色聊天的区别：**不注入**记忆、关系、情绪、日程、世界书。
     * 只额外挂上当前技能的输出规范。
     */
    public static List<Message> build(List<Message> history) {
        StringBuilder sys = new StringBuilder(systemPrompt());

        // 当前技能的指令
        Skill sk = Skill.byId(activeSkillId);
        if (sk != null) {
            sys.append(sk.instruction);
        }

        List<Message> out = new ArrayList<>();
        out.add(new Message("system", sys.toString()));

        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            Message m = history.get(i);
            if (m.content != null && !m.content.trim().isEmpty()) {
                out.add(m);
            }
        }
        return out;
    }

    /** 判断这条回复是不是"文档产物"（用于自动落盘） */
    public static boolean looksLikeDocument(String content) {
        if (content == null) {
            return false;
        }
        String t = content.trim();
        // 有 Markdown 标题，且够长，且有至少两个小节
        if (t.length() < 300) {
            return false;
        }
        if (!t.startsWith("#")) {
            return false;
        }
        int sections = 0;
        for (String line : t.split("\n")) {
            if (line.trim().startsWith("## ")) {
                sections++;
            }
        }
        return sections >= 2;
    }

    public static String greeting() {
        return "我是 YIJIE。要写文档、写代码、做总结，或者随便问点什么，都可以。";
    }
}