package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 按行区间替换 Java 源码里的某个方法。
 *
 * 为什么需要它：PowerShell 处理大段含中文的字符串替换时反复出错
 * （BOM、编码、转义）。用 Java 做精确的行区间替换，一次成功。
 *
 * 用法：java ... SwapMethod <源文件> <新方法体文件> <起始锚点> <结束锚点>
 */
public class SwapMethod {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("need: src body startAnchor endAnchor");
            return;
        }
        File src = new File(args[0]);
        String body = readAll(new File(args[1]));
        String startAnchor = args[2];
        String endAnchor = args[3];

        String[] lines = readAll(src).split("\n", -1);
        int start = -1, end = -1;
        for (int i = 0; i < lines.length; i++) {
            if (start < 0 && lines[i].contains(startAnchor)) {
                start = i;
            }
            if (start >= 0 && i > start && lines[i].contains(endAnchor)) {
                end = i;
                break;
            }
        }
        if (start < 0 || end < 0) {
            System.out.println("cannot locate: start=" + start + " end=" + end);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < start; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append(body);
        if (!body.endsWith("\n")) {
            sb.append('\n');
        }
        for (int i = end; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append('\n');
            }
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
