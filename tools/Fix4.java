package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 彻底修复被误伤的源码（最终版）。
 *
 * ── 事故与修复的完整链路 ──────────────────────────
 * 事故：一条过宽正则把 Java 里合法的 `", "` 改成了 `「, 」`，
 *       把字符串边界的 `"` 也吃掉了。
 *
 * 我的修复必须做两件事，缺一不可：
 *   1. 删掉所有残留的 「
 *   2. **补回被吃掉的 "**
 *
 * 只做第 1 步会留下 `... 「abc"。` 这种缺引号的残骸 —— 这正是前两轮
 * 修完还是 100 个错误的原因。
 *
 * ── 判定规则（逐行）────────────────────────────
 *   · 注释行不动
 *   · 不含 「 的行不动
 *   · 含 「 的行：先删 「，再看引号奇偶
 *       - 偶数：说明删完就合法，直接采用
 *       - 奇数：说明缺一个 "，在「原位置处补一个 "
 *
 * 默认只预演（打印 diff），加 --apply 才落盘。
 */
public class Fix4 {

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
        int nFiles = 0, nLines = 0, nSkip = 0;
        for (File f : files) {
            String[] lines = readAll(f).split("\n", -1);
            boolean dirty = false;
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i];
                String t = ln.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                    continue;
                }
                int idx = ln.indexOf('\u300c');
                if (idx < 0) {
                    continue;
                }

                // 先删掉所有 「
                String removed = ln.replace("\u300c", "");
                String candidate;
                if (countQuotes(removed) % 2 == 0) {
                    candidate = removed;
                } else {
                    // 缺一个引号：在第一个 「 的位置补上 "
                    StringBuilder sb = new StringBuilder(removed);
                    sb.insert(Math.min(idx, removed.length()), '"');
                    candidate = sb.toString();
                    if (countQuotes(candidate) % 2 != 0) {
                        System.out.println("SKIP " + f.getName() + ":" + (i + 1) + "  " + t);
                        nSkip++;
                        continue;
                    }
                }
                if (!candidate.equals(ln)) {
                    System.out.println((apply ? "FIX  " : "DRY  ") + f.getName() + ":" + (i + 1)
                            + "\n     - " + t + "\n     + " + candidate.trim());
                    lines[i] = candidate;
                    dirty = true;
                    nLines++;
                }
            }
            if (dirty) {
                nFiles++;
                if (apply) {
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
                }
            }
        }
        System.out.println((apply ? "APPLIED " : "WOULD FIX ")
                + nFiles + " files, " + nLines + " lines, skipped " + nSkip);
    }

    static int countQuotes(String s) {
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
