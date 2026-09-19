package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 修复「我在字符串里插入了多余引号」的问题。
 *
 * ── 到这一步才看清原始意图 ──────────────────────
 * 原代码里中文引号有两种合法用法：
 *   A) 字符串**内容**里的引号，用全角：「收下」   ← 正确，不该动
 *   B) 字符串**边界**的引号，用半角："收下"       ← 也正确，不该动
 *
 * 我第一轮把 A 改成了 B 的一半（「 → 删、」 → "），
 * 第二轮又在缺引号处补 "，于是造出 `就点"收下"。` 这种
 * 引号错位的残骸。
 *
 * ── 本轮的修法 ────────────────────────────────
 * 目标形态：**全角内容引号**（A 类）。
 *   把 `"...就点"收下"。")` 还原成 `"...就点「收下」。")`
 *   即：一对把中文词包起来的半角引号 → 换成「」
 *
 * 判定：正则找 `"([^"\n]{1,30})"` 其中两侧紧邻的是中文/标点，
 * 且**不是**字符串的边界（后面不能紧跟 , ) ; + 等语法符号）。
 *
 * 默认预演；--apply 落盘。
 */
public class Fix5 {

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

        int nFiles = 0, nLines = 0;
        for (File f : files) {
            String[] lines = readAll(f).split("\n", -1);
            boolean dirty = false;
            for (int i = 0; i < lines.length; i++) {
                String ln = lines[i];
                String t = ln.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
                    continue;
                }
                // 只处理引号数为奇数、或引号明显错位的行
                String out = ln;
                // 形如：点"收下"。    → 点「收下」。
                //      写"陌生人你好"这种 → 写「陌生人你好」这种
                // 判据：左引号前面是中文/"，右引号后面是中文
                out = out.replaceAll("\"(?=[\\u4e00-\\u9fff])", "\u300c");
                // 上面会误伤字符串开头的 "中文…  —— 需要回退：
                // 如果 " 前面是 ( , = 空格 等语法位置，说明是字符串边界，还原
                out = out.replaceAll("([(,=+\\s:\\[])\\u300c", "$1\"");

                if (!out.equals(ln)) {
                    // 只有引号总数不变才接受（保证没破坏字符串配对）
                    if (countQuotes(out) == countQuotes(ln)) {
                        System.out.println((apply ? "FIX  " : "DRY  ") + f.getName() + ":" + (i + 1)
                                + "\n     - " + t + "\n     + " + out.trim());
                        lines[i] = out;
                        dirty = true;
                        nLines++;
                    }
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
                + nFiles + " files, " + nLines + " lines");
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
