package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 模拟手机主屏。
 *
 * 让手机「像真的"的三个层次：
 *  1. **系统层**：状态栏是真实时间与真实电量（PhoneSystem），不是写死的
 *  2. **桌面层**：时钟小组件 + 应用图标网格 + Dock + 页面指示点
 *  3. **内容层**：每个图标都通向真实功能（微信 / 朋友圈 / 神社 / 设置 / 日程 / 音乐…）
 *
 * 这比「换个背景就是手机"要实在得多——用户能一路点下去。
 */
public class PhoneHomeActivity extends AppCompatActivity {

    private LinearLayout grid;
    private PhoneStatusBar statusBar;
    private TextView clock;
    private TextView date;
    private java.util.Timer clockTimer;

    /** 桌面应用：名称 + 图标 + 目标 */
    private static class AppEntry {
        final String name;
        final int icon;
        final View.OnClickListener onClick;

        AppEntry(String name, int icon, View.OnClickListener onClick) {
            this.name = name;
            this.icon = icon;
            this.onClick = onClick;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_home);

        // 状态栏：真实时间 / 电量
        statusBar = new PhoneStatusBar(this, findViewById(android.R.id.content));
        statusBar.bind();

        // 时钟小组件
        clock = findViewById(R.id.home_clock);
        date = findViewById(R.id.home_date);
        updateClock();
        clockTimer = new java.util.Timer("phone-clock", true);
        clockTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // 在 Activity 实例上 post，避免方法引用在匿名内部类里解析失败
                runOnUiThread(() -> updateClock());
            }
        }, 1000, 1000);

        // Dock
        findViewById(R.id.dock_chat).setOnClickListener(v -> openPhoneList());
        findViewById(R.id.dock_moments).setOnClickListener(v -> openMoments());
        findViewById(R.id.dock_shrine).setOnClickListener(v -> openShrine());
        findViewById(R.id.dock_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // 桌面网格
        grid = findViewById(R.id.home_grid);
        buildGrid();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusBar != null) {
            statusBar.refresh();
        }
        updateClock();
        if (grid != null) {
            buildGrid();   // 回到桌面时刷新角标
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusBar != null) {
            statusBar.stopTicking();
        }
        if (clockTimer != null) {
            clockTimer.cancel();
        }
    }

    // ==================================================================

    private void updateClock() {
        if (clock == null) {
            return;
        }
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        if (date != null) {
            date.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));
        }
    }

    private void openPhoneList() {
        startActivity(new Intent(this, PhoneListActivity.class));
    }

    private void openMoments() {
        Intent i = new Intent(this, PhoneMomentsActivity.class);
        startActivity(i);
    }

    /** 打开「小应用"集合里的其中一个 */
    private void openApp(String app) {
        Intent i = new Intent(this, PhoneAppActivity.class);
        i.putExtra(PhoneAppActivity.EXTRA_APP, app);
        startActivity(i);
    }

    private void openShrine() {
        // 神社需要一个角色上下文，取最近活跃的那个
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards.isEmpty()) {
            startActivity(new Intent(this, CardListActivity.class));
            return;
        }
        Intent i = new Intent(this, ShrineActivity.class);
        i.putExtra("card_id", cards.get(0).id);
        startActivity(i);
    }

    /** 桌面网格：每行 4 个 */
    private void buildGrid() {
        grid.removeAllViews();
        List<AppEntry> apps = new ArrayList<>();

        apps.add(new AppEntry("微信", R.drawable.icon_phone_chat,
                v -> openPhoneList()));
        apps.add(new AppEntry("朋友圈", R.drawable.icon_phone_moments,
                v -> openMoments()));
        apps.add(new AppEntry("角色库", R.drawable.icon_phone_contacts,
                v -> startActivity(new Intent(this, CardListActivity.class))));
        apps.add(new AppEntry("神社", R.drawable.icon_phone_shrine,
                v -> openShrine()));

        apps.add(new AppEntry("日程", R.drawable.icon_phone_schedule,
                v -> startActivity(new Intent(this, PhoneScheduleActivity.class))));
        apps.add(new AppEntry("电话", R.drawable.icon_phone_dialer,
                v -> startActivity(new Intent(this, PhoneDialerActivity.class))));
        apps.add(new AppEntry("相册", R.drawable.icon_phone_gallery,
                v -> openApp("gallery")));
        apps.add(new AppEntry("音乐", R.drawable.icon_phone_music,
                v -> openApp("music")));

        apps.add(new AppEntry("天气", R.drawable.icon_phone_weather,
                v -> openApp("weather")));
        apps.add(new AppEntry("备忘", R.drawable.icon_phone_notes,
                v -> openApp("notes")));
        apps.add(new AppEntry("计算器", R.drawable.icon_phone_calc,
                v -> openApp("calc")));
        apps.add(new AppEntry("文档", R.drawable.icon_phone_doc,
                v -> startActivity(new Intent(this, DocActivity.class))));
        apps.add(new AppEntry("漂流瓶", R.drawable.icon_phone_bottle,
                v -> startActivity(new Intent(this, BottleActivity.class))));
        apps.add(new AppEntry("助手", R.drawable.icon_phone_assistant,
                v -> startActivity(new Intent(this, GeneralAssistantActivity.class))));
        apps.add(new AppEntry("桌宠", R.drawable.icon_phone_pet,
                v -> startActivity(new Intent(this, PetSettingsActivity.class))));
        apps.add(new AppEntry("语音", R.drawable.icon_phone_voice,
                v -> startActivity(new Intent(this, TtsSettingsActivity.class))));
        apps.add(new AppEntry("世界书", R.drawable.icon_phone_book,
                v -> {
                    Intent i = new Intent(this, LorebookActivity.class);
                    List<CharacterCard> cs = PhoneStore.recentCards(this);
                    if (!cs.isEmpty()) {
                        i.putExtra("card_id", cs.get(0).id);
                    }
                    startActivity(i);
                }));
        apps.add(new AppEntry("生图", R.drawable.icon_phone_art,
                v -> {
                    Intent i = new Intent(this, ImageGenActivity.class);
                    List<CharacterCard> cs = PhoneStore.recentCards(this);
                    if (!cs.isEmpty()) {
                        i.putExtra("card_id", cs.get(0).id);
                    }
                    startActivity(i);
                }));
        apps.add(new AppEntry("设置", R.drawable.icon_phone_settings,
                v -> startActivity(new Intent(this, SettingsActivity.class))));

        apps.add(new AppEntry("关于", R.drawable.app_icon,
                v -> showAbout()));

        LinearLayout row = null;
        for (int i = 0; i < apps.size(); i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = dp(20);
                row.setLayoutParams(rlp);
                grid.addView(row);
            }
            row.addView(appIcon(apps.get(i)));
        }
    }

    /** 单个桌面图标：rounded 图标底 + 名称；微信/朋友圈带角标 */
    private View appIcon(AppEntry app) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        item.setClickable(true);
        item.setFocusable(true);
        item.setPadding(0, dp(4), 0, dp(4));

        FrameLayout iconWrap = new FrameLayout(this);
        int iconSize = dp(56);
        iconWrap.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setImageResource(app.icon);
        iconWrap.addView(iv);

        // 角标：微信显示未读会话数，朋友圈显示新动态数
        int badge = badgeFor(app.name);
        if (badge > 0) {
            TextView b = new TextView(this);
            int bs = dp(18);
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, bs);
            blp.gravity = Gravity.TOP | Gravity.END;
            b.setLayoutParams(blp);
            b.setMinWidth(bs);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(5), 0, dp(5), 0);
            b.setBackground(EfUi.roundRectPx(0xFFFA5151, 0, dp(9)));
            b.setText(String.valueOf(badge > 99 ? "99+" : badge));
            b.setTextColor(0xFFFFFFFF);
            b.setTextSize(10);
            b.setTypeface(null, Typeface.BOLD);
            iconWrap.addView(b);
        }
        item.addView(iconWrap);

        TextView name = new TextView(this);
        name.setText(app.name);
        name.setTextSize(11);
        name.setTextColor(0xFFFFFFFF);
        name.setShadowLayer(4f, 0f, 1f, 0x99000000);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(5);
        name.setLayoutParams(nlp);
        item.addView(name);

        item.setOnClickListener(app.onClick);
        return item;
    }

    /** 角标数量 */
    private int badgeFor(String appName) {
        if ("朋友圈".equals(appName)) {
            return PostStore.listPosts(this).size();
        }
        if ("微信".equals(appName)) {
            // 真实未读：她发了话而你没读的条数。
            // 原先统计的是「有聊天记录的角色数"，所以聊过一次就永远挂着角标，点进去也不消。
            return ReadCursor.totalUnread(this);
        }
        return 0;
    }

    private void showAbout() {
        String msg = "星语 EchoFlow\n"
                + getString(R.string.app_slogan) + "\n\n"
                + "内置角色：" + CardStore.listCards(this).size() + " 个\n"
                + "朋友圈动态：" + PostStore.listPosts(this).size() + " 条\n"
                + "模型来源：" + (ProviderStore.isOllama(this) ? "本地 Ollama" : "云端 API") + "\n"
                + "当前模型：" + ProviderStore.effectiveModel(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage(msg)
                .setPositiveButton("好", null)
                .show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}