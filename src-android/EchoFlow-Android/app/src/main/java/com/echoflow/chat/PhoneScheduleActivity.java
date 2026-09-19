package com.echoflow.chat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 她的今天（日程表）—— 新功能。
 *
 * 这是「她有自己在过的生活"最直接的证明：打开就能看到她几点在做什么，
 * 并且高亮**当前时段**。而且这个时段会注入 Prompt，
 * 让她在忙的时候回复更短 —— 日程不是摆设，是真的在影响对话。
 */
public class PhoneScheduleActivity extends AppCompatActivity {

    private final List<CharacterCard> cards = new ArrayList<>();
    private List<Schedule.Item> items = new ArrayList<>();
    private CharacterCard current;
    private LinearLayout charRow;
    private RecyclerView list;
    private ItemAdapter adapter;
    private TextView nowText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_schedule);

        new PhoneStatusBar(this, findViewById(android.R.id.content)).bind();

        findViewById(R.id.sc_back).setOnClickListener(v -> finish());
        findViewById(R.id.sc_now).setOnClickListener(v -> {
            // 跳到当前时段那一条
            Schedule.Now n = Schedule.now(items);
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).what.equals(n.what)) {
                    list.smoothScrollToPosition(i);
                    break;
                }
            }
        });

        charRow = findViewById(R.id.sc_chars);
        nowText = findViewById(R.id.sc_now_text);
        list = findViewById(R.id.sc_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter();
        list.setAdapter(adapter);

        cards.addAll(PhoneStore.recentCards(this));
        if (cards.isEmpty()) {
            findViewById(R.id.sc_now_card).setVisibility(View.GONE);
            return;
        }
        String pick = getIntent().getStringExtra("card_id");
        current = null;
        if (pick != null) {
            for (CharacterCard c : cards) {
                if (c.id.equals(pick)) {
                    current = c;
                    break;
                }
            }
        }
        if (current == null) {
            current = cards.get(0);
        }

        buildCharRow();
        loadSchedule();
    }

    private void buildCharRow() {
        charRow.removeAllViews();
        for (CharacterCard c : cards) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            item.setPadding(dp(8), dp(2), dp(8), dp(2));
            item.setClickable(true);

            ImageView iv = new ImageView(this);
            int s = dp(44);
            iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Bitmap bmp = CardStore.loadAvatar(this, c.id);
            if (bmp != null) {
                iv.setImageBitmap(bmp);
            } else {
                iv.setImageResource(R.drawable.app_icon);
            }
            item.addView(iv);

            TextView name = new TextView(this);
            name.setText(c.name);
            name.setTextSize(11);
            name.setTextColor(c.id.equals(current.id) ? 0xFF07C160 : 0xFF888888);
            name.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            nlp.topMargin = dp(4);
            name.setLayoutParams(nlp);
            item.addView(name);

            // 选中态：底下加一条绿色横线
            View bar = new View(this);
            int bw = dp(28);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(bw, dp(2));
            blp.topMargin = dp(4);
            bar.setLayoutParams(blp);
            bar.setBackground(EfUi.roundRectPx(
                    c.id.equals(current.id) ? 0xFF07C160 : 0x00000000, 0, dp(1)));
            item.addView(bar);

            item.setOnClickListener(v -> {
                current = c;
                buildCharRow();
                loadSchedule();
            });
            charRow.addView(item);
        }
    }

    private void loadSchedule() {
        items = Schedule.load(this, current.id);
        adapter.notifyDataSetChanged();

        Schedule.Now n = Schedule.now(items);
        String hm = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date());
        StringBuilder sb = new StringBuilder();
        sb.append(n.what);
        if (n.note != null && !n.note.isEmpty()) {
            sb.append("\n").append(n.note);
        }
        sb.append("\n").append(hm).append(n.busy ? " · 正在忙" : " · 有空");
        nowText.setText(sb.toString());
        nowText.setTextColor(n.busy ? 0xFF888888 : 0xFF07C160);

        // 滚动到当前时段
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).what.equals(n.what)) {
                final int pos = i;
                list.post(() -> list.scrollToPosition(Math.max(0, pos - 1)));
                break;
            }
        }
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Schedule.Item it = items.get(position);
            Schedule.Now n = Schedule.now(items);
            boolean isNow = it.what.equals(n.what);

            h.time.setText(it.time);
            h.what.setText(it.what);
            h.what.setTextColor(isNow ? 0xFF07C160 : 0xFF1A1A1A);
            h.what.setTypeface(null, isNow
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            if (it.note == null || it.note.isEmpty()) {
                h.note.setVisibility(View.GONE);
            } else {
                h.note.setVisibility(View.VISIBLE);
                h.note.setText(it.note);
            }
            h.dot.setBackgroundResource(isNow ? R.drawable.phone_dot_on : R.drawable.phone_dot_off);
            h.dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    isNow ? 0xFF07C160 : 0x33000000));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView time;
            final View dot;
            final TextView what;
            final TextView note;

            VH(@NonNull View itemView) {
                super(itemView);
                time = itemView.findViewById(R.id.si_time);
                dot = itemView.findViewById(R.id.si_dot);
                what = itemView.findViewById(R.id.si_what);
                note = itemView.findViewById(R.id.si_note);
            }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}