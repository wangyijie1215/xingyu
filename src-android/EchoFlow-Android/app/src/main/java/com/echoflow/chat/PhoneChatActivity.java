package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 手机模式 · 聊天页（微信风）。
 *
 * 与主聊天（CardChatActivity）的关系：
 *  · 独立的聊天历史（PhoneStore），但**共用角色卡 / 关系 / 情绪 / 记忆**
 *  · Prompt 走 PhonePrompt：只说话，不要动作旁白
 *  · 回复结束后同样跑 StateAnalyzer，所以这边聊的内容，主聊天里她也记得
 *
 * 界面刻意做成微信的样子：浅灰底、白气泡 / 绿气泡、底部输入栏。
 */
public class PhoneChatActivity extends AppCompatActivity {

    private CharacterCard card;
    private Persona persona;
    private RelationshipState rel;
    private EmotionState emo;
    private final List<Message> messages = new ArrayList<>();
    private PhoneMessageAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private TextView send;
    private boolean sending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_chat);

        card = CardStore.getCard(this, getIntent().getStringExtra("card_id"));
        if (card == null) {
            Toast.makeText(this, "角色不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        List<Persona> personas = CardStore.listPersonas(this);
        persona = (personas != null && !personas.isEmpty()) ? personas.get(0) : new Persona();

        rel = CharacterStateStore.loadRelationship(this, card.id);
        emo = CharacterStateStore.loadEmotion(this, card.id);

        // 顶部
        TextView title = findViewById(R.id.wx_title);
        title.setText(card.name);
        findViewById(R.id.wx_back).setOnClickListener(v -> finish());
        findViewById(R.id.wx_more).setOnClickListener(v -> {
            // 弹一个明确的菜单，而不是直接跳走 ——
            // 原来点 ··· 会静默切到角色扮演模式，用户不知道发生了什么。
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(card.name)
                    .setItems(new String[]{
                            "角色扮演模式（有动作、神态描写）",
                            "清空这次对话",
                            "查看角色资料"
                    }, (d, w) -> {
                        switch (w) {
                            case 0: {
                                Intent i = new Intent(this, CardChatActivity.class);
                                i.putExtra("card_id", card.id);
                                startActivity(i);
                                finish();
                                break;
                            }
                            case 1:
                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                        .setTitle("清空对话？")
                                        .setMessage("这里的历史会被删除，角色卡和记忆不受影响。")
                                        .setPositiveButton("清空", (dd, ww) -> {
                                            PhoneStore.clear(this, card.id);
                                            messages.clear();
                                            adapter.notifyDataSetChanged();
                                            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("取消", null)
                                        .show();
                                break;
                            default: {
                                Intent i = new Intent(this, CharacterProfileActivity.class);
                                i.putExtra("card_id", card.id);
                                startActivity(i);
                                break;
                            }
                        }
                    })
                    .show();
        });

        // 状态栏（真实时间 / 电量）
        new PhoneStatusBar(this, findViewById(android.R.id.content)).bind();

        // 顶部标题右侧：显示她此刻在做什么
        Schedule.Now now = Schedule.now(Schedule.load(this, card.id));
        if (now.what != null && !now.what.isEmpty()) {
            TextView sub = findViewById(R.id.wx_subtitle);
            if (sub != null) {
                sub.setText(now.busy ? "此刻：" + now.what : "此刻有空 · " + now.what);
                sub.setTextColor(now.busy ? 0xFF999999 : 0xFF07C160);
            }
        }

        // 消息列表
        list = findViewById(R.id.wx_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        Bitmap avatar = CardStore.loadAvatar(this, card.id);
        adapter = new PhoneMessageAdapter(messages, avatar);
        list.setAdapter(adapter);

        // 输入
        input = findViewById(R.id.wx_input);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        send = findViewById(R.id.wx_send);
        send.setOnClickListener(v -> sendMessage());
        findViewById(R.id.wx_voice).setOnClickListener(v ->
                Toast.makeText(this, "按住说话（语音输入待接入）", Toast.LENGTH_SHORT).show());

        // 载入独立历史；首次进入给一条开场白
        messages.addAll(PhoneStore.list(this, card.id));
        if (messages.isEmpty()) {
            String greeting = PhonePromptGreeting.first(card, persona);
            if (greeting != null && !greeting.isEmpty()) {
                messages.add(new Message("assistant", greeting));
                PhoneStore.save(this, card.id, messages);
            }
        }
        adapter.notifyDataSetChanged();
        scrollToBottom(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (card != null) {
            rel = CharacterStateStore.loadRelationship(this, card.id);
            emo = CharacterStateStore.loadEmotion(this, card.id);
            // 进入会话即视为已读 —— 回到列表角标就消了
            ReadCursor.markRead(this, card.id);
        }
    }

    // ==================================================================

    private String baseUrl() {
        return ProviderStore.effectiveBaseUrl(this);
    }

    private String model() {
        return ProviderStore.effectiveModel(this);
    }

    private boolean isLocal() {
        return ProviderStore.isOllama(this);
    }

    private String apiKey() {
        String k = SecureStore.get(this);
        return k == null ? "" : k;
    }

    private void sendMessage() {
        if (sending) {
            return;
        }
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        if (!isLocal() && apiKey().isEmpty()) {
            Toast.makeText(this, "请先到设置页填写 API 密钥（本地 Ollama 不需要）",
                    Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        Message userMsg = new Message("user", text);
        Message aiMsg = new Message("assistant", "");
        messages.add(userMsg);
        messages.add(aiMsg);
        input.setText("");
        adapter.notifyItemRangeInserted(messages.size() - 2, 2);
        scrollToBottom(true);

        sending = true;
        send.setEnabled(false);
        adapter.setGenerating(aiMsg);
        adapter.notifyItemChanged(messages.size() - 1);

        // 组装：手机模式 Prompt（只说话，不要动作）
        List<Message> history = new ArrayList<>();
        for (Message m : messages) {
            if (m == aiMsg) {
                continue;
            }
            history.add(m);
        }
        List<Memory> memories = MemoryStore.topForPrompt(this, card.id, 15);
        Fortune fortune = FortuneService.load(this, card.id, Fortune.today());
        PromptBuilder.LifeContext life = buildLifeContext();
        // 日程：让她知道自己此刻在做什么（忙的时候回复会更短）
        String scheduleLine = Schedule.promptLine(Schedule.load(this, card.id));

        // 世界书：按最近对话的关键词触发，注入「她所在的世界"的设定
        String world = Lorebook.buildPrompt(this, card.id, history);

        List<Message> ctx = PhonePrompt.build(card, persona, history, memories,
                rel, emo, fortune, life, scheduleLine, world);

        ApiClient.streamChat(baseUrl(), apiKey(), model(), ctx, isLocal(),
                new ApiClient.Callback() {
                    @Override
                    public void onChunk(String fullText) {
                        runOnUiThread(() -> {
                            aiMsg.content = fullText;
                            adapter.notifyItemChanged(messages.indexOf(aiMsg));
                            scrollToBottom(true);
                        });
                    }

                    @Override
                    public void onDone(boolean error, String errMsg) {
                        runOnUiThread(() -> {
                            sending = false;
                            send.setEnabled(true);
                            adapter.setGenerating(null);
                            if (error && errMsg != null && !errMsg.isEmpty()) {
                                Toast.makeText(PhoneChatActivity.this, errMsg,
                                        Toast.LENGTH_LONG).show();
                            }
                            // 失败且一字未出 → 移除空壳
                            if (error && (aiMsg.content == null || aiMsg.content.trim().isEmpty())) {
                                int idx = messages.indexOf(aiMsg);
                                if (idx >= 0) {
                                    messages.remove(idx);
                                    adapter.notifyItemRemoved(idx);
                                }
                            }
                            PhoneStore.save(PhoneChatActivity.this, card.id, messages);
                            // 与主聊天共用状态分析：记忆 / 关系 / 情绪 / 事件
                            if (!error) {
                                List<Message> recent = new ArrayList<>();
                                int start = Math.max(0, messages.size() - 6);
                                for (int i = start; i < messages.size(); i++) {
                                    Message m = messages.get(i);
                                    if (m.content != null && !m.content.isEmpty()) {
                                        recent.add(m);
                                    }
                                }
                                StateAnalyzer.analyze(PhoneChatActivity.this, card, persona,
                                        baseUrl(), apiKey(), model(), recent);
                            }
                        });
                    }
                });
    }

    /** 与主聊天一致的生活侧写边界（日记只说没说出口的那句、胶囊只给存在事实） */
    private PromptBuilder.LifeContext buildLifeContext() {
        PromptBuilder.LifeContext life = new PromptBuilder.LifeContext();
        List<DiaryEntry> diaries = DiaryStore.list(this, card.id);
        String today = Fortune.today();
        for (DiaryEntry d : diaries) {
            if (d.date.equals(today)) {
                continue;
            }
            if (d.unsaid != null && !d.unsaid.trim().isEmpty()) {
                life.lastDiaryUnsaid = d.unsaid.trim();
                break;
            }
        }
        for (Letter l : LetterStore.list(this, card.id)) {
            if (l.fromHer()) {
                life.lastLetterHint = l.dateLabel + "，你在信里写过：「"
                        + LetterService.excerptOf(l.body, 40) + "」";
                break;
            }
        }
        life.sealedCapsuleTeasers = CapsuleStore.sealedTeasers(this, card.id);
        return life;
    }

    private void scrollToBottom(boolean smooth) {
        if (messages.isEmpty()) {
            return;
        }
        list.post(() -> {
            int last = adapter.getItemCount() - 1;
            if (last >= 0) {
                if (smooth) {
                    list.smoothScrollToPosition(last);
                } else {
                    list.scrollToPosition(last);
                }
            }
        });
    }
}