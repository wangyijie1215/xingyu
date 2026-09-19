package com.echoflow.chat;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 开屏。
 *
 * 动画编排（总长约 1.5 秒，不拖沓）：
 *   0ms     背景淡入
 *   0ms     光晕从 0.6 放大到 1.0，同步淡入
 *   120ms   图标从 0.7 弹性放大到 1.0（OvershootInterpolator 给一点回弹）
 *   420ms   名字上浮 + 淡入
 *   600ms   标语上浮 + 淡入
 *   1050ms  底部品牌行淡入
 *   1500ms  整体淡出 → 进主页
 *
 * 为什么不用系统 SplashScreen API：那个在 Android 12+ 表现好，
 * 但低版本行为不一致，而且没法做这种分段的元素动画。
 */
public class SplashActivity extends AppCompatActivity {

    private static final long TOTAL = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View glow = findViewById(R.id.splash_glow);
        View icon = findViewById(R.id.splash_icon);
        View name = findViewById(R.id.splash_name);
        View slogan = findViewById(R.id.splash_slogan);
        View footer = findViewById(R.id.splash_footer);
        View root = findViewById(android.R.id.content);

        // 初始状态
        glow.setAlpha(0f);
        glow.setScaleX(0.6f);
        glow.setScaleY(0.6f);

        icon.setAlpha(0f);
        icon.setScaleX(0.7f);
        icon.setScaleY(0.7f);

        name.setAlpha(0f);
        slogan.setAlpha(0f);
        footer.setAlpha(0f);

        // 光晕：放大 + 淡入
        glow.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(900)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 图标：弹性放大 + 淡入（延迟 120ms）
        icon.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(120)
                .setDuration(620)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();

        // 名字：上浮 + 淡入
        slideIn(name, 0, 26, 420);

        // 标语：上浮 + 淡入
        slideIn(slogan, 0, 18, 600);

        // 底部：淡入
        footer.animate().alpha(1f).setStartDelay(1050).setDuration(400).start();

        // 光晕持续缓慢呼吸（让画面不死）
        ValueAnimator breathe = ValueAnimator.ofFloat(1f, 1.12f);
        breathe.setDuration(1800);
        breathe.setRepeatCount(ValueAnimator.INFINITE);
        breathe.setRepeatMode(ValueAnimator.REVERSE);
        breathe.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            glow.setScaleX(v);
            glow.setScaleY(v);
        });
        breathe.start();

        // 收尾：整体淡出 → 主页
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) {
                return;
            }
            root.animate().alpha(0f).setDuration(280).withEndAction(() -> {
                if (isFinishing()) {
                    return;
                }
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }).start();
        }, TOTAL);
    }

    private void slideIn(View v, float dx, float dy, long delay) {
        v.setTranslationY(dy);
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(460)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    @Override
    public void onBackPressed() {
        // 开屏期间按返回不做事，避免退出后进不去
    }
}
