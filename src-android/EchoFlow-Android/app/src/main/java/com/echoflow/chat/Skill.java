package com.echoflow.chat;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * YIJIE 助手的技能插件系统。
 *
 * 设计取向：**技能是「提示词 + 输出格式约定"的组合，不是真正的工具调用。**
 *
 * 为什么这样做：
 *   · 真正的 function calling 需要模型支持并且在客户端解析流式 JSON，
 *     本地 7B 模型经常吐不出合法 JSON，反而更不可靠
 *   · 而"给模型一套明确的输出规范 + 把结果写成文件"能达到同样效果，
 *     且在任何模型上都稳定
 *
 * 所以每个 Skill 做的事是：
 *   1. 定义一段**追加到 system 的指令**（规定要输出什么格式）
 *   2. 定义**产物落盘的位置**（写文档 → files/docs/*.md）
 *   3. 在对话里显示一个「产物卡片"，用户可以点开看 / 导出
 */
public class Skill {

    public String id;
    public String name;
    public String desc;
    public String icon;
    /** 追加到 system 的输出规范 */
    public String instruction;

    public Skill(String id, String name, String desc, String icon, String instruction) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.icon = icon;
        this.instruction = instruction;
    }

    // ==================================================================
    // 内置技能
    // ==================================================================

    public static List<Skill> all() {
        List<Skill> out = new ArrayList<>();

        out.add(new Skill("doc", "写文档", "生成 Markdown 文档并保存到应用内", "📄",
                "\n\n【当前技能：写文档】\n"
                        + "用户要一份文档。请输出**完整、可用的 Markdown**，要求：\n"
                        + "1. 用 # 一级标题开头，给出文档标题\n"
                        + "2. 按内容合理分节（## / ###），不要全堆在一段里\n"
                        + "3. 该用表格的地方用表格，该用列表的地方用列表\n"
                        + "4. 代码用 ``` 代码块并标注语言\n"
                        + "5. 不要写「以下是文档内容」这类前言，直接给 Markdown 正文\n"
                        + "6. 内容要具体、可执行，不要写空泛的套话"));

        out.add(new Skill("code", "写代码", "生成代码并给出解释", "💻",
                "\n\n【当前技能：写代码】\n"
                        + "1. 先给完整可运行的代码，用 ``` 标注语言\n"
                        + "2. 再简短说明关键点（不超过 5 条）\n"
                        + "3. 如果有明显的坑或边界情况，指出来\n"
                        + "4. 不要写「好的，以下是代码」这类废话"));

        out.add(new Skill("summary", "总结", "把长内容压缩成要点", "📝",
                "\n\n【当前技能：总结】\n"
                        + "1. 先用一句话说结论\n"
                        + "2. 再给 3~7 条要点，每条不超过 25 字\n"
                        + "3. 保留关键数字和专有名词\n"
                        + "4. 不要加「总结如下」这类引导语"));

        out.add(new Skill("translate", "翻译", "中英互译，保留格式", "🌐",
                "\n\n【当前技能：翻译】\n"
                        + "1. 先把译文完整给出\n"
                        + "2. 保留原文的段落、列表、代码块结构\n"
                        + "3. 专有名词首次出现时用「中文（English）」形式\n"
                        + "4. 如果原文有歧义，在最后用一行说明\n"
                        + "5. 直接给译文，不要复述原文"));

        out.add(new Skill("plan", "做计划", "把目标拆成可执行步骤", "📋",
                "\n\n【当前技能：做计划】\n"
                        + "1. 先明确目标和产出物\n"
                        + "2. 拆成有顺序的步骤，每步说清「做什么」和「完成标志」\n"
                        + "3. 标出关键依赖和风险点\n"
                        + "4. 用有序列表，不要写成散文"));

        out.add(new Skill("analyze", "分析", "拆解问题、给判断", "🔍",
                "\n\n【当前技能：分析】\n"
                        + "1. 先复述你要解决的核心问题（一句话）\n"
                        + "2. 列出关键因素，说明哪个是主要矛盾\n"
                        + "3. 给出明确判断，不要「看情况」这类和稀泥\n"
                        + "4. 如果信息不足，说清缺什么、怎么补"));

        return out;
    }

    public static Skill byId(String id) {
        if (id == null) {
            return null;
        }
        for (Skill s : all()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    // ==================================================================
    // 产物落盘
    // ==================================================================

    private static File docDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "docs");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    /**
     * 把写文档技能的产物存成 Markdown 文件。
     *
     * @return 保存后的文件名；失败返回 null
     */
    public static String saveDocument(Context ctx, String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        // 文件名取正文里的第一个标题，没有就用时间
        String title = null;
        for (String line : content.split("\n")) {
            String t = line.trim();
            if (t.startsWith("#")) {
                title = t.replaceAll("^#+\\s*", "").trim();
                break;
            }
        }
        String safe = title == null ? "doc_" + stamp
                : title.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        if (safe.length() > 40) {
            safe = safe.substring(0, 40);
        }
        File f = new File(docDir(ctx), safe + "_" + stamp + ".md");
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
        } catch (Exception e) {
            return null;
        }
        return f.getName();
    }

    public static File docFile(Context ctx, String name) {
        return new File(docDir(ctx), name);
    }

    public static List<File> documents(Context ctx) {
        List<File> out = new ArrayList<>();
        File[] fs = docDir(ctx).listFiles();
        if (fs != null) {
            for (File f : fs) {
                if (f.getName().endsWith(".md")) {
                    out.add(f);
                }
            }
        }
        // 新的在前
        out.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return out;
    }

    public static void deleteDoc(Context ctx, String name) {
        File f = docFile(ctx, name);
        if (f.exists()) {
            f.delete();
        }
    }
}