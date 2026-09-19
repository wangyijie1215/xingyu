package com.echoflow.chat;

/**
 * 计算器求值：一个极小的表达式解析器（递归下降）。
 *
 * 为什么不用 ScriptEngine：Android 上没有 Nashorn，
 * 而为了一个计算器引入表达式库不值得。这里 150 行搞定四则运算 + 括号 + 小数。
 *
 * 支持的运算符（与键盘上的符号一致）：
 *   +  -  ×  ÷  %  以及括号和正负号
 */
public class CalcEngine {

    private final String src;
    private int pos;

    private CalcEngine(String src) {
        this.src = src;
    }

    /** 求值；出错时返回 "错误" */
    public static String eval(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return "0";
        }
        try {
            CalcEngine p = new CalcEngine(expr);
            double v = p.parseExpr();
            if (p.pos < p.src.length()) {
                return "错误";
            }
            return format(v);
        } catch (Exception e) {
            return "错误";
        }
    }

    /** 去掉浮点误差产生的 .000000001 之类 */
    private static String format(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "错误";
        }
        if (v == Math.floor(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        String s = String.format(java.util.Locale.US, "%.10f", v);
        // 去掉尾部多余的 0
        s = s.replaceAll("0+$", "");
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private double parseExpr() {
        double left = parseTerm();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '+' || c == '−' || c == '-') {
                pos++;
                double right = parseTerm();
                left = (c == '+') ? left + right : left - right;
            } else {
                break;
            }
        }
        return left;
    }

    private double parseTerm() {
        double left = parseFactor();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '×' || c == '*' || c == '÷' || c == '/' || c == '%') {
                pos++;
                double right = parseFactor();
                if (c == '×' || c == '*') {
                    left = left * right;
                } else if (c == '%') {
                    left = left % right;
                } else {
                    left = right == 0 ? Double.NaN : left / right;
                }
            } else {
                break;
            }
        }
        return left;
    }

    private double parseFactor() {
        skipSpaces();
        if (pos < src.length() && (src.charAt(pos) == '-' || src.charAt(pos) == '−')) {
            pos++;
            return -parseFactor();
        }
        if (pos < src.length() && src.charAt(pos) == '+') {
            pos++;
            return parseFactor();
        }
        if (pos < src.length() && src.charAt(pos) == '(') {
            pos++;
            double v = parseExpr();
            if (pos < src.length() && src.charAt(pos) == ')') {
                pos++;
            }
            return v;
        }
        return parseNumber();
    }

    private double parseNumber() {
        skipSpaces();
        int start = pos;
        boolean dot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' && !dot) {
                dot = true;
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw new RuntimeException("no number");
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void skipSpaces() {
        while (pos < src.length() && src.charAt(pos) == ' ') {
            pos++;
        }
    }
}
