package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * 桌宠的绘制：**角色的 Q 版形象** + 弹跳形变。
 *
 * 与之前那颗手绘球的区别：现在是真人设的 Q 版图，才有「她"的感觉。
 *
 * 三个细节让图片看起来是「活的"：
 *   1. **圆形裁切**（不是方图贴上去）—— 悬浮在桌面上必须没有方边
 *   2. **弹跳形变**：落地压扁、起跳拉长。位图做形变用 Matrix 缩放，
 *      为了保证不变形，缩放前先把图居中裁成正方形
 *   3. **底部的接触阴影**：接近地面时阴影变浓，飞起来时变淡 ——
 *      这是「她在桌面上"而不是「贴在屏幕上"的关键
 */
public class PetAvatarRenderer {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();

    private Bitmap source;        // 原始 Q 版图（正方形）
    private BitmapShader shader;
    private int shaderSize;

    /** 设置（或更换）角色图 */
    public void setBitmap(Bitmap bmp) {
        if (bmp == null) {
            source = null;
            shader = null;
            return;
        }
        // 居中裁成正方形 —— 否则缩放时会被拉变形
        int side = Math.min(bmp.getWidth(), bmp.getHeight());
        int left = (bmp.getWidth() - side) / 2;
        int top = (int) ((bmp.getHeight() - side) * 0.35f);   // 偏上，保住脸
        if (top + side > bmp.getHeight()) {
            top = bmp.getHeight() - side;
        }
        source = Bitmap.createBitmap(bmp, left, Math.max(0, top), side, side);
        shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shaderSize = 0;
    }

    public boolean hasBitmap() {
        return source != null;
    }

    public Bitmap source() {
        return source;
    }

    /**
     * 画到目标画布。
     *
     * @param canvas  目标画布（尺寸 = 悬浮窗大小）
     * @param squashY 纵向压缩（1.0 = 正常，0.78 = 落地压扁）
     * @param fallbackColor 没有图时的兜底颜色
     * @param grounded 是否贴地（影响阴影浓度）
     */
    public void draw(Canvas canvas, float squashY, int fallbackColor, float grounded) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // ---- 底部接触阴影 ----
        float shadowAlpha = 0.10f + 0.26f * clamp01(grounded);
        float shadowW = w * (0.56f + 0.16f * clamp01(grounded));
        float shadowH = h * 0.11f;
        shadowPaint.setShader(new RadialGradient(
                w / 2f, h * 0.94f, shadowW / 2f,
                new int[]{Color.argb((int) (shadowAlpha * 255), 0, 0, 0),
                        Color.argb(0, 0, 0, 0)},
                null, Shader.TileMode.CLAMP));
        canvas.drawOval(
                w / 2f - shadowW / 2f, h * 0.94f - shadowH / 2f,
                w / 2f + shadowW / 2f, h * 0.94f + shadowH / 2f,
                shadowPaint);

        if (source == null) {
            // 兜底：画一颗纯色圆（还没生成 Q 版图时）
            paint.setShader(null);
            paint.setColor(fallbackColor);
            float r = Math.min(w, h) * 0.34f;
            canvas.drawCircle(w / 2f, h * 0.46f, r, paint);
            return;
        }

        // ---- 形变：压扁时横向变宽，保持体积 ----
        float scaleY = squashY;
        float scaleX = 2f - squashY;   // squash=0.78 → scaleX=1.22

        float drawW = w * scaleX;
        float drawH = h * scaleY;
        float dx = (w - drawW) / 2f;
        // 压扁时底部对齐（脚不离地），否则中心对齐
        float dy = h - drawH - (h * 0.04f);

        // 用 shader + matrix 缩放，避免 createBitmap 每帧新建
        if (shader != null) {
            Matrix m = new Matrix();
            m.setScale(drawW / source.getWidth(), drawH / source.getHeight());
            m.postTranslate(dx, dy);
            shader.setLocalMatrix(m);
            paint.setShader(shader);
            paint.setAlpha(255);
            canvas.drawCircle(w / 2f, dy + drawH / 2f, Math.min(drawW, drawH) / 2f, paint);

            // 再画一次矩形保证圆外部分被裁掉（drawCircle 已经约束，这里保险）
            paint.setShader(null);
        }
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}