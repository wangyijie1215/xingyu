package com.echoflow.chat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 「星轨"设计系统的运行时组件工厂。
 *
 * 现有工程大量使用代码构建视图（CharacterProfileActivity 就是这么写的），
 * 因此这里把设计 token 封装成 Java 工厂方法，保证新页面与设计稿一致，
 * 且不需要为每个小控件单独写 xml。
 */
public class EfUi {

    // ---------- 基础换算 ----------
    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, int resId) {
        return androidx.core.content.ContextCompat.getColor(c, resId);
    }

    // ---------- 胶囊 Chip ----------
    /**
     * 语义胶囊。tone: 0 中性 / 1 主色 / 2 羁绊 / 3 情绪 / 4 记忆 / 5 语音 / 6 神社金
     */
    public static TextView chip(Context c, String text, int tone) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setIncludeFontPadding(false);
        int padH = dp(c, 10);
        int padV = dp(c, 4);
        tv.setPadding(padH, padV, padH, padV);

        int fg;
        int bg;
        int stroke;
        switch (tone) {
            case 1:
                fg = color(c, R.color.ef_primary_light);
                bg = color(c, R.color.ef_primary_16);
                stroke = color(c, R.color.ef_primary_42);
                break;
            case 2:
                fg = color(c, R.color.ef_bond);
                bg = color(c, R.color.ef_bond_14);
                stroke = color(c, R.color.ef_bond_38);
                break;
            case 3:
                fg = color(c, R.color.ef_emotion);
                bg = color(c, R.color.ef_emotion_14);
                stroke = color(c, R.color.ef_emotion_38);
                break;
            case 4:
                fg = color(c, R.color.ef_memory);
                bg = color(c, R.color.ef_memory_14);
                stroke = color(c, R.color.ef_memory_38);
                break;
            case 5:
                fg = color(c, R.color.ef_voice);
                bg = color(c, R.color.ef_voice_14);
                stroke = color(c, R.color.ef_voice_38);
                break;
            case 6:
                fg = color(c, R.color.ef_shrine_gold);
                bg = color(c, R.color.ef_shrine_gold_20);
                stroke = color(c, R.color.ef_shrine_gold_42);
                break;
            default:
                fg = color(c, R.color.ef_text_2);
                bg = color(c, R.color.ef_glass_2);
                stroke = color(c, R.color.ef_hairline);
                break;
        }
        tv.setTextColor(fg);
        tv.setBackground(roundRect(bg, stroke, 999));
        return tv;
    }

    // ---------- 进度条 ----------
    public static class MeterView extends View {
        private float ratio = 0f;
        private int from = 0xFF9B7BFF;
        private int to = 0xFFC7A8FF;
        private int track = 0x1AFFFFFF;

        public MeterView(Context c) {
            super(c);
        }

        public void set(float ratio, int from, int to) {
            this.ratio = Math.max(0f, Math.min(1f, ratio));
            this.from = from;
            this.to = to;
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            float h = getHeight();
            float r = h / 2f;
            p.setColor(track);
            canvas.drawRoundRect(0, 0, getWidth(), h, r, r, p);
            if (ratio > 0f) {
                p.setShader(new android.graphics.LinearGradient(
                        0, 0, getWidth() * ratio, 0, from, to,
                        android.graphics.Shader.TileMode.CLAMP));
                canvas.drawRoundRect(0, 0, getWidth() * ratio, h, r, r, p);
                p.setShader(null);
            }
        }
    }

    /** 高 5dp 的语义进度条 */
    public static MeterView meter(Context c, float ratio, int from, int to) {
        MeterView m = new MeterView(c);
        m.set(ratio, from, to);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 5));
        m.setLayoutParams(lp);
        return m;
    }

    // ---------- 分段条（签文的六项运势用）----------
    /**
     * 5 格分段条：已达成格用朱→金渐变。神社的签文本来就是分格写的，
     * 用分段而非连续进度条更贴题。
     */
    public static LinearLayout segmentBar(Context c, int value, int segments) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int filled = Math.round(value / (100f / segments));
        for (int i = 0; i < segments; i++) {
            View seg = new View(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 6), 1f);
            lp.rightMargin = i == segments - 1 ? 0 : dp(c, 3);
            seg.setLayoutParams(lp);
            if (i < filled) {
                GradientDrawable g = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        new int[]{color(c, R.color.ef_shrine_red), color(c, R.color.ef_shrine_gold)});
                g.setCornerRadius(dp(c, 3));
                seg.setBackground(g);
            } else {
                seg.setBackground(roundRect(0x243A3128, 0, 3));
            }
            row.addView(seg);
        }
        return row;
    }

    // ---------- 文本 ----------
    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        tv.setIncludeFontPadding(false);
        return tv;
    }

    public static TextView label(Context c, String s) {
        return text(c, s, 11, color(c, R.color.ef_text_3), false);
    }

    // ---------- 卡片 ----------
    public static LinearLayout card(Context c, boolean raised) {
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setBackgroundResource(raised ? R.drawable.ef_bg_raised : R.drawable.ef_bg_glass);
        int p = dp(c, 14);
        ll.setPadding(p, p, p, p);
        return ll;
    }

    /** 玻璃卡 + 上下外边距 */
    public static LinearLayout cardSpaced(Context c, boolean raised, int topMarginDp) {
        LinearLayout ll = card(c, raised);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        ll.setLayoutParams(lp);
        return ll;
    }

    // ---------- 区块标题 ----------
    public static LinearLayout sectionHead(Context c, String title, String action, View.OnClickListener onAction) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(c, 10);
        rowLp.topMargin = dp(c, 22);
        row.setLayoutParams(rowLp);

        TextView t = text(c, title, 16, color(c, R.color.ef_text_1), true);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (action != null && !action.isEmpty()) {
            TextView a = text(c, action, 12, color(c, R.color.ef_primary), false);
            if (onAction != null) {
                a.setOnClickListener(onAction);
            }
            row.addView(a);
        }
        return row;
    }

    // ---------- 圆角背景工具 ----------
    public static GradientDrawable roundRect(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (stroke != 0) {
            g.setStroke(1, stroke);
        }
        g.setCornerRadius(radiusDp);
        return g;
    }

    public static GradientDrawable roundRectPx(int fill, int stroke, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (stroke != 0) {
            g.setStroke(1, stroke);
        }
        g.setCornerRadius(radiusPx);
        return g;
    }

    // ---------- 亲密度环 ----------
    /** 62dp 的亲密度环：紫→橙渐变描边，中心「72 / 100" */
    public static class BondRingView extends View {
        private int bond = 0;

        public BondRingView(Context c) {
            super(c);
        }

        public void setBond(int bond) {
            this.bond = Math.max(0, Math.min(100, bond));
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float stroke = 5f * getResources().getDisplayMetrics().density;
            float r = Math.min(cx, cy) - stroke / 2f;

            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            p.setColor(0x1AFFFFFF);
            canvas.drawCircle(cx, cy, r, p);

            if (bond > 0) {
                p.setShader(new android.graphics.SweepGradient(cx, cy,
                        new int[]{0xFF9B7BFF, 0xFFFFA657, 0xFF9B7BFF}, new float[]{0f, 0.5f, 1f}));
                android.graphics.RectF oval = new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
                canvas.drawArc(oval, -90f, bond * 3.6f, false, p);
                p.setShader(null);
            }

            // 中心数值
            p.setStyle(android.graphics.Paint.Style.FILL);
            p.setColor(0xFFF4F2FF);
            p.setTextAlign(android.graphics.Paint.Align.CENTER);
            float d = getResources().getDisplayMetrics().density;
            p.setTextSize(17f * d);
            p.setFakeBoldText(true);
            canvas.drawText(String.valueOf(bond), cx, cy + 3f * d, p);
            p.setTextSize(9f * d);
            p.setColor(0xFF8B84A8);
            p.setFakeBoldText(false);
            canvas.drawText("/100", cx, cy + 14f * d, p);
        }
    }

    public static BondRingView bondRing(Context c, int bond) {
        BondRingView v = new BondRingView(c);
        v.setBond(bond);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 62), dp(c, 62));
        v.setLayoutParams(lp);
        return v;
    }

    // ---------- 排版工具 ----------
    /** 一行的行距友好的 TextView */
    public static TextView body(Context c, String s) {
        TextView tv = text(c, s, 14, color(c, R.color.ef_text_1), false);
        tv.setLineSpacing(dp(c, 6), 1f);
        return tv;
    }

    public static TextView bodyDim(Context c, String s) {
        TextView tv = text(c, s, 13, color(c, R.color.ef_text_2), false);
        tv.setLineSpacing(dp(c, 5), 1f);
        return tv;
    }

    /** 水平间隔 */
    public static View space(Context c, int wDp, int hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp)));
        return v;
    }

    /** 水平方向的固定宽度间隔（用于 Chip 之间） */
    public static View spacer(Context c, int wDp) {
        return space(c, wDp, 1);
    }
}