package com.echoflow.chat;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * 漂流瓶。
 *
 * 三种交互：
 *   · 捞一个 —— 从海里捡，有概率捡到角色写给你的
 *   · 写一个 —— 自己写一句扔进海里
 *   · 收藏 / 我扔的
 *
 * 背景是自绘的海面（SeaView），不依赖任何图片资源。
 */
public class BottleActivity extends AppCompatActivity {

    private SeaView sea;
    private TextView bottleView;
    private TextView hint;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0A0716);
        // 让整个根布局让出系统状态栏，否则系统的时间/电量会和自绘的状态栏重叠
        root.setFitsSystemWindows(true);

        sea = new SeaView(this);
        root.addView(sea, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout fore = new LinearLayout(this);
        fore.setOrientation(LinearLayout.VERTICAL);
        fore.setPadding(dp(20), dp(6), dp(20), dp(24));

        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, fore, false);
        sb.setBackgroundColor(0x00000000);
        fore.addView(sb);
        new PhoneStatusBar(this, sb).bind();

        fore.addView(head());

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bottleView = new TextView(this);
        bottleView.setTextSize(64);
        bottleView.setText("🍾");
        bottleView.setGravity(Gravity.CENTER);
        bottleView.setAlpha(0.92f);
        center.addView(bottleView);

        hint = new TextView(this);
        hint.setText("海面上漂着一些瓶子\n捞一个看看");
        hint.setTextSize(14);
        hint.setTextColor(0xFF9A93BB);
        hint.setGravity(Gravity.CENTER);
        hint.setLineSpacing(dp(6), 1f);
        hint.setPadding(0, dp(20), 0, 0);
        center.addView(hint);
        fore.addView(center);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(actionBtn("捞一个", 0xFF9B7BFF, v -> pickBottle()));
        actions.addView(actionBtn("写一个", 0x33FFFFFF, v -> writeBottle()));
        fore.addView(actions);

        root.addView(fore);
        setContentView(root);

        sea.start();

        ObjectAnimator bob = ObjectAnimator.ofFloat(bottleView, "translationY", -10f, 10f);
        bob.setDuration(2200);
        bob.setRepeatCount(ObjectAnimator.INFINITE);
        bob.setRepeatMode(ObjectAnimator.REVERSE);
        bob.start();
    }

    private View head() {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, dp(12), 0, 0);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(28);
        back.setTextColor(0xFFEDE8FF);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        head.addView(back);

        TextView title = new TextView(this);
        title.setText("漂流瓶");
        title.setTextSize(19);
        title.setTextColor(0xFFEDE8FF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(title);

        TextView collectBtn = new TextView(this);
        collectBtn.setText("收藏");
        collectBtn.setTextSize(14);
        collectBtn.setTextColor(0xFFB9A8FF);
        collectBtn.setPadding(dp(12), dp(8), dp(4), dp(8));
        collectBtn.setOnClickListener(v -> showCollected());
        head.addView(collectBtn);
        return head;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sea != null) {
            sea.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sea != null) {
            sea.stop();
        }
    }

    // ==================================================================

    private View actionBtn(String label, int bg, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(15);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setBackground(EfUi.roundRectPx(bg, 0x33FFFFFF, dp(24)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        lp.leftMargin = dp(6);
        lp.rightMargin = dp(6);
        t.setLayoutParams(lp);
        t.setOnClickListener(l);
        return t;
    }

    private void pickBottle() {
        if (busy) {
            return;
        }
        busy = true;
        bottleView.animate().rotation(360f).setDuration(600).withEndAction(() ->
                bottleView.setRotation(0f)).start();
        hint.setText("捞上来了…");

        ui.postDelayed(() -> {
            BottleStore.Item it = BottleStore.pick(this);
            if (it.fromCardId != null && !it.fromCardId.isEmpty()) {
                generateFromCharacter(it);
            } else {
                showBottle(it);
                busy = false;
            }
        }, 700);
    }

    private void generateFromCharacter(BottleStore.Item it) {
        CharacterCard card = CardStore.getCard(this, it.fromCardId);
        String apiKey = SecureStore.get(this);
        boolean local = ProviderStore.isOllama(this);
        if (card == null || (!local && (apiKey == null || apiKey.isEmpty()))) {
            showBottle(it);
            busy = false;
            return;
        }
        ApiClient.streamChat(
                ProviderStore.effectiveBaseUrl(this),
                apiKey == null ? "" : apiKey,
                ProviderStore.effectiveModel(this),
                BottleWriter.prompt(card),
                local, new ApiClient.Callback() {
                    private String buf = "";

                    @Override
                    public void onChunk(String fullText) {
                        buf = fullText;
                    }

                    @Override
                    public void onDone(boolean error, String errMsg) {
                        ui.post(() -> {
                            if (!error) {
                                String clean = BottleWriter.clean(buf);
                                if (!clean.isEmpty()) {
                                    it.text = clean;
                                }
                            }
                            showBottle(it);
                            busy = false;
                        });
                    }
                });
    }

    private void showBottle(BottleStore.Item it) {
        bottleView.setText("📜");
        hint.setText("");

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView tag = new TextView(this);
        tag.setText("· " + BottleText.tagName(it.tag) + " ·");
        tag.setTextSize(12);
        tag.setTextColor(0xFFB9A8FF);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(0, 0, 0, dp(14));
        box.addView(tag);

        TextView body = new TextView(this);
        body.setText(it.text);
        body.setTextSize(16);
        body.setLineSpacing(dp(9), 1f);
        body.setTextColor(0xFFEDE8FF);
        body.setGravity(Gravity.CENTER);
        box.addView(body);

        if (it.from != null && !it.from.isEmpty()) {
            TextView from = new TextView(this);
            from.setText("—— " + it.from + " 扔的");
            from.setTextSize(12);
            from.setTextColor(0xFF8B84A8);
            from.setGravity(Gravity.CENTER);
            from.setPadding(0, dp(18), 0, 0);
            box.addView(from);
        }
        sv.addView(box);

        new AlertDialog.Builder(this)
                .setView(sv)
                .setPositiveButton("收下", (d, w) -> {
                    BottleStore.collect(this, it);
                    Toast.makeText(this, "已放进收藏", Toast.LENGTH_SHORT).show();
                    resetStage();
                })
                .setNegativeButton("放回海里", (d, w) -> resetStage())
                .setNeutralButton("再捞一个", (d, w) -> {
                    resetStage();
                    ui.postDelayed(this::pickBottle, 300);
                })
                .show();
    }

    private void resetStage() {
        bottleView.setText("🍾");
        hint.setText("海面上漂着一些瓶子\n捞一个看看");
        busy = false;
    }

    private void writeBottle() {
        final EditText input = new EditText(this);
        input.setHint("写一句想对陌生人说的话…");
        input.setTextColor(0xFF1A1A1A);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(this)
                .setTitle("扔一个瓶子")
                .setMessage("它会漂到某个你不认识的人手里。")
                .setView(input)
                .setPositiveButton("扔出去", (d, w) -> {
                    String t = input.getText().toString().trim();
                    if (t.isEmpty()) {
                        return;
                    }
                    BottleStore.throwBottle(this, t, "blessing");
                    bottleView.animate().translationY(dp(400)).alpha(0f)
                            .setDuration(700).withEndAction(() -> {
                                bottleView.setTranslationY(0f);
                                bottleView.setAlpha(0.92f);
                                Toast.makeText(this, "扔出去了", Toast.LENGTH_SHORT).show();
                            }).start();
                })
                .setNegativeButton("算了", null)
                .show();
    }

    private void showCollected() {
        final List<BottleStore.Item> list = BottleStore.collected(this);
        if (list.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("收藏")
                    .setMessage("还没有收藏的瓶子。\n\n捞到喜欢的就点收下。")
                    .setPositiveButton("好", null)
                    .show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("收藏（" + list.size() + "）")
                .setItems(preview(list), (d, w) -> showBottleReadonly(list.get(w)))
                .setNeutralButton("我扔的", (d, w) -> showMine())
                .show();
    }

    private String[] preview(List<BottleStore.Item> list) {
        String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String t = list.get(i).text.replace("\n", " ");
            items[i] = t.length() > 26 ? t.substring(0, 26) + "…" : t;
        }
        return items;
    }

    private void showBottleReadonly(BottleStore.Item it) {
        String msg = it.text
                + (it.from == null || it.from.isEmpty() ? "" : "\n\n—— " + it.from + " 扔的");
        new AlertDialog.Builder(this)
                .setTitle(BottleText.tagName(it.tag))
                .setMessage(msg)
                .setPositiveButton("关", null)
                .setNeutralButton("删掉", (d, w) -> {
                    BottleStore.uncollect(this, it.text);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showMine() {
        final List<BottleStore.Item> list = BottleStore.mine(this);
        if (list.isEmpty()) {
            Toast.makeText(this, "你还没扔过瓶子", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("我扔的（" + list.size() + "）")
                .setItems(preview(list), (d, w) -> new AlertDialog.Builder(this)
                        .setTitle("我扔的瓶子")
                        .setMessage(list.get(w).text)
                        .setPositiveButton("关", null)
                        .show())
                .show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
