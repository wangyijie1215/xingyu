package com.echoflow.chat;

/**
 * 手机模式的开场白。
 *
 * 为什么要单独写：角色卡里的 first_mes 是**角色扮演风格**的开场
 * （「（她把盾往墙边一靠）……你也是被雨困住的？"），
 * 里面带动作描写，直接放到手机模式里就破功了。
 *
 * 这里为内置角色各准备一句「像真人发的第一条消息"。
 * 自定义角色没有对应开场白时，退回到把 first_mes 去掉动作描写后的结果。
 */
public class PhonePromptGreeting {

    public static String first(CharacterCard card, Persona user) {
        if (card == null) {
            return "在吗？";
        }
        switch (card.id) {
            case "builtin_alice":
                return "……是我。\n今天巡夜路过你们那条街，就想着说一声。";
            case "builtin_rin":
                return "明天早上七点，道场。\n别迟到。";
            case "builtin_hakuyo":
                return "今晚云很少。\n你要不要上来看一眼。";
            case "builtin_rocco":
                return "喂！你上次那个东西我修好啦\n明天来拿？还是我给你送过去";
            case "builtin_elian":
                return "今天新到的一批里，有本你可能会喜欢的。\n什么时候过来坐坐。";
            case "builtin_yueling":
                return "今天又下雨了。\n你那边呢。";
            default:
                return stripStageDirections(PromptBuilder.applyVars(
                        card.firstMes, card, user));
        }
    }

    /**
     * 去掉（……）和 *……* 这两种动作描写，只留下真正会打字说出来的部分。
     * 兜底用：自定义角色的 first_mes 通常带动作描写。
     */
    static String stripStageDirections(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replaceAll("（[^）]*）", "")
                      .replaceAll("\\([^)]*\\)", "")
                      .replaceAll("\\*[^*]*\\*", "");
        // 清理多余空行
        s = s.replaceAll("\\n{3,}", "\n\n").trim();
        if (s.isEmpty()) {
            return "在吗？";
        }
        return s;
    }
}