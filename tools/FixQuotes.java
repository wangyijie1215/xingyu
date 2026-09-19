package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 修复被正则误伤的 Java 源码。
 *
 * 事故背景：我用一条正则想「把 Java 字符串里的中文裸引号换成「」」，
 * 结果匹配过宽，把合法代码里的 `", "` 也改成了 `「, 」`。
 *
 * 本工具做的事：把误改的形态精确还原：
 *   「, 」   →  ", "
 *   「,\n    →  ",\n
 *   「,      →  "
 *   」,      →  "
 *
 * 之所以用 Java 而不是 PowerShell：PS 的 -replace 对含引号的替换串
 * 会报 "allows only two elements"，反而把问题搞得更乱。
 */
public class FixQuotes {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("need: <dir>");
            return;
        }
        File dir = new File(args[0]);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) {
            System.out.println("no files");
            return;
        }
        int fixed = 0;
        for (File f : files) {
            String src = readAll(f);
            String out = src
                    .replace("\u300c, \u300d", "\", \"")   // 「, 」 → ", "
                    .replace("\u300c,\r\n", "\",\r\n")     // 「, + CRLF
                    .replace("\u300c,\n", "\",\n")         // 「, + LF
                    .replace("\u300c,", "\",")             // 剩余 「,
                    .replace("\u300d,", "\",");            // 」,
            if (!out.equals(src)) {
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(out);
                }
                System.out.println("  fixed: " + f.getName());
                fixed++;
            }
        }
        System.out.println("total " + fixed);
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
