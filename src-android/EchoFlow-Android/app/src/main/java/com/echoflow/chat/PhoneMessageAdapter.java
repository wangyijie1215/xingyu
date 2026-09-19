package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 手机模式的消息适配器：微信风左右气泡。
 *
 * 与主聊天 MessageAdapter 的差异：
 *  · 不解析 Markdown（微信里没人发 markdown），纯文本
 *  · 白底对方气泡 + 微信绿自己气泡
 *  · 对方头像来自角色卡，自己用占位
 */
public class PhoneMessageAdapter extends RecyclerView.Adapter<PhoneMessageAdapter.VH> {

    private final List<Message> messages;
    private final Bitmap avatar;

    /** 正在生成的那条（用于显示「对方正在输入…"） */
    private Message generating;

    public PhoneMessageAdapter(List<Message> messages, Bitmap avatar) {
        this.messages = messages;
        this.avatar = avatar;
    }

    public void setGenerating(Message m) {
        this.generating = m;
    }

    public void notifyLast() {
        if (messages.isEmpty()) {
            return;
        }
        notifyItemChanged(messages.size() - 1);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wx_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = messages.get(position);
        boolean isUser = "user".equals(m.role);
        Context ctx = h.itemView.getContext();

        // 上方显示时间（间隔超过 5 分钟才显示）
        if (position == 0 || shouldShowTime(messages.get(position - 1), m)) {
            h.time.setVisibility(View.VISIBLE);
            h.time.setText(PhoneFormat.chatTime(System.currentTimeMillis()));
        } else {
            h.time.setVisibility(View.GONE);
        }

        // 左右布局：用权重把气泡推到两侧
        if (isUser) {
            h.leftSpace.setVisibility(View.VISIBLE);
            h.rightSpace.setVisibility(View.GONE);
            h.avatar.setVisibility(View.GONE);
            h.avatarRight.setVisibility(View.GONE);
            h.bubble.setBackgroundResource(R.drawable.wx_bubble_right);
            h.text.setTextColor(0xFF1A1A1A);
        } else {
            h.leftSpace.setVisibility(View.GONE);
            h.rightSpace.setVisibility(View.VISIBLE);
            h.avatar.setVisibility(View.VISIBLE);
            h.avatarRight.setVisibility(View.GONE);
            if (avatar != null) {
                h.avatar.setImageBitmap(avatar);
            } else {
                h.avatar.setImageResource(R.drawable.app_icon);
            }
            h.bubble.setBackgroundResource(R.drawable.wx_bubble_left);
            h.text.setTextColor(0xFF1A1A1A);
        }

        String content = m.content == null ? "" : m.content;
        // 生成中且还没吐字 → 显示「对方正在输入…"
        if (m == generating && content.trim().isEmpty()) {
            h.text.setText("对方正在输入…");
            h.text.setTypeface(null, Typeface.ITALIC);
            h.text.setTextColor(0xFF999999);
        } else {
            h.text.setText(content);
            h.text.setTypeface(null, Typeface.NORMAL);
            h.text.setTextColor(0xFF1A1A1A);
        }
    }

    private boolean shouldShowTime(Message prev, Message cur) {
        return false;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView time;
        final View leftSpace;
        final View rightSpace;
        final ImageView avatar;
        final ImageView avatarRight;
        final View bubble;
        final TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.wx_time);
            leftSpace = itemView.findViewById(R.id.wx_left_space);
            rightSpace = itemView.findViewById(R.id.wx_right_space);
            avatar = itemView.findViewById(R.id.wx_avatar);
            avatarRight = itemView.findViewById(R.id.wx_avatar_right);
            bubble = itemView.findViewById(R.id.wx_bubble);
            text = itemView.findViewById(R.id.wx_text);
        }
    }
}