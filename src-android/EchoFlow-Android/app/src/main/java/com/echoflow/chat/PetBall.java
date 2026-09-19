package com.echoflow.chat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * 桌宠的 Q 版小球 —— 用代码画，不用位图。
 *
 * 为什么手绘而不是用角色头像：
 *   · 头像裁成圆的放大后有明显的方形边缘，悬浮在桌面上很违和
 *   · 手绘的球可以做「呼吸"「眨眼"「跳动压扁拉伸"这些形变
 *   · 体积为零（不用打包任何图片）
 *
 * 形变是关键：一个真正滚动/弹跳的球，落地瞬间会被压扁（squash），
 * 起跳拉长（stretch）。没有这个形变，看起来就是个贴图在平移。
 */
public class PetBall {

    /** 球的主体颜色（取角色的代表色） */
    public int baseColor = 0xFF9B7BFF;
    /** 高光颜色 */
    public int highlightColor = 0xFFD6C8FF;
    /** 表情：0=正常 1=闭眼 2=开心 3=困 */
    public int face = 0;

    private Bitmap cache;
    private int cacheSize;
    private int cacheColor;
    private int cacheFace;

    /**
     * 生成球的位置。
     *
     * @param size 直径（像素）
     * @param squashY 纵向压缩系数（1.0 = 不压，0.8 = 落地压扁到 80%）
     */
    public Bitmap render(int size, float squashY) {
        int key = (size << 8) | (int) (squashY * 100);
        // 形变是连续的，缓存意义不大；只在完全静止时复用
        synchronized (this) {
            if (cache != null && cacheSize == size && cacheColor == baseColor
                    && cacheFace == face && Math.abs(squashY - 1f) < 0.01f) {
                return cache;
            }
            cache = draw(size, squashY);
            cacheSize = size;
            cacheColor = baseColor;
            cacheFace = face;
            return cache;
        }
    }

    private Bitmap draw(int size, float squashY) {
        float h = size * squashY;
        int w = (int) (size * (2f - squashY));   // 压扁时横向变宽，保持体积感
        Bitmap bmp = Bitmap.createBitmap(w, (int) h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f;

        // 主体：径向渐变，光源在左上
        RadialGradient body = new RadialGradient(
                cx - r * 0.32f, cy - r * 0.36f, r * 1.35f,
                new int[]{highlightColor, baseColor, darken(baseColor, 0.55f)},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP);
        p.setShader(body);
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);

        // 高光点
        p.setColor(0x66FFFFFF);
        c.drawCircle(cx - r * 0.34f, cy - r * 0.40f, r * 0.16f, p);

        // 底部反光（让它有「球"的体积感）
        p.setColor(0x22FFFFFF);
        c.drawCircle(cx + r * 0.18f, cy + r * 0.42f, r * 0.22f, p);

        // 脸
        drawFace(c, cx, cy, r);

        return bmp;
    }

    private void drawFace(Canvas c, float cx, float cy, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF2A1A4A);
        float eyeR = Math.max(1.5f, r * 0.075f);
        float eyeY = cy - r * 0.04f;
        float eyeDx = r * 0.26f;

        switch (face) {
            case 1:   // 闭眼：两条弧
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.2f, r * 0.055f));
                p.setStrokeCap(Paint.Cap.ROUND);
                android.graphics.RectF e1 = new android.graphics.RectF(
                        cx - eyeDx - eyeR * 1.6f, eyeY - eyeR * 0.9f,
                        cx - eyeDx + eyeR * 1.6f, eyeY + eyeR * 1.6f);
                c.drawArc(e1, 200, 140, false, p);
                android.graphics.RectF e2 = new android.graphics.RectF(
                        cx + eyeDx - eyeR * 1.6f, eyeY - eyeR * 0.9f,
                        cx + eyeDx + eyeR * 1.6f, eyeY + eyeR * 1.6f);
                c.drawArc(e2, 200, 140, false, p);
                break;
            case 2:   // 开心：弯月眼 + 张嘴
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.2f, r * 0.06f));
                p.setStrokeCap(Paint.Cap.ROUND);
                android.graphics.RectF f1 = new android.graphics.RectF(
                        cx - eyeDx - eyeR * 1.8f, eyeY - eyeR * 1.2f,
                        cx - eyeDx + eyeR * 1.8f, eyeY + eyeR * 1.4f);
                c.drawArc(f1, 20, 140, false, p);
                android.graphics.RectF f2 = new android.graphics.RectF(
                        cx + eyeDx - eyeR * 1.8f, eyeY - eyeR * 1.2f,
                        cx + eyeDx + eyeR * 1.8f, eyeY + eyeR * 1.4f);
                c.drawArc(f2, 20, 140, false, p);
                // 嘴
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xCC2A1A4A);
                android.graphics.RectF m = new android.graphics.RectF(
                        cx - r * 0.14f, cy + r * 0.18f, cx + r * 0.14f, cy + r * 0.42f);
                c.drawArc(m, 0, 180, true, p);
                break;
            case 3:   // 困：一条横线眼
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.2f, r * 0.05f));
                p.setStrokeCap(Paint.Cap.ROUND);
                c.drawLine(cx - eyeDx - eyeR, eyeY, cx - eyeDx + eyeR, eyeY, p);
                c.drawLine(cx + eyeDx - eyeR, eyeY, cx + eyeDx + eyeR, eyeY, p);
                break;
            default:  // 正常：两个圆点 + 轻微微笑
                c.drawCircle(cx - eyeDx, eyeY, eyeR, p);
                c.drawCircle(cx + eyeDx, eyeY, eyeR, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, r * 0.045f));
                p.setStrokeCap(Paint.Cap.ROUND);
                android.graphics.RectF s = new android.graphics.RectF(
                        cx - r * 0.16f, cy + r * 0.06f, cx + r * 0.16f, cy + r * 0.30f);
                c.drawArc(s, 20, 140, false, p);
                break;
        }
    }

    private int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(a, Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /** 按角色 id 取代表色，让每个角色的球不一样 */
    public static int colorFor(String cardId) {
        if (cardId == null) {
            return 0xFF9B7BFF;
        }
        switch (cardId) {
            case "builtin_alice":  return 0xFFA8B4FF;
            case "builtin_rin":    return 0xFFFF7B7B;
            case "builtin_hakuyo": return 0xFF5B7FD8;
            case "builtin_rocco":  return 0xFF7FE0C4;
            case "builtin_elian":  return 0xFFE0A87F;
            case "builtin_yueling": return 0xFFB48BFF;
            default: return 0xFF9B7BFF;
        }
    }
}