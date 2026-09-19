// CalcEngine 的离线测试：把同样的算法在桌面跑一遍，验证四则运算与优先级。
// 之所以单独测：模拟器上计算器的显示是 TextView，adb 读不稳；
// 而真正需要保证正确性的是求值逻辑本身。
using System;

public static class CalcTest
{
    static int pos;
    static string src;

    static double ParseExpr()
    {
        double left = ParseTerm();
        while (pos < src.Length)
        {
            char c = src[pos];
            if (c == '+' || c == '-' || c == '\u2212')
            {
                pos++;
                double r = ParseTerm();
                left = (c == '+') ? left + r : left - r;
            }
            else break;
        }
        return left;
    }

    static double ParseTerm()
    {
        double left = ParseFactor();
        while (pos < src.Length)
        {
            char c = src[pos];
            if (c == '\u00d7' || c == '*' || c == '\u00f7' || c == '/' || c == '%')
            {
                pos++;
                double r = ParseFactor();
                if (c == '\u00d7' || c == '*') left = left * r;
                else if (c == '%') left = left % r;
                else left = (r == 0) ? double.NaN : left / r;
            }
            else break;
        }
        return left;
    }

    static double ParseFactor()
    {
        while (pos < src.Length && src[pos] == ' ') pos++;
        if (pos < src.Length && (src[pos] == '-' || src[pos] == '\u2212')) { pos++; return -ParseFactor(); }
        if (pos < src.Length && src[pos] == '+') { pos++; return ParseFactor(); }
        if (pos < src.Length && src[pos] == '(')
        {
            pos++;
            double v = ParseExpr();
            if (pos < src.Length && src[pos] == ')') pos++;
            return v;
        }
        return ParseNumber();
    }

    static double ParseNumber()
    {
        while (pos < src.Length && src[pos] == ' ') pos++;
        int start = pos;
        bool dot = false;
        while (pos < src.Length)
        {
            char c = src[pos];
            if (c >= '0' && c <= '9') pos++;
            else if (c == '.' && !dot) { dot = true; pos++; }
            else break;
        }
        if (start == pos) throw new Exception("no number");
        return double.Parse(src.Substring(start, pos - start),
            System.Globalization.CultureInfo.InvariantCulture);
    }

    public static string Eval(string e)
    {
        if (e == null || e.Trim().Length == 0) return "0";
        try
        {
            src = e; pos = 0;
            double v = ParseExpr();
            if (pos < src.Length) return "ERR";
            if (v == Math.Floor(v) && Math.Abs(v) < 1e15)
                return ((long)v).ToString();
            return v.ToString("0.##########", System.Globalization.CultureInfo.InvariantCulture);
        }
        catch { return "ERR"; }
    }

    public static void Main()
    {
        // 输入 → 期望
        string[,] cases = {
            { "12\u00d78", "96" },
            { "7+8", "15" },
            { "100\u00f72", "50" },
            { "9-15", "-6" },
            { "2+3\u00d74", "14" },          // 优先级：先乘后加
            { "(2+3)\u00d74", "20" },        // 括号
            { "10\u00f73", "3.3333333333" }, // 小数
            { "5%3", "2" },
            { "1.5+2.25", "3.75" },
            { "-8+3", "-5" },
        };
        int pass = 0, fail = 0;
        for (int i = 0; i < cases.GetLength(0); i++)
        {
            string got = Eval(cases[i, 0]);
            bool ok = got == cases[i, 1];
            if (ok) pass++; else fail++;
            Console.WriteLine((ok ? "  PASS  " : "  FAIL  ") + cases[i, 0]
                + " = " + got + (ok ? "" : "   (expected " + cases[i, 1] + ")"));
        }
        Console.WriteLine();
        Console.WriteLine("passed " + pass + ", failed " + fail);
    }
}
