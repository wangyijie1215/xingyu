package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 最终修复：把「夹在两个中文字之间的引号」还原成全角引号。
 *
 * ── 诊断依据（这一步才是对的）──────────────────────
 * 坏行的确定特征不是"引号数为奇数"，而是：
 *     引号**两侧都是中文字**  →  这一定是字符串内容的引号，不该用半角
 * 例：
 *     "…自动存到"我的文档"里。"
 *                ^^^^^^^^^ 这两侧的引号夹着中文 → 应为「」
 *
 * ── 修复规则 ───────────────────────────────────
 * 在含此形态的行里，把这类引号成对换成「 」：
 *   · 第一个中文夹引号 → 「
 *   · 与之配对的第二个 → 」
 *
 * 成对处理是关键：单换一个会再次制造不平衡。
 *
 * 默认预演，--apply 落盘。
 */
public class Fix6 {

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
                String out = fixLine(ln);
                if (!out.equals(ln)) {
                    System.out.println((apply ? "FIX  " : "DRY  ") + f.getName() + ":" + (i + 1)
                            + "\n     - " + t + "\n     + " + out.trim());
                    lines[i] = out;
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
                + nFiles + " files, " + nLines + " lines");
    }

    /**
     * 逐行把「夹在中文中间的引号」成对替换成全角。
     * 用状态机而不是正则：正则在"连续多个引号"时容易配错对。
     */
    static String fixLine(String ln) {
        StringBuilder sb = new StringBuilder();
        boolean inPair = false;   // 是否处在"中文引号对"里
        for (int i = 0; i < ln.length(); i++) {
            char c = ln.charAt(i);
            if (c != '"') {
                sb.append(c);
                continue;
            }
            boolean leftCjk = i > 0 && isCjk(ln.charAt(i - 1));
            boolean rightCjk = i + 1 < ln.length() && isCjk(ln.charAt(i + 1));
            if (leftCjk && rightCjk) {
                // 夹在中文里：开合交替
                sb.append(inPair ? '\u300d' : '\u300c');
                inPair = !inPair;
            } else {
                sb.append('"');
            }
        }
        return sb.toString();
    }

    static boolean isCjk(char c) {
        return c >= 0x4e00 && c <= 0x9fff;
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
