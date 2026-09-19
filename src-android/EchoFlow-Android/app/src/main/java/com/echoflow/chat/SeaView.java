package com.echoflow.chat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 漂流瓶的海面背景 —— 自绘。
 *
 * 为什么自绘：这是整个应用里唯一一个「氛围型"界面，
 * 用静态图片撑不起来（也没有合适的素材）。自绘能做：
 *
 *   1. **多层波浪**，不同速度、不同透明度 → 产生纵深
 *   2. **月光在水面的碎光**，随机闪烁 → 让画面不死
 *   3. **渐变夜空** + 地平线光晕 → 安静但不空
 *
 * 性能：一个 ValueAnimator 驱动相位，onDraw 只做路径绘制，
 * 不需要重绘任何位图。
 */
public class SeaView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wavePath = new Path();

    private float phase = 0f;
    private ValueAnimator anim;

    /** 碎光的随机位置（固定下来，只让透明度变化） */
    private float[] sparkX;
    private float[] sparkY;
    private float[] sparkSeed;

    public SeaView(Context context) {
        super(context);
        init();
    }

    private void init() {
        int n = 26;
        sparkX = new float[n];
        sparkY = new float[n];
        sparkSeed = new float[n];
        for (int i = 0; i < n; i++) {
            sparkX[i] = (float) Math.random();
            sparkY[i] = 0.42f + (float) Math.random() * 0.52f;
            sparkSeed[i] = (float) (Math.random() * Math.PI * 2);
        }
    }

    /** 开始/停止波浪动画 */
    public void start() {
        if (anim != null && anim.isRunning()) {
            return;
        }
        anim = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        anim.setDuration(6000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void stop() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float horizon = h * 0.44f;

        // ---------- 夜空 ----------
        LinearGradient sky = new LinearGradient(0, 0, 0, horizon,
                new int[]{0xFF0A0716, 0xFF171030, 0xFF2A1E4A},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(sky);
        c.drawRect(0, 0, w, horizon, paint);
        paint.setShader(null);

        // ---------- 星星 ----------
        for (int i = 0; i < sparkX.length; i++) {
            float x = sparkX[i] * w;
            float y = sparkY[i] * horizon * 0.86f;
            float tw = (float) (0.5 + 0.5 * Math.sin(phase * 1.6 + sparkSeed[i]));
            paint.setColor(Color.argb((int) (30 + 120 * tw), 255, 250, 235));
            c.drawCircle(x, y, 1.2f + 1.4f * tw, paint);
        }

        // ---------- 月亮 ----------
        float moonX = w * 0.74f;
        float moonY = horizon * 0.30f;
        float moonR = h * 0.030f;

        // 外层光晕（大而淡）
        paint.setShader(new RadialGradient(moonX, moonY, h * 0.26f,
                new int[]{0x33FFF6DC, 0x14FFF6DC, 0x00FFF6DC},
                new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(moonX, moonY, h * 0.26f, paint);
        // 中层晕
        paint.setShader(new RadialGradient(moonX, moonY, moonR * 3.2f,
                new int[]{0x66FFF6DC, 0x22FFF6DC, 0x00FFF6DC},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(moonX, moonY, moonR * 3.2f, paint);
        paint.setShader(null);
        // 月盘本身
        paint.setColor(0xFFF8F2E0);
        c.drawCircle(moonX, moonY, moonR, paint);
        // 月面暗部：让它看起来是个球，而不是一个圆片
        paint.setColor(0x14B8A88A);
        c.drawCircle(moonX + moonR * 0.28f, moonY + moonR * 0.22f, moonR * 0.72f, paint);
        c.drawCircle(moonX - moonR * 0.30f, moonY - moonR * 0.16f, moonR * 0.34f, paint);

        // ---------- 海面底色 ----------
        LinearGradient sea = new LinearGradient(0, horizon, 0, h,
                new int[]{0xFF1B2540, 0xFF121A2E, 0xFF0A0F1D},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(sea);
        c.drawRect(0, horizon, w, h, paint);
        paint.setShader(null);

        // ---------- 月光在水面的倒影带 ----------
        for (int i = 0; i < 22; i++) {
            float t = i / 22f;
            float y = horizon + t * (h - horizon);
            float wob = (float) Math.sin(phase * 1.2 + t * 5f) * w * 0.035f * (0.3f + t);
            float bandW = w * (0.05f + t * 0.16f);
            int alpha = (int) (46 * (1f - t * 0.7f));
            paint.setColor(Color.argb(alpha, 255, 246, 220));
            c.drawRect(moonX - bandW / 2f + wob, y, moonX + bandW / 2f + wob, y + 2.5f, paint);
        }

        // ---------- 三层波浪 ----------
        drawWave(c, w, h, horizon, 0.052f, 0.9f, 26, 0x22A8C4E8, 1.0f);
        drawWave(c, w, h, horizon, 0.072f, 1.35f, 20, 0x33759CC8, 1.6f);
        drawWave(c, w, h, horizon, 0.098f, 1.8f, 14, 0x44C8D8F0, 2.3f);
    }

    /**
     * 画一层波浪。
     *
     * @param amp   振幅（占高度的比例）
     * @param freq  频率（一个屏幕里几个波）
     * @param speed 相位速度倍率
     */
    private void drawWave(Canvas c, int w, int h, float horizon,
                          float amp, float freq, int segments,
                          int color, float speed) {
        wavePath.reset();
        wavePath.moveTo(0, h);
        float baseY = horizon + h * (0.02f + amp * 0.4f);
        for (int i = 0; i <= segments; i++) {
            float x = w * i / segments;
            float t = (float) i / segments;
            float y = baseY
                    + (float) Math.sin(phase * speed + t * freq * Math.PI * 2) * h * amp
                    + (float) Math.sin(phase * speed * 0.6f + t * freq * 3.1f) * h * amp * 0.35f;
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(w, h);
        wavePath.close();

        paint.setColor(color);
        c.drawPath(wavePath, paint);
    }
}