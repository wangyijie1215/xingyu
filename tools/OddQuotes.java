package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 找出**引号数奇数**的行 —— 这是"字符串里有裸引号"的确定特征。
 *
 * 只读不写。这次事故的教训是：改之前必须先有准确的诊断。
 */
public class OddQuotes {

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
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                    continue;
                }
                if (count(ln) % 2 != 0) {
                    System.out.println(f.getName() + ":" + (i + 1) + "  " + t);
                    total++;
                }
            }
        }
        System.out.println("total " + total);
    }

    static int count(String s) {
        int n = 0;
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) {
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                n++;
            }
        }
        return n;
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
