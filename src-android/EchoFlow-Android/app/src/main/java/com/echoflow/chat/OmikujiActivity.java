package com.echoflow.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * 摇签页 —— 一次有仪式感的交互。
 *
 * 流程：默念愿望 → 点击摇一摇（签筒左右摆动 + 金色光晕淡入）
 *      → 1.4s 落下一根签条 → 再点「展开签文"跳转签文页。
 *
 * 关键：摇签只是「展开"的动作，签的结果在进入神社时就已经定下并落库了，
 * 因此这里不做任何随机，纯粹的呈现。
 */
public class OmikujiActivity extends AppCompatActivity {

    private CharacterCard card;
    private OmikujiTubeView tube;
    private TextView prayer;
    private TextView shakeBtn;
    private LinearLayout dropZone;
    private boolean shook = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        card = CardStore.getCard(this, getIntent().getStringExtra("card_id"));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.ef_shrine_night));
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF3A1E24, 0xFF20142C, 0xFF0E0A12}));
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);

        // 手水舍实拍作为底图，压暗后不抢签筒
        ImageView bgv = new ImageView(this);
        bgv.setImageResource(R.drawable.ef_shrine_water);
        bgv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgv.setAlpha(0.28f);
        page.addView(bgv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        scroll.addView(page);

        // 返回
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = EfUi.dp(this, 12);
        nlp.leftMargin = EfUi.dp(this, 12);
        nav.setLayoutParams(nlp);
        TextView back = EfUi.text(this, "‹", 24, 0xFFFFFFFF, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(EfUi.roundRectPx(0x85101018, 0x29FFFFFF, EfUi.dp(this, 999)));
        int bs = EfUi.dp(this, 36);
        back.setLayoutParams(new LinearLayout.LayoutParams(bs, bs));
        back.setOnClickListener(v -> finish());
        nav.addView(back);
        page.addView(nav);

        // 眉标
        TextView eyebrow = EfUi.text(this, "银 月 神 社 · 末 社", 11, 0xFFE8C87A, true);
        eyebrow.setLetterSpacing(0.24f);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = EfUi.dp(this, 24);
        eyebrow.setLayoutParams(elp);
        eyebrow.setGravity(Gravity.CENTER);
        page.addView(eyebrow);

        // 签筒（自绘）
        tube = new OmikujiTubeView(this);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                EfUi.dp(this, 132), EfUi.dp(this, 210));
        tlp.topMargin = EfUi.dp(this, 26);
        tube.setLayoutParams(tlp);
        page.addView(tube);

        // 落签区
        dropZone = new LinearLayout(this);
        dropZone.setOrientation(LinearLayout.HORIZONTAL);
        dropZone.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, EfUi.dp(this, 80));
        dlp.topMargin = EfUi.dp(this, 16);
        dropZone.setLayoutParams(dlp);
        page.addView(dropZone);

        // 祈愿文案
        prayer = EfUi.text(this, "在心中默念你的愿望，\n然后摇一摇这支签筒。", 13, 0xFFC3BEDB, false);
        prayer.setGravity(Gravity.CENTER);
        prayer.setLineSpacing(EfUi.dp(this, 7), 1f);
        page.addView(prayer);

        // 摇一摇按钮
        shakeBtn = EfUi.text(this, "摇 一 摇", 14, 0xFFFFFFFF, true);
        shakeBtn.setGravity(Gravity.CENTER);
        shakeBtn.setBackgroundResource(R.drawable.ef_bg_btn_shrine);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                EfUi.dp(this, 168), EfUi.dp(this, 46));
        blp.topMargin = EfUi.dp(this, 30);
        shakeBtn.setLayoutParams(blp);
        shakeBtn.setOnClickListener(v -> onShake());
        page.addView(shakeBtn);

        TextView hint = EfUi.text(this, "每 日 一 签 · 结 果 由 今 日 因 缘 而 定", 11, 0xFF8B84A8, false);
        hint.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = EfUi.dp(this, 14);
        hint.setLayoutParams(hlp);
        page.addView(hint);

        setContentView(scroll);
    }

    private void onShake() {
        if (shook) {
            // 已摇过 → 直接看签文
            Intent i = new Intent(this, FortuneActivity.class);
            i.putExtra("card_id", card == null ? "" : card.id);
            startActivity(i);
            return;
        }
        shook = true;
        shakeBtn.setText("摇 签 中 …");
        shakeBtn.setAlpha(0.7f);
        tube.startShake();
        prayer.setText("签 筒 在 手 里 发 出 细 碎 的 声 响 …");

        handler.postDelayed(() -> {
            tube.stopShake();
            dropStick();
            prayer.setText("一 支 签 落 了 出 来 。");
        }, 1400);

        handler.postDelayed(() -> {
            shakeBtn.setText("展 开 签 文");
            shakeBtn.setAlpha(1f);
        }, 2600);
    }

    /** 落签：一根签条从上方落下并轻微过冲回弹 */
    private void dropStick() {
        View stick = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                EfUi.dp(this, 9), EfUi.dp(this, 74));
        stick.setLayoutParams(lp);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFF4ECDC, 0xFFCBB98F});
        g.setCornerRadius(EfUi.dp(this, 5));
        stick.setBackground(g);
        stick.setAlpha(0f);
        dropZone.addView(stick);

        ObjectAnimator drop = ObjectAnimator.ofFloat(stick, "translationY",
                -EfUi.dp(this, 120), EfUi.dp(this, 6), -EfUi.dp(this, 4), 0f);
        drop.setDuration(1000);
        drop.addUpdateListener(a -> stick.setAlpha(Math.min(1f, stick.getAlpha() + 0.06f)));
        drop.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}