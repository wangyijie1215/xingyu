// 生成星语应用图标各密度 PNG。
// 用 C# 而不是 PowerShell：PS 对 GDI+ 的重载解析会挑错。
// 语言级别压到 C# 4（.NET 4.0 的 csc），所以不用元组/内插字符串。
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

public static class IconGen
{
    const string Res = @"C:\Users\30828\.qclaw\dsh-next\_echoflow\src-android\EchoFlow-Android\app\src\main\res";

    static PointF P(double x, double y, double s)
    {
        return new PointF((float)(x * s), (float)(y * s));
    }

    public static Bitmap Render(int size)
    {
        Bitmap bmp = new Bitmap(size, size);
        Graphics g = Graphics.FromImage(bmp);
        try
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            double s = size / 108.0;

            // 背景：对角渐变（左上亮紫 -> 右下深紫）
            Rectangle rect = new Rectangle(0, 0, size, size);
            LinearGradientBrush bg = new LinearGradientBrush(
                new PointF(0, 0),
                new PointF(size, size),
                Color.FromArgb(255, 58, 37, 96),
                Color.FromArgb(255, 21, 14, 40));
            try
            {
                g.FillRectangle(bg, rect);
            }
            finally
            {
                bg.Dispose();
            }

            // 后气泡（左下，淡紫）
            SolidBrush b1 = new SolidBrush(Color.FromArgb(255, 199, 168, 255));
            try
            {
                g.FillEllipse(b1, (float)(24 * s), (float)(34 * s), (float)(28 * s), (float)(25 * s));
                g.FillPolygon(b1, new PointF[] {
                    P(33.4, 58.1, s), P(27.6, 61.2, s), P(28.9, 55.6, s)
                });
            }
            finally
            {
                b1.Dispose();
            }

            // 前气泡（右上，亮白）
            SolidBrush b2 = new SolidBrush(Color.FromArgb(255, 244, 242, 255));
            try
            {
                g.FillEllipse(b2, (float)(44 * s), (float)(26 * s), (float)(38 * s), (float)(35 * s));
                g.FillPolygon(b2, new PointF[] {
                    P(57.4, 60.0, s), P(49.1, 64.4, s), P(50.9, 56.4, s)
                });
            }
            finally
            {
                b2.Dispose();
            }

            // 四角星（右上角，暖橙）
            SolidBrush b3 = new SolidBrush(Color.FromArgb(255, 255, 166, 87));
            try
            {
                g.FillPolygon(b3, new PointF[] {
                    P(78.0, 20.0, s), P(80.6, 28.4, s), P(87.0, 29.0, s), P(80.6, 31.6, s),
                    P(78.0, 38.0, s), P(75.4, 31.6, s), P(69.0, 29.0, s), P(75.4, 28.4, s)
                });
            }
            finally
            {
                b3.Dispose();
            }
        }
        finally
        {
            g.Dispose();
        }
        return bmp;
    }

    public static void Save(Bitmap b, string path)
    {
        string dir = Path.GetDirectoryName(path);
        if (!Directory.Exists(dir))
        {
            Directory.CreateDirectory(dir);
        }
        b.Save(path, ImageFormat.Png);
    }

    public static void Run()
    {
        string[] names = { "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi" };
        int[] sizes = { 48, 72, 96, 144, 192 };
        for (int i = 0; i < names.Length; i++)
        {
            Bitmap b = Render(sizes[i]);
            try
            {
                Save(b, Path.Combine(Path.Combine(Res, names[i]), "ic_launcher.png"));
                Console.WriteLine("  " + names[i] + " -> " + sizes[i] + "px");
            }
            finally
            {
                b.Dispose();
            }
        }

        Bitmap app = Render(192);
        try
        {
            Save(app, Path.Combine(Path.Combine(Res, "drawable"), "app_icon.png"));
        }
        finally
        {
            app.Dispose();
        }

        Bitmap large = Render(512);
        try
        {
            Save(large, Path.Combine(Path.Combine(Res, "drawable"), "ic_launcher_large.png"));
        }
        finally
        {
            large.Dispose();
        }

        Console.WriteLine("  app_icon.png / ic_launcher_large.png updated");
    }

    public static void Main()
    {
        Run();
    }
}
