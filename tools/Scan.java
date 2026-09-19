package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 只报告、不修改：列出所有还在 Java 代码里（非注释、非字符串）的
 * 全角引号 「 」 残留位置，供人工确认。
 *
 * 这次事故的教训：**批量正则改源码之前必须先有备份或 dry-run**。
 * 这个工具就是 dry-run —— 只打印行号和上下文，绝不写文件。
 */
public class Scan {

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".java"));
        if (files == null) {
            return;
        }
        int total = 0;
        for (File f : files) {
            String[] lines = readAll(f).split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i];
                String t = ln.trim();
                // 跳过注释行
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                    continue;
                }
                if (ln.indexOf('\u300c') >= 0 || ln.indexOf('\u300d') >= 0) {
                    System.out.println(f.getName() + ":" + (i + 1) + "  " + t);
                    total++;
                }
            }
        }
        System.out.println("total " + total);
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
