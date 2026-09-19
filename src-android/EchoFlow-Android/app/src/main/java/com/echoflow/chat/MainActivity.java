package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 星语首页。
 *
 * 设计取向：**以角色为中心，而不是以聊天记录为中心**。
 *
 * 旧版是一个空聊天框 + 一排按钮，用户进来不知道该点哪。
 * 新版回答三个问题：
 *   1. 她此刻在做什么？      → 「今天"卡片（读取日程 + 情绪）
 *   2. 我该找谁继续聊？      → 「继续对话"横滑角色卡（带未读/在线点）
 *   3. 最近发生了什么？      → 「最近动态"预览（来自手机里的朋友圈）
 *
 * 入口收敛：原先把 5 个按钮铺在顶栏，加到第 6 个时「设置"被挤出屏幕。
 * 现在只在顶栏留「手机"和一个 ⋯ 菜单，其余全进菜单。
 */
public class MainActivity extends AppCompatActivity {

    private LinearLayout strip;
    private RecyclerView momentList;
    private MomentAdapter momentAdapter;
    private final List<Post> moments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 首次启动装入内置角色：用户一进来就有内容可聊，而不是空白页
        BuiltinCharacters.installIfNeeded(getApplicationContext());
        BuiltinLorebooks.installIfNeeded(getApplicationContext());

        setContentView(R.layout.activity_main);

        TextView subtitle = findViewById(R.id.subtitle);
        if (subtitle != null) {
            subtitle.setText(getString(R.string.app_slogan));
        }

        strip = findViewById(R.id.char_strip);

        momentList = findViewById(R.id.moments_preview);
        momentList.setLayoutManager(new LinearLayoutManager(this));
        momentAdapter = new MomentAdapter();
        momentList.setAdapter(momentAdapter);

        // 手机模式（模拟手机主屏）
        findViewById(R.id.btn_phone_mode).setOnClickListener(v ->
                startActivity(new Intent(this, PhoneHomeActivity.class)));

        // 溢出菜单：把其余入口收进来，避免顶栏继续膨胀
        findViewById(R.id.btn_more).setOnClickListener(this::showMoreMenu);

        // 底部
        // btn_quick_chat 是那个「说点什么…」输入框 —— 点了进手机模式聊天
        View btnQuick = findViewById(R.id.btn_quick_chat);
        btnQuick.setOnClickListener(v -> openPrimaryChat());

        // 「开始对话」这个按钮的 id 叫 btn_cards（历史遗留，和卡片无关）。
        // 它以前跳角色库，用户得再点角色、再点聊天才进得去 ——
        // 这就是"手机外对话没了"的真正原因：入口被藏在两层之后。
        // 现在直接进**角色扮演**聊天。
        findViewById(R.id.btn_cards).setOnClickListener(v -> openRoleplayChat());

        findViewById(R.id.btn_open_moments).setOnClickListener(v ->
                startActivity(new Intent(this, PhoneMomentsActivity.class)));
    }

    /**
     * 直接进入角色扮演聊天。
     *
     * 没有角色时引导去创建，而不是静默跳角色库让人一头雾水。
     */
    private void openRoleplayChat() {
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards.isEmpty()) {
            List<CharacterCard> all = CardStore.listCards(this);
            if (all.isEmpty()) {
                android.widget.Toast.makeText(this, "还没有角色，先创建一个",
                        android.widget.Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CardEditActivity.class));
                return;
            }
            cards = all;
        }
        Intent i = new Intent(this, CardChatActivity.class);
        i.putExtra("card_id", cards.get(0).id);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildCharacterStrip();
        reloadMoments();
        refreshTodayCard();
    }

    // ==================================================================
    // 顶栏溢出菜单
    // ==================================================================

    /**
     * 顶栏 ⋯ 菜单。
     *
     * 不用 PopupMenu：它的背景由系统主题控制，实测在某些系统上出现
     * 白底白字（主题里的 android:popupBackground 对 AppCompat 的 PopupMenu
     * 不生效，反射拿 getPopup 也不通）。
     * 自己搭一个列表对话框，颜色 100% 可控，不依赖任何主题行为。
     */
    private void showMoreMenu(View anchor) {
        showOverflowDialog();
    }

    private void showOverflowDialog() {
        final String[] items = {
                "角色库", "新建角色", "今日运势", "通用助手",
                "桌宠", "世界书", "生图", "日程", "设置", "关于"
        };

        // 自建列表：深紫底 + 浅色文字，不依赖主题
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFF1E1633);
        int pad = dp(6);
        box.setPadding(pad, pad, pad, pad);

        final androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setView(box)
                        .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    EfUi.roundRectPx(0xFF1E1633, 0x33FFFFFF, dp(16)));
        }

        for (int i = 0; i < items.length; i++) {
            final String label = items[i];
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText(label);
            tv.setTextSize(16);
            tv.setTextColor(0xFFF4F2FF);
            tv.setPadding(dp(18), dp(14), dp(18), dp(14));
            tv.setClickable(true);
            tv.setFocusable(true);
            // 按下时给一点反馈
            tv.setOnTouchListener((v, e) -> {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(0x22FFFFFF);
                } else if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP
                        || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundColor(0x00000000);
                }
                return false;
            });
            tv.setOnClickListener(v -> {
                dialog.dismiss();
                handleMenu(label);
            });
            box.addView(tv);

            // 分隔线（最后一项不加）
            if (i < items.length - 1) {
                View line = new View(this);
                android.widget.LinearLayout.LayoutParams lp =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.leftMargin = dp(18);
                lp.rightMargin = dp(18);
                line.setLayoutParams(lp);
                line.setBackgroundColor(0x14FFFFFF);
                box.addView(line);
            }
        }

        dialog.show();
    }

    /** 菜单项点击分发 */
    private void handleMenu(String t) {
        switch (t) {
            case "角色库":
                startActivity(new Intent(this, CardListActivity.class));
                break;
            case "新建角色":
                startActivity(new Intent(this, CardEditActivity.class));
                break;
            case "今日运势":
                openShrine();
                break;
            case "日程":
                startActivity(new Intent(this, PhoneScheduleActivity.class));
                break;
            case "通用助手":
                startActivity(new Intent(this, GeneralAssistantActivity.class));
                break;
            case "桌宠":
                startActivity(new Intent(this, PetSettingsActivity.class));
                break;
            case "世界书":
                openLorebook();
                break;
            case "生图":
                openImageGen();
                break;
            case "设置":
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case "关于":
                showAbout();
                break;
        }
    }
    private void openLorebook() {
        Intent i = new Intent(this, LorebookActivity.class);
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (!cards.isEmpty()) {
            i.putExtra("card_id", cards.get(0).id);
        }
        startActivity(i);
    }

    private void openImageGen() {
        Intent i = new Intent(this, ImageGenActivity.class);
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (!cards.isEmpty()) {
            i.putExtra("card_id", cards.get(0).id);
        }
        startActivity(i);
    }

    private void openShrine() {
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards.isEmpty()) {
            startActivity(new Intent(this, CardListActivity.class));
            return;
        }
        Intent i = new Intent(this, ShrineActivity.class);
        i.putExtra("card_id", cards.get(0).id);
        startActivity(i);
    }

    private void showAbout() {
        String msg = getString(R.string.app_name) + "\n"
                + getString(R.string.app_slogan) + "\n\n"
                + "角色：" + CardStore.listCards(this).size() + " 个\n"
                + "动态：" + PostStore.listPosts(this).size() + " 条\n"
                + "模型：" + (ProviderStore.isOllama(this) ? "本地 Ollama" : "云端 API")
                + " · " + ProviderStore.effectiveModel(this);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("关于")
                .setMessage(msg)
                .setPositiveButton("好", null)
                .show();
    }

    // ==================================================================
    // 今天：她此刻在做什么
    // ==================================================================

    private void refreshTodayCard() {
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        TextView who = findViewById(R.id.today_who);
        TextView text = findViewById(R.id.today_text);
        TextView time = findViewById(R.id.today_time);
        if (cards.isEmpty()) {
            who.setText("还没有角色");
            text.setText("去「角色库」创建或导入一个角色，她就会出现在这里。");
            return;
        }
        CharacterCard c = cards.get(0);
        Schedule.Now now = Schedule.now(Schedule.load(this, c.id));
        EmotionState emo = CharacterStateStore.loadEmotion(this, c.id);
        RelationshipState rel = CharacterStateStore.loadRelationship(this, c.id);

        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" · ");
        sb.append(now.busy ? "正在" + now.what : "此刻" + (now.what == null || now.what.isEmpty() ? "闲着" : "" + now.what));
        who.setText(sb.toString());

        StringBuilder body = new StringBuilder();
        if (now.note != null && !now.note.isEmpty()) {
            body.append(now.note).append("\n");
        }
        body.append("关系 ").append(rel.levelName())
            .append(" · 亲密度 ").append(rel.points).append("/100");
        if (emo != null && emo.mood != null && !emo.mood.isEmpty()) {
            body.append(" · 情绪 ").append(emo.mood);
        }
        if (now.busy) {
            body.append("\n她这会儿在忙，回复可能会短一些。");
        }
        text.setText(body.toString());

        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    }

    // ==================================================================
    // 继续对话
    // ==================================================================

    private void buildCharacterStrip() {
        if (strip == null) {
            return;
        }
        strip.removeAllViews();

        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("点右下角「开始对话」去角色库挑一个");
            empty.setTextSize(13);
            empty.setTextColor(0xFF8B84A8);
            empty.setPadding(dp(16), dp(12), dp(16), dp(12));
            strip.addView(empty);
            return;
        }

        LayoutInflater inf = LayoutInflater.from(this);
        for (CharacterCard c : cards) {
            View item = inf.inflate(R.layout.item_home_character, strip, false);
            TextView name = item.findViewById(R.id.hc_name);
            TextView status = item.findViewById(R.id.hc_status);
            TextView unread = item.findViewById(R.id.hc_unread);
            ImageView avatar = item.findViewById(R.id.hc_avatar);
            View dot = item.findViewById(R.id.hc_dot);

            name.setText(c.name == null || c.name.isEmpty() ? "未命名" : c.name);

            Bitmap bmp = CardStore.loadAvatar(this, c.id);
            if (bmp != null) {
                avatar.setImageBitmap(bmp);
                avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }

            // 状态：此刻在做什么
            Schedule.Now now = Schedule.now(Schedule.load(this, c.id));
            status.setText(now.what == null || now.what.isEmpty() ? "在" : now.what);

            // 在线点：忙的时候变暗
            dot.setAlpha(now.busy ? 0.35f : 1f);

            // 未读：用已读游标算（进过会话就清零）
            int unreadCount = ReadCursor.unread(this, c.id);
            if (unreadCount > 0) {
                unread.setVisibility(View.VISIBLE);
                unread.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            } else {
                unread.setVisibility(View.GONE);
            }

            // 「继续对话」—— 点角色卡直接进**角色扮演**聊天。
            //
            // 这里原来进的是手机模式（微信风）聊天，和首页那个
            // 「开始对话」按钮是同一个毛病：把角色扮演藏起来了。
            // 手机模式改到长按，两种模式各有一个手势。
            item.setOnClickListener(v -> {
                Intent i = new Intent(this, CardChatActivity.class);
                i.putExtra("card_id", c.id);
                startActivity(i);
            });
            item.setOnLongClickListener(v -> {
                Intent i = new Intent(this, PhoneChatActivity.class);
                i.putExtra("card_id", c.id);
                startActivity(i);
                return true;
            });
            strip.addView(item);
        }
    }

    /** 打开最近活跃角色的聊天；没有角色则去角色库 */
    private void openPrimaryChat() {
        List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards.isEmpty()) {
            startActivity(new Intent(this, CardListActivity.class));
            return;
        }
        Intent i = new Intent(this, PhoneChatActivity.class);
        i.putExtra("card_id", cards.get(0).id);
        startActivity(i);
    }

    // ==================================================================
    // 最近动态
    // ==================================================================

    private void reloadMoments() {
        moments.clear();
        List<Post> all = PostStore.listPosts(this);
        // 只显示最近 8 条
        for (int i = 0; i < Math.min(8, all.size()); i++) {
            moments.add(all.get(i));
        }
        momentAdapter.notifyDataSetChanged();
        View empty = findViewById(R.id.moments_empty);
        if (empty != null) {
            empty.setVisibility(moments.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_moment_preview, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Post p = moments.get(position);
            h.author.setText(p.author);
            h.text.setText(p.content);
            h.time.setText(relative(p.time));

            Bitmap bmp = CardStore.loadAvatar(MainActivity.this, p.cardId);
            if (bmp != null) {
                h.avatar.setImageBitmap(bmp);
                h.avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                h.avatar.setImageResource(R.drawable.app_icon);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, PhoneMomentsActivity.class);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return moments.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView author;
            final TextView text;
            final TextView time;

            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.mv_avatar);
                author = itemView.findViewById(R.id.mv_author);
                text = itemView.findViewById(R.id.mv_text);
                time = itemView.findViewById(R.id.mv_time);
            }
        }
    }

    private String relative(long t) {
        if (t <= 0) {
            return "";
        }
        long min = (System.currentTimeMillis() - t) / 60000;
        if (min < 1) {
            return "刚刚";
        }
        if (min < 60) {
            return min + "分";
        }
        long hour = min / 60;
        if (hour < 24) {
            return hour + "时";
        }
        return (hour / 24) + "天";
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}