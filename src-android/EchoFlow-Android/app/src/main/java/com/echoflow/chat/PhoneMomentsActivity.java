package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 手机内的朋友圈。
 *
 * 与独立的 PostsActivity 的差异：
 *  · 带真实状态栏（跟手机模式的其他页保持一致）
 *  · 微信风顶部栏 + 封面 + 我的头像
 *  · 点动态可进对应角色的**手机聊天**（而不是主聊天）
 *
 * 动态内容与 PostsActivity 共用 PostStore，两个入口看到的是同一批数据。
 */
public class PhoneMomentsActivity extends AppCompatActivity {

    private final List<Post> posts = new ArrayList<>();
    private MomentAdapter adapter;
    private RecyclerView list;
    private boolean generating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_moments);

        new PhoneStatusBar(this, findViewById(android.R.id.content)).bind();

        findViewById(R.id.mo_back).setOnClickListener(v -> finish());
        findViewById(R.id.mo_camera).setOnClickListener(v ->
                Toast.makeText(this, "长按底部按钮可以让角色们发动态", Toast.LENGTH_SHORT).show());

        list = findViewById(R.id.mo_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MomentAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.mo_generate).setOnClickListener(v -> generateAll());

        // 我的头像：随便取一个角色的头像当「我"的（没有就用 app 图标）
        ImageView me = findViewById(R.id.mo_me_avatar);
        List<CharacterCard> cards = CardStore.listCards(this);
        if (!cards.isEmpty()) {
            Bitmap bmp = CardStore.loadAvatar(this, cards.get(0).id);
            if (bmp != null) {
                me.setImageBitmap(bmp);
            }
        }
        TextView meName = findViewById(R.id.mo_me_name);
        String personaName = ChatStore.getPersona(this);
        meName.setText(personaName == null || personaName.trim().isEmpty()
                ? "我" : "我");

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        posts.clear();
        posts.addAll(PostStore.listPosts(this));
        adapter.notifyDataSetChanged();
    }

    // ==================================================================
    // 生成动态
    // ==================================================================

    private void generateAll() {
        if (generating) {
            return;
        }
        String apiKey = SecureStore.get(this);
        boolean local = ProviderStore.isOllama(this);
        if (!local && (apiKey == null || apiKey.isEmpty())) {
            Toast.makeText(this, "请先到设置页配置模型（本地 Ollama 不需要密钥）",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        List<CharacterCard> cards = CardStore.listCards(this);
        if (cards.isEmpty()) {
            Toast.makeText(this, "先去角色库创建或导入角色吧", Toast.LENGTH_SHORT).show();
            return;
        }
        generating = true;
        Toast.makeText(this, "角色们正在发动态…", Toast.LENGTH_SHORT).show();
        generateOne(cards, 0);
    }
    private void generateOne(List<CharacterCard> cards, int index) {
        if (index >= cards.size()) {
            generating = false;
            runOnUiThread(() -> {
                reload();
                Toast.makeText(this, "新动态已生成", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        CharacterCard card = cards.get(index);
        String baseUrl = (card.baseUrl != null && !card.baseUrl.isEmpty())
                ? card.baseUrl : ProviderStore.effectiveBaseUrl(this);
        String model = (card.model != null && !card.model.isEmpty())
                ? card.model : ProviderStore.effectiveModel(this);

        // 必须在方法内取一次：局部变量才能安全地被下面的匿名类捕获
        final boolean isLocal = ProviderStore.isOllama(this);
        final String key = SecureStore.get(this) == null ? "" : SecureStore.get(this);

        // 带上情绪与时间，让动态更贴合人设与当下
        EmotionState emo = CharacterStateStore.loadEmotion(this, card.id);
        RelationshipState rel = CharacterStateStore.loadRelationship(this, card.id);

        List<Message> msgs = new ArrayList<>();
        StringBuilder sys = new StringBuilder();
        sys.append("你现在是 ").append(card.name).append("。");
        if (card.personality != null && !card.personality.isEmpty()) {
            sys.append("性格：").append(card.personality);
        }
        if (emo != null && emo.mood != null) {
            sys.append("\n你现在的情绪是：").append(emo.mood);
        }
        if (rel != null) {
            sys.append("\n你和对方的关系是「").append(rel.levelName())
               .append("」，亲密度 ").append(rel.points).append("/100");
        }
        sys.append("\n现在的时间是：").append(PhoneFormat.chatTime(System.currentTimeMillis()));
        sys.append("\n\n请以").append(card.name).append("的第一人称，发一条朋友圈动态。要求：");
        sys.append("\n1. 一到两句，像真人随手发的朋友圈");
        sys.append("\n2. 可以分享日常、心情、所见所感");
        sys.append("\n3. 完全符合角色性格和身份");
        sys.append("\n4. 不要写动作描写，不要用括号旁白 —— 朋友圈就是打字");
        sys.append("\n5. 不要解释，不要引号，直接输出动态正文");
        sys.append("\n6. 不要加「发了一条朋友圈」这类前缀");
        msgs.add(new Message("system", sys.toString()));
        msgs.add(new Message("user", "发一条今天的朋友圈"));

        final int next = index + 1;
        ApiClient.streamChat(baseUrl, key, model, msgs, isLocal,
                new ApiClient.Callback() {
                    private String buf = "";

                    @Override
                    public void onChunk(String fullText) {
                        buf = fullText;
                    }

                    @Override
                    public void onDone(boolean error, String errMsg) {
                        if (!error && buf != null && !buf.trim().isEmpty()) {
                            Post p = new Post();
                            p.id = "p" + System.currentTimeMillis() + "_" + index;
                            p.cardId = card.id;
                            p.author = card.name;
                            p.content = buf.trim();
                            p.time = System.currentTimeMillis();
                            p.likes = 0;
                            try {
                                PostStore.savePost(PhoneMomentsActivity.this, p);
                            } catch (Exception ignore) {
                            }
                        }
                        generateOne(cards, next);
                    }
                });
    }

    // ==================================================================

    class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_phone_moment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Post p = posts.get(position);
            h.name.setText(p.author);
            h.text.setText(p.content);
            h.meta.setText(relativeTime(p.time) + (p.likes > 0 ? "　♡ " + p.likes : ""));

            Bitmap bmp = CardStore.loadAvatar(PhoneMomentsActivity.this, p.cardId);
            if (bmp != null) {
                h.avatar.setImageBitmap(bmp);
                h.avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                h.avatar.setImageResource(R.drawable.app_icon);
            }

            // 配上场景图：按角色固定挑一张，保证每次看到的是同一张
            int imgRes = sceneFor(p.cardId);
            if (imgRes != 0) {
                h.image.setVisibility(View.VISIBLE);
                h.image.setImageResource(imgRes);
            } else {
                h.image.setVisibility(View.GONE);
            }

            h.likeBtn.setOnClickListener(v -> {
                PostStore.like(PhoneMomentsActivity.this, p.id);
                reload();
            });
            h.commentBtn.setOnClickListener(v ->
                    Toast.makeText(PhoneMomentsActivity.this, "评论功能待接入", Toast.LENGTH_SHORT).show());

            // 点头像/正文 → 进她的手机聊天
            View.OnClickListener goChat = v -> {
                if (p.cardId == null || p.cardId.isEmpty()) {
                    return;
                }
                Intent i = new Intent(PhoneMomentsActivity.this, PhoneChatActivity.class);
                i.putExtra("card_id", p.cardId);
                startActivity(i);
            };
            h.avatar.setOnClickListener(goChat);
            h.name.setOnClickListener(goChat);

            h.itemView.setOnLongClickListener(v -> {
                PostStore.deletePost(PhoneMomentsActivity.this, p.id);
                reload();
                Toast.makeText(PhoneMomentsActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return posts.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView name;
            final TextView text;
            final ImageView image;
            final TextView meta;
            final TextView likeBtn;
            final TextView commentBtn;

            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.mp_avatar);
                name = itemView.findViewById(R.id.mp_name);
                text = itemView.findViewById(R.id.mp_text);
                image = itemView.findViewById(R.id.mp_image);
                meta = itemView.findViewById(R.id.mp_meta);
                likeBtn = itemView.findViewById(R.id.mp_like_btn);
                commentBtn = itemView.findViewById(R.id.mp_comment_btn);
            }
        }
    }

    /** 按角色分配场景图，稳定不跳变 */
    private int sceneFor(String cardId) {
        if (cardId == null) {
            return 0;
        }
        switch (cardId) {
            case "builtin_alice":  return R.drawable.ef_rainy_night;
            case "builtin_rin":    return R.drawable.ef_campus;
            case "builtin_hakuyo": return R.drawable.ef_shrine_water;
            case "builtin_rocco":  return R.drawable.ef_ema_rack;
            case "builtin_elian":  return R.drawable.ef_campus;
            case "builtin_yueling": return R.drawable.ef_shrine_hero;
            default: return 0;
        }
    }

    private String relativeTime(long t) {
        if (t <= 0) {
            return "";
        }
        long diff = System.currentTimeMillis() - t;
        long min = diff / 60000;
        if (min < 1) {
            return "刚刚";
        }
        if (min < 60) {
            return min + " 分钟前";
        }
        long hour = min / 60;
        if (hour < 24) {
            return hour + " 小时前";
        }
        long day = hour / 24;
        if (day < 30) {
            return day + " 天前";
        }
        return new java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA).format(t);
    }
}