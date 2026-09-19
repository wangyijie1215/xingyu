package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界书管理页。
 *
 * 支持：
 *  · 看内置的 4 本世界书，以及每本里的条目
 *  · 给某个角色启用/停用某本世界书
 *  · 看每本自带的天气（切换世界书 → 天气跟着换）
 *
 * 编辑条目没做（要做的话得再来一个编辑器页，收益不高）——
 * 但内置的 4 本已经覆盖了 4 种风格，够用来演示机制。
 */
public class LorebookActivity extends AppCompatActivity {

    private String cardId;
    private LinearLayout body;
    /** 当前查看的世界书（点标题切换） */
    private String viewingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cardId = getIntent().getStringExtra("card_id");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E0A18);

        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, root, false);
        root.addView(sb);
        new PhoneStatusBar(this, sb).bind();

        root.addView(bar());

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(30));
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        render();
    }

    private View bar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundResource(R.drawable.wx_topbar);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        bar.setPadding(dp(12), 0, dp(12), 0);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(26);
        back.setTextColor(0xFF181818);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView t = new TextView(this);
        t.setText("世界书");
        t.setTextSize(17);
        t.setTextColor(0xFF181818);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(t);

        TextView add = new TextView(this);
        add.setText("恢复");
        add.setTextSize(14);
        add.setTextColor(0xFF07C160);
        add.setGravity(Gravity.CENTER);
        add.setLayoutParams(new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT));
        add.setOnClickListener(v -> {
            BuiltinLorebooks.reinstall(this);
            Toast.makeText(this, "内置世界书已恢复", Toast.LENGTH_SHORT).show();
            render();
        });
        bar.addView(add);
        return bar;
    }

    private void render() {
        body.removeAllViews();

        List<Lorebook> books = Lorebook.list(this);
        List<String> bound = cardId == null
                ? new ArrayList<>() : Lorebook.bindingsFor(this, cardId);

        // 说明
        TextView intro = new TextView(this);
        intro.setText("世界书 = 按关键词触发的小设定。说到相关词，对应条目才会注入给模型。\n"
                + "每本世界书还自带天气 —— 换一本，她那个世界的天就换了。");
        intro.setTextSize(12);
        intro.setTextColor(0xFF8B84A8);
        intro.setLineSpacing(dp(4), 1f);
        intro.setPadding(dp(4), 0, dp(4), dp(14));
        body.addView(intro);

        if (cardId != null) {
            TextView cur = new TextView(this);
            CharacterCard c = CardStore.getCard(this, cardId);
            cur.setText("正在看的角色：" + (c == null ? "未知" : c.name));
            cur.setTextSize(13);
            cur.setTextColor(0xFFB9A8FF);
            cur.setPadding(dp(4), 0, dp(4), dp(10));
            body.addView(cur);
        }

        for (Lorebook lb : books) {
            body.addView(bookCard(lb, bound.contains(lb.id)));
        }

        if (books.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有世界书。点右上角「恢复」装入内置的 4 本。");
            empty.setTextSize(13);
            empty.setTextColor(0xFF8B84A8);
            body.addView(empty);
        }
    }

    private View bookCard(final Lorebook lb, boolean enabled) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        card.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(16)));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        // 标题行：名字 + 启用开关
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(lb.name);
        name.setTextSize(16);
        name.setTextColor(0xFFF4F2FF);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(name);

        if (cardId != null && !lb.global) {
            TextView toggle = new TextView(this);
            toggle.setText(enabled ? "已启用" : "未启用");
            toggle.setTextSize(12);
            toggle.setTextColor(enabled ? 0xFF7FE0C4 : 0xFF8B84A8);
            toggle.setBackground(EfUi.roundRectPx(
                    enabled ? 0x2A7FE0C4 : 0x1AFFFFFF, 0, dp(10)));
            toggle.setPadding(dp(10), dp(5), dp(10), dp(5));
            toggle.setOnClickListener(v -> {
                boolean now = Lorebook.bindingsFor(this, cardId).contains(lb.id);
                Lorebook.bind(this, cardId, lb.id, !now);
                render();
            });
            head.addView(toggle);
        } else if (lb.global) {
            TextView g = new TextView(this);
            g.setText("全局");
            g.setTextSize(12);
            g.setTextColor(0xFFE8C87A);
            head.addView(g);
        }
        card.addView(head);

        TextView desc = new TextView(this);
        desc.setText(lb.description);
        desc.setTextSize(12);
        desc.setTextColor(0xFF8B84A8);
        desc.setLineSpacing(dp(3), 1f);
        desc.setPadding(0, dp(6), 0, 0);
        card.addView(desc);

        // 今天的天气
        BuiltinLorebooks.Weather w = BuiltinLorebooks.todayWeather(lb.id, Fortune.today());
        if (w != null) {
            TextView weather = new TextView(this);
            weather.setText("今天的天气：" + w.cond + " · " + w.temp);
            weather.setTextSize(12);
            weather.setTextColor(0xFF79D3F5);
            weather.setPadding(0, dp(8), 0, 0);
            card.addView(weather);
        }

        // 展开/收起条目
        final boolean[] expanded = {false};
        TextView toggleEntries = new TextView(this);
        toggleEntries.setText("查看 " + lb.entries.size() + " 条设定 ▾");
        toggleEntries.setTextSize(12);
        toggleEntries.setTextColor(0xFFB9A8FF);
        toggleEntries.setPadding(0, dp(10), 0, 0);
        card.addView(toggleEntries);

        LinearLayout entriesBox = new LinearLayout(this);
        entriesBox.setOrientation(LinearLayout.VERTICAL);
        entriesBox.setVisibility(View.GONE);
        card.addView(entriesBox);

        for (Lorebook.Entry e : lb.entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.topMargin = dp(10);
            row.setLayoutParams(rlp);

            TextView t = new TextView(this);
            StringBuilder k = new StringBuilder();
            if (e.constant) {
                k.append("常驻");
            } else {
                for (int i = 0; i < e.keys.size(); i++) {
                    if (i > 0) {
                        k.append(" / ");
                    }
                    k.append(e.keys.get(i));
                }
            }
            t.setText((e.comment.isEmpty() ? "条目" : e.comment) + "　[" + k + "]");
            t.setTextSize(12);
            t.setTextColor(0xFFE8C87A);
            row.addView(t);

            TextView c = new TextView(this);
            c.setText(e.content);
            c.setTextSize(12);
            c.setTextColor(0xFFA9A3C2);
            c.setLineSpacing(dp(3), 1f);
            c.setPadding(0, dp(3), 0, 0);
            row.addView(c);

            entriesBox.addView(row);
        }

        toggleEntries.setOnClickListener(v -> {
            expanded[0] = !expanded[0];
            entriesBox.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
            toggleEntries.setText((expanded[0] ? "收起 ▴" : "查看 " + lb.entries.size() + " 条设定 ▾"));
        });

        return card;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}