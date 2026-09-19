package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 用 Python 风格的脚本替换 PhoneAppActivity 里的 weatherBody 方法。
 * 之所以用 Java 而不是 PowerShell：PS 处理大段含 CJK 的字符串替换时
 * 反复出问题（BOM、转义、编码）。这里做一次精确的行区间替换。
 *
 * 用法：java ReplaceWeather <源文件> <新方法体文件>
 */
public class ReplaceWeather {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("need src and body file");
            return;
        }
        File src = new File(args[0]);
        String body = readAll(new File(args[1]));
        String[] lines = readAll(src).split("\n", -1);

        // 找 weatherBody 起始行与 notesBody 起始行（0-based）
        int start = -1, end = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("private View weatherBody()")) start = i;
            if (start >= 0 && lines[i].contains("private View notesBody()")) {
                end = i;
                break;
            }
        }
        if (start < 0 || end < 0) {
            System.out.println("cannot locate methods: start=" + start + " end=" + end);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < start; i++) sb.append(lines[i]).append('\n');
        sb.append(body);
        if (!body.endsWith("\n")) sb.append('\n');
        for (int i = end; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append('\n');
        }

        try (java.io.FileWriter fw = new java.io.FileWriter(src)) {
            fw.write(sb.toString());
        }
        System.out.println("replaced lines " + (start + 1) + ".." + end);
    }

    static String readAll(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) sb.append('\n');
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }
}
