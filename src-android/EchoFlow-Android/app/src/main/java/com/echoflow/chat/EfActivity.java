package com.echoflow.chat;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 所有页面的基类。
 *
 * 抽这一层的直接原因：`dp(int)` 这个方法在 13 个文件里各写了一遍，
 * 状态栏注入代码手写了 9 遍，`getSharedPreferences` 出现 18 次。
 * 结果是改一个通用行为要动十几个文件 —— 上次换设计色板时，
 * 我只能靠全项目 grep 去找散落的硬编码。
 *
 * 这个基类只做三件最重复的事：
 *   1. dp() —— 尺寸换算
 *   2. 状态栏 —— 手机模式那套自绘状态栏，统一在这里挂
 *   3. 深色底 —— 避免 ScrollView 内容不满屏时露出主题默认白底
 *      （这个坑在桌宠设置页真的出过一次）
 */
public class EfActivity extends AppCompatActivity {

    /** 页面底色，和设计系统的 bg_page 一致 */
    public static final int BG = 0xFF0E0A18;

    protected float density;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
    }

    /** dp 转 px */
    public int dp(float v) {
        return Math.round(v * density);
    }

    /**
     * 把内容包进一个带底色的滚动容器。
     *
     * 两个必须同时做的动作，少一个就会出问题：
     *   · LinearLayout 设底色 —— 正常情况
     *   · ScrollView **也要**设底色 —— 内容不满一屏时，下方露出的是
     *     ScrollView 自己的背景（主题默认白）
     */
    public android.widget.ScrollView wrapScroll(LinearLayout content) {
        content.setBackgroundColor(BG);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(content);
        return sv;
    }

    /** 新建一个竖向内容容器，带统一的内边距和底色 */
    public LinearLayout contentColumn() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(BG);
        col.setPadding(dp(18), dp(24), dp(18), dp(30));
        return col;
    }

    /**
     * 在内容容器顶部挂一条自绘状态栏（时间/信号/电量）。
     *
     * 手机模式那十几个页面都要这个，之前每个文件复制一遍。
     * 现在一行搞定。
     */
    public View attachStatusBar(LinearLayout content) {
        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, content, false);
        content.addView(sb);
        new PhoneStatusBar(this, sb).bind();
        return sb;
    }

    /**
     * 让根布局让出系统状态栏。
     *
     * 用在自绘状态栏的页面上 —— 否则系统的时间/电量会和自绘的重叠。
     * 注意必须设在**根**布局上，设在子 View 上不生效
     * （这个我在漂流瓶页面踩过一次）。
     */
    public void fitSystemWindows(View root) {
        root.setFitsSystemWindows(true);
    }

    /** 一个简单的标题 + 副标题，多处以同样样式出现 */
    public void addTitle(LinearLayout col, String title, String sub) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(title);
        tv.setTextSize(22);
        tv.setTextColor(0xFFF4F2FF);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dp(16), 0, dp(6));
        col.addView(tv);

        if (sub != null && !sub.isEmpty()) {
            android.widget.TextView s = new android.widget.TextView(this);
            s.setText(sub);
            s.setTextSize(13);
            s.setTextColor(0xFF8B84A8);
            s.setLineSpacing(dp(4), 1f);
            s.setPadding(0, 0, 0, dp(18));
            col.addView(s);
        }
    }

    /** 小节标题 */
    public void addSection(LinearLayout col, String t) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(t);
        tv.setTextSize(12);
        tv.setTextColor(0xFF8B84A8);
        tv.setPadding(dp(2), dp(20), 0, dp(8));
        col.addView(tv);
    }

    /** 说明文字 */
    public android.widget.TextView hint(String t) {
        android.widget.TextView tv = new android.widget.TextView(this);
        tv.setText(t);
        tv.setTextSize(12);
        tv.setTextColor(0xFF6F6A8B);
        tv.setLineSpacing(dp(4), 1f);
        tv.setPadding(dp(4), dp(4), dp(4), dp(10));
        return tv;
    }

    /**
     * 一行「标签 —— 值 ›」的可点条目。
     * 设置类页面里这种行出现了几十次，抽出来。
     */
    public View settingRow(LinearLayout parent, String label, String value,
                           View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        r.setLayoutParams(lp);
        r.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(12)));
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (click != null) {
            r.setOnClickListener(click);
        }

        android.widget.TextView l = new android.widget.TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(0xFFF4F2FF);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(l);

        android.widget.TextView v = new android.widget.TextView(this);
        v.setText(value);
        v.setTextSize(13);
        v.setTextColor(0xFF8B84A8);
        v.setMaxLines(1);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        r.addView(v);

        android.widget.TextView arrow = new android.widget.TextView(this);
        arrow.setText("  ›");
        arrow.setTextSize(14);
        arrow.setTextColor(0xFF5A5478);
        r.addView(arrow);

        if (parent != null) {
            parent.addView(r);
        }
        return r;
    }
}
