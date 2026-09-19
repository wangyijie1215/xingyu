package com.echoflow.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 手机模式 · 会话列表（微信风）。
 *
 * 每个角色是一个「联系人"。点进去就是微信式聊天。
 * 未读数由 PhoneStore 的消息条数推算——这里简化成一个标记，
 * 真实场景应该维护已读游标，但当前不需要那么重。
 */
public class PhoneListActivity extends AppCompatActivity {

    private final List<CharacterCard> cards = new ArrayList<>();
    private SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_list);

        findViewById(R.id.ph_back).setOnClickListener(v -> finish());
        findViewById(R.id.ph_new).setOnClickListener(v -> {
            Intent i = new Intent(this, CardListActivity.class);
            startActivity(i);
        });

        RecyclerView list = findViewById(R.id.ph_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter();
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cards.clear();
        cards.addAll(PhoneStore.recentCards(this));
        adapter.notifyDataSetChanged();
    }

    class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_wx_session, parent, false);
            return new VH(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CharacterCard c = cards.get(position);
            h.name.setText(c.name == null || c.name.isEmpty() ? "未命名" : c.name);

            Bitmap bmp = CardStore.loadAvatar(PhoneListActivity.this, c.id);
            if (bmp != null) {
                h.avatar.setImageBitmap(bmp);
                h.avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                h.avatar.setImageResource(R.drawable.app_icon);
            }

            // 没聊过时显示「此刻在做什么"，而不是干巴巴的"还没有聊过"
            Schedule.Now now = Schedule.now(Schedule.load(PhoneListActivity.this, c.id));
            String preview = PhoneStore.lastPreview(PhoneListActivity.this, c.id, 20);
            if (preview.isEmpty() && now.what != null && !now.what.isEmpty()) {
                preview = "此刻：" + now.what;
            }
            List<Message> msgs = PhoneStore.list(PhoneListActivity.this, c.id);
            if (msgs.isEmpty()) {
                h.preview.setText("还没有聊过");
                h.preview.setTextColor(0xFFBBBBBB);
                h.time.setText("");
                h.badge.setVisibility(View.GONE);
            } else {
                h.preview.setText(preview);
                h.preview.setTextColor(0xFF999999);
                h.time.setText(PhoneFormat.sessionTime(PhoneStore.lastActive(PhoneListActivity.this, c.id)));
                // 真实未读角标
                int unread = ReadCursor.unread(PhoneListActivity.this, c.id);
                if (unread > 0) {
                    h.badge.setVisibility(View.VISIBLE);
                    h.badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                } else {
                    h.badge.setVisibility(View.GONE);
                }
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(PhoneListActivity.this, PhoneChatActivity.class);
                i.putExtra("card_id", c.id);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView badge;
            final TextView name;
            final TextView preview;
            final TextView time;

            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.ph_avatar);
                badge = itemView.findViewById(R.id.ph_badge);
                name = itemView.findViewById(R.id.ph_name);
                preview = itemView.findViewById(R.id.ph_preview);
                time = itemView.findViewById(R.id.ph_time);
            }
        }
    }
}