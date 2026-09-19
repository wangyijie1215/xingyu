package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 诊断工具：列出**同一行里出现 3 个或以上引号**且含中文的行。
 *
 * 这类行是"嵌套引号被压平"的典型残骸，例如：
 *     button("生成「世界"场景图", ...)
 *                      ^ 这个引号把字符串提前闭合了
 *
 * 引号数恰好为偶数的坏行不会被"奇偶检查"发现，
 * 所以必须用"引号密度 + 中文相邻"来定位。
 *
 * 只读不写。
 */
public class Dense {

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
                int q = count(ln);
                if (q < 3) {
                    continue;
                }
                // 判断是否存在 "中文…"中文 这种"引号夹在中文之间"的形态
                boolean suspicious = false;
                for (int k = 0; k < ln.length() - 1; k++) {
                    if (ln.charAt(k) == '"') {
                        boolean leftCjk = k > 0 && isCjk(ln.charAt(k - 1));
                        boolean rightCjk = k + 1 < ln.length() && isCjk(ln.charAt(k + 1));
                        if (leftCjk && rightCjk) {
                            suspicious = true;
                            break;
                        }
                    }
                }
                if (suspicious) {
                    System.out.println(f.getName() + ":" + (i + 1) + "  " + t);
                    total++;
                }
            }
        }
        System.out.println("total " + total);
    }

    static boolean isCjk(char c) {
        return c >= 0x4e00 && c <= 0x9fff;
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
