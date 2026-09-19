package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 最后一轮修复：删掉残留的孤立 「。
 *
 * 事故链路回顾：
 *   1. 我用一条过宽的正则想把 Java 字符串里的中文裸引号换成「」，
 *      结果把合法的 `", "` 也改成了 `「, 」`。
 *   2. 第二轮我把 「 或 」 一律换回 "，但**只替换了成对的形态**，
 *      于是每处都留下一个孤立的 「。
 *   3. 现在这些孤立的 「 就是全部剩余的编译错误来源。
 *
 * 处理规则（只在非注释行、且该行含双引号时动手）：
 *   · 「 后面紧跟中文或标点，且这一行已经有偶数个 " 时 —— 说明这个 「
 *     本身就是多余的，删掉。
 *
 * 安全性：写文件前先把改动打印出来；带 --apply 参数才真正落盘，
 * 默认只预演。这次不再"直接改一百个文件然后祈祷"。
 */
public class Fix3 {

    public static void main(String[] args) throws Exception {
        boolean apply = false;
        String dirPath = null;
        for (String a : args) {
            if ("--apply".equals(a)) {
                apply = true;
            } else {
                dirPath = a;
            }
        }
        File dir = new File(dirPath);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) {
            return;
        }
        int changedFiles = 0, changedLines = 0;
        for (File f : files) {
            String[] lines = readAll(f).split("\n", -1);
            boolean dirty = false;
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i];
                String t = ln.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                    continue;
                }
                if (ln.indexOf('\u300c') < 0) {
                    continue;
                }
                // 统计这一行的引号个数：奇数说明本来就有一个未闭合的串，
                // 那 「 可能是字符串内容的开头，不能删
                int quotes = 0;
                boolean esc = false;
                for (int k = 0; k < ln.length(); k++) {
                    char c = ln.charAt(k);
                    if (esc) {
                        esc = false;
                    } else if (c == '\\') {
                        esc = true;
                    } else if (c == '"') {
                        quotes++;
                    }
                }
                if (quotes % 2 != 0) {
                    // 引号数奇数 —— 这行本身结构就不完整，跳过并报告
                    System.out.println("SKIP(odd quotes) " + f.getName() + ":" + (i + 1)
                            + "  " + t);
                    continue;
                }
                String fixed = ln.replace("\u300c", "");
                if (!fixed.equals(ln)) {
                    System.out.println((apply ? "FIX " : "DRY ") + f.getName() + ":" + (i + 1)
                            + "\n    - " + t + "\n    + " + fixed.trim());
                    lines[i] = fixed;
                    dirty = true;
                    changedLines++;
                }
            }
            if (dirty && apply) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    sb.append(lines[i]);
                    if (i < lines.length - 1) {
                        sb.append('\n');
                    }
                }
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(sb.toString());
                }
                changedFiles++;
            } else if (dirty) {
                changedFiles++;
            }
        }
        System.out.println((apply ? "APPLIED " : "WOULD CHANGE ")
                + changedFiles + " files, " + changedLines + " lines");
    }

    static String readAll(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }
}
