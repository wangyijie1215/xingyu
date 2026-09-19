package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 修复被那条过宽正则误伤的 Java 源码（第二轮）。
 *
 * 三种残留形态（第一轮已修掉「, 」，这里补剩下的）：
 *   A) 」xxx"     行内起始引号被吃 → 应为 "xxx"
 *   B) "…「 : 」…" 三元表达式里的字符串边界被吃 → 应为 "…" : "…"
 *   C) put("恋爱" new String[]{...   参数间的逗号被吃 → 应为 put("恋爱", new ...
 *
 * 之所以用 Java 而不是 PowerShell：PS 处理含引号的替换串会报
 * "allows only two elements"，反而把源码改得更乱。
 */
public class Fix2 {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) {
            return;
        }
        int changed = 0;
        for (File f : files) {
            String s = readAll(f);
            String o = s;

            // C) put("key" new String[]{  →  put("key", new String[]{
            o = o.replaceAll("(put\\(\"[^\"]+\")\\s+(new\\s+String)", "$1, $2");

            // B) 三元表达式： "…「 : 」…"   →  "…" : "…"
            //    「 出现在字符串末尾、: 之后紧跟 」 的情况
            o = o.replaceAll("\u300c\\s*:\\s*\u300d", "\" : \"");
            // 单侧残留
            o = o.replace("\u300c : ", "\" : ");
            o = o.replace(" : \u300d", " : \"");

            // A) 行内 」 起始引号： "… 「xxx 」yyy" 形式 → 先处理孤立的 」
            //    把出现在 { 或 , 或 空格 之后、且不是成对的 」 还原成 "
            o = o.replaceAll("(\\{\\s*)\u300d", "$1\"");
            o = o.replaceAll("(,\\s*)\u300d", "$1\"");
            o = o.replaceAll("(:\\s*)\u300d", "$1\"");
            o = o.replaceAll("(\\?\\s*)\u300d", "$1\"");
            o = o.replaceAll("(\\[\\s*)\u300d", "$1\"");
            // 行尾孤立的 」 作为字符串开头
            o = o.replaceAll("\u300d(?=[^\"\\n]*(?:\"|\\n))", "\"");

            if (!o.equals(s)) {
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(o);
                }
                System.out.println("  fixed: " + f.getName());
                changed++;
            }
        }
        System.out.println("total " + changed);
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
