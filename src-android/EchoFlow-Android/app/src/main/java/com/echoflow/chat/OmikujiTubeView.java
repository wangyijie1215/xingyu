package com.echoflow.chat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * 签筒自绘 View（神社 · 摇签页）。
 *
 * 为什么自绘：签筒是圆柱体 + 金色筒口 + 七根错落的签条 + 摇动时的整体位移旋转，
 * 用 xml 形状拼不出来。自绘也让摇动动画的每一帧都完全可控。
 *
 * 摇动时：整体 translateX 在 ±11dp 摆动并叠加 ±3.5° 旋转，
 * 背后一层金色光晕同步淡入，松手后落签。
 */
public class OmikujiTubeView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    /** 摇动相位 0~1，驱动位移与旋转 */
    private float phase = 0f;
    /** 光晕强度 0~1 */
    private float glow = 0f;
    /** 是否正在摇 */
    private boolean shaking = false;

    private ValueAnimator anim;

    public OmikujiTubeView(Context context) {
        super(context);
        init();
    }

    public OmikujiTubeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OmikujiTubeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    /** 开始摇动；无限循环，直到 stopShake() */
    public void startShake() {
        if (shaking) {
            return;
        }
        shaking = true;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(460);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            glow = Math.min(1f, glow + 0.06f);
            invalidate();
        });
        anim.start();
    }

    public void stopShake() {
        shaking = false;
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
        phase = 0f;
        glow = 0f;
        invalidate();
    }

    public boolean isShaking() {
        return shaking;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float density = getResources().getDisplayMetrics().density;

        // 摇动时的整体位移与旋转
        float dx = 0f;
        float rot = 0f;
        if (shaking) {
            double angle = phase * Math.PI * 2;
            dx = (float) Math.sin(angle) * 11f * density;
            rot = (float) Math.sin(angle) * 3.5f;
        }

        // ---- 金色光晕（在签筒后面）----
        if (glow > 0f) {
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.max(w, h) * 0.78f;
            paint.setShader(new android.graphics.RadialGradient(
                    cx, cy, radius,
                    new int[]{withAlpha(0xFFE8C87A, (int) (86 * glow)), 0x00E8C87A},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
        }

        canvas.save();
        canvas.translate(dx, 0f);
        if (rot != 0f) {
            canvas.rotate(rot, w / 2f, h);
        }

        // ---- 签筒主体：朱红木纹渐变 ----
        float bodyLeft = w * 0.22f;
        float bodyRight = w * 0.78f;
        float bodyTop = h * 0.18f;
        float bodyBottom = h * 0.92f;
        float radius = 12f * density;

        rect.set(bodyLeft, bodyTop, bodyRight, bodyBottom);
        paint.setShader(new LinearGradient(
                bodyLeft, 0, bodyRight, 0,
                new int[]{0xFF8C3A2E, 0xFFC4553F, 0xFFE5804F, 0xFFA5412F, 0xFF7A2E24},
                new float[]{0f, 0.22f, 0.5f, 0.78f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        // 金色描边
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * density);
        paint.setColor(0x59E8C87A);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);

        // ---- 筒口：椭圆（透视）----
        rect.set(bodyLeft - 4f * density, bodyTop - 7f * density,
                bodyRight + 4f * density, bodyTop + 9f * density);
        paint.setShader(new LinearGradient(
                0, rect.top, 0, rect.bottom,
                new int[]{0xFF5E241C, 0xFF8C3A2E},
                null, Shader.TileMode.CLAMP));
        canvas.drawOval(rect, paint);
        paint.setShader(null);

        // ---- 七根签条，错落伸出筒口 ----
        float[] offsets = {-0.30f, -0.20f, -0.10f, 0f, 0.10f, 0.20f, 0.30f};
        float[] heights = {22f, 28f, 20f, 26f, 21f, 25f, 19f};
        float[] tilts = {-8f, -3f, 2f, 6f, -5f, 4f, -2f};
        float stickW = 7f * density;
        float cx = w / 2f;
        float baseY = bodyTop - 4f * density;

        for (int i = 0; i < offsets.length; i++) {
            float sx = cx + offsets[i] * (bodyRight - bodyLeft) * 0.92f;
            float sh = heights[i] * density;
            canvas.save();
            canvas.rotate(tilts[i], sx, baseY);
            rect.set(sx - stickW / 2f, baseY - sh, sx + stickW / 2f, baseY + 6f * density);
            paint.setShader(new LinearGradient(
                    0, rect.top, 0, rect.bottom,
                    new int[]{0xFFF4ECDC, 0xFFD9CBAC},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, stickW / 2f, stickW / 2f, paint);
            paint.setShader(null);
            canvas.restore();
        }

        // ---- 筒身「御籤"二字 ----
        paint.setColor(0xFFE8C87A);
        paint.setTextSize(30f * density);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(6f, 0, 2f, 0x99000000);
        float textY = bodyTop + (bodyBottom - bodyTop) * 0.62f;
        canvas.drawText("御", cx, textY, paint);
        canvas.drawText("籤", cx, textY + 32f * density, paint);
        paint.clearShadowLayer();
        paint.setFakeBoldText(false);

        canvas.restore();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}