package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 通话界面 —— 新功能。
 *
 * 诚实说明：这里**没有真正的实时语音流**（那需要 STT+TTS+音频通道，
 * 是独立的大工程）。当前实现的是「**通话式文本对话**"：
 *
 *   · 界面是真实的通话界面（呼叫中 → 接通 → 计时 → 挂断）
 *   · 她的每一句话以「字幕"形式出现，带打字感
 *   · 通话结束后把这段对话**写入手机聊天记录**，并跑状态分析
 *
 * 也就是说：体验上是通话，数据上是聊天。
 * 这样在没有 TTS/STT 的情况下也能用，而且接上 TTS 后只需把字幕换成语音播放。
 */
public class VoiceCallActivity extends AppCompatActivity {

    private CharacterCard card;
    private Persona persona;
    private RelationshipState rel;
    private EmotionState emo;

    private TextView status;
    private TextView timer;
    private TextView caption;
    private TextView nowText;
    private TextView transcriptBtn;

    private final List<Message> turns = new ArrayList<>();
    private int seconds = 0;
    private boolean connected = false;
    private boolean muted = false;
    private boolean speaker = true;
    private boolean waiting = false;

    /** TTS：让她真的出声（注意别和上面的"扬声器"布尔搞混） */
    private TtsSpeaker tts;
    /** 正在播语音（用于界面提示和防打断） */
    private boolean talking = false;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable ticker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        card = CardStore.getCard(this, getIntent().getStringExtra("card_id"));
        if (card == null) {
            Toast.makeText(this, "角色不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tts = new TtsSpeaker(this);
        List<Persona> personas = CardStore.listPersonas(this);
        persona = (personas != null && !personas.isEmpty()) ? personas.get(0) : new Persona();
        rel = CharacterStateStore.loadRelationship(this, card.id);
        emo = CharacterStateStore.loadEmotion(this, card.id);

        status = findViewById(R.id.vc_status);
        timer = findViewById(R.id.vc_timer);
        caption = findViewById(R.id.vc_caption);
        nowText = findViewById(R.id.vc_now);
        transcriptBtn = findViewById(R.id.vc_transcript);

        ((TextView) findViewById(R.id.vc_name)).setText(card.name);
        ImageView avatar = findViewById(R.id.vc_avatar);
        Bitmap bmp = CardStore.loadAvatar(this, card.id);
        if (bmp != null) {
            avatar.setImageBitmap(bmp);
        }
        ImageView bg = findViewById(R.id.vc_bg);
        bg.setImageResource(R.drawable.ef_shrine_hero);

        Schedule.Now now = Schedule.now(Schedule.load(this, card.id));
        if (now.what != null && !now.what.isEmpty()) {
            nowText.setText(now.busy
                    ? "她正在「" + now.what + "」"
                    : "此刻有空 · " + now.what);
        }

        findViewById(R.id.vc_hangup).setOnClickListener(v -> hangUp());
        findViewById(R.id.vc_mute).setOnClickListener(v -> {
            muted = !muted;
            Toast.makeText(this, muted ? "已静音" : "取消静音", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.vc_speaker).setOnClickListener(v -> {
            speaker = !speaker;
            Toast.makeText(this, speaker ? "免提开" : "免提关", Toast.LENGTH_SHORT).show();
        });
        transcriptBtn.setOnClickListener(v -> openTextChat());

        connect();
    }

    // ==================================================================

    private void connect() {
        status.setText("正在呼叫…");
        ui.postDelayed(() -> {
            if (isFinishing()) {
                return;
            }
            connected = true;
            status.setText("通话中 · 转录已开启");
            startTimer();
            // 她先开口
            speak(firstLine());
        }, 1800);
    }

    /** 接通后她说的第一句（按日程决定忙不忙） */
    private String firstLine() {
        Schedule.Now now = Schedule.now(Schedule.load(this, card.id));
        if (now.busy) {
            return "喂？……我现在在" + now.what + "，你说，我听着。";
        }
        return "喂，是我。";
    }

    private void startTimer() {
        ticker = new Runnable() {
            @Override
            public void run() {
                if (!connected) {
                    return;
                }
                seconds++;
                timer.setText(String.format(java.util.Locale.CHINA, "%02d:%02d",
                        seconds / 60, seconds % 60));
                ui.postDelayed(this, 1000);
            }
        };
        ui.postDelayed(ticker, 1000);
    }

    /**
     * 播出她的话：**字幕 + 语音**同时进行。
     *
     * 之前这里只有打字机字幕，没有声音 —— 所以"打电话"其实是看字。
     * 现在两条线并行：
     *   · 字幕按字数渐显（保留打字感）
     *   · 音频同时开始播
     * 整句说完（以**音频为准**）才解锁输入，因为听比读慢。
     *
     * 音频失败时不会卡住：TtsSpeaker 保证回调一定被调用。
     */
    private void speak(String text) {
        caption.setVisibility(View.VISIBLE);
        caption.setText("");
        transcriptBtn.setText("正在通话…");

        // ---- 字幕线 ----
        final int[] i = {0};
        ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || i[0] > text.length()) {
                    return;
                }
                caption.setText(text.substring(0, Math.min(i[0], text.length())));
                if (i[0] < text.length()) {
                    i[0]++;
                    ui.postDelayed(this, 45);
                }
            }
        }, 300);

        // ---- 音频线 ----
        if (!TtsProvider.load(this).enabled) {
            // TTS 关掉时只显示字幕，说完就解锁
            ui.postDelayed(() -> finishTurn(text), 300 + text.length() * 45L);
            return;
        }

        tts.speak(text, voiceForCard(), new TtsSpeaker.Callback() {
            @Override
            public void onStart() {
                runOnUiThread(() -> talking = true);
            }

            @Override
            public void onDone() {
                runOnUiThread(() -> {
                    talking = false;
                    // 字幕可能还没打完，补齐，避免出现"话说完了字还没显示全"
                    caption.setText(text);
                    finishTurn(text);
                });
            }
        });
    }

    /** 一轮结束：记进上下文、解锁输入 */
    private void finishTurn(String text) {
        if (isFinishing()) {
            return;
        }
        if (turns.isEmpty() || !turns.get(turns.size() - 1).content.equals(text)) {
            turns.add(new Message("assistant", text));
        }
        waiting = false;
        transcriptBtn.setText("按住说话（点这里输入）");
    }

    /** 这个角色的音色 —— 角色卡上没存就用按 id 分配的默认值 */
    private String voiceForCard() {
        if (card == null) {
            return null;
        }
        String saved = TtsStore.voiceFor(this, card.id);
        if (saved != null && !saved.isEmpty()) {
            return saved;
        }
        TtsProvider.Config cfg = TtsProvider.load(this);
        return TtsProvider.defaultVoiceFor(card.id, cfg.kind);
    }

    /** 用户「说"了一句（点按钮输入） */
    private void openTextChat() {
        if (waiting) {
            Toast.makeText(this, "她还在说…", Toast.LENGTH_SHORT).show();
            return;
        }
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("说点什么");
        input.setTextColor(0xFF1A1A1A);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("接通中 · 说点什么")
                .setView(input)
                .setPositiveButton("说", (d, w) -> {
                    String t = input.getText().toString().trim();
                    if (!t.isEmpty()) {
                        turns.add(new Message("user", t));
                        replyTo(t);
                    }
                })
                .setNegativeButton("不说", null)
                .show();
    }

    private void replyTo(String userSaid) {
        String apiKey = SecureStore.get(this);
        boolean local = ProviderStore.isOllama(this);
        if (!local && (apiKey == null || apiKey.isEmpty())) {
            speak("（还没配置模型，接不上。）");
            return;
        }
        waiting = true;
        status.setText("她正在说…");

        // 通话模式的 prompt：口语化、更短、更即时
        List<Message> history = new ArrayList<>(turns);
        StringBuilder sys = new StringBuilder();
        sys.append("你现在是 ").append(card.name).append("，正在和对方打电话。");
        if (card.personality != null && !card.personality.isEmpty()) {
            sys.append("性格：").append(card.personality);
        }
        sys.append("\n【你们的关系】").append(rel.levelName())
           .append("，亲密度 ").append(rel.points).append("/100");
        if (emo != null && emo.mood != null) {
            sys.append("\n【你当前的情绪】").append(emo.mood);
        }
        String sched = Schedule.promptLine(Schedule.load(this, card.id));
        if (!sched.isEmpty()) {
            sys.append("\n【你此刻的日程】").append(sched);
        }
        sys.append("\n\n【通话要求】");
        sys.append("\n1. 这是**打电话**，不是发消息。说话要口语、自然。");
        sys.append("\n2. 不要写动作、神态、旁白，不要用括号。");
        sys.append("\n3. 简短。通常一两句，说完就停，等对方回应。");
        sys.append("\n4. 可以有语气词、停顿、口头禅。");
        sys.append("\n5. 直接输出你要说的话。");

        List<Message> msgs = new ArrayList<>();
        msgs.add(new Message("system", sys.toString()));
        int start = Math.max(0, history.size() - 8);
        for (int i = start; i < history.size(); i++) {
            msgs.add(history.get(i));
        }

        ApiClient.streamChat(ProviderStore.effectiveBaseUrl(this),
                apiKey == null ? "" : apiKey,
                ProviderStore.effectiveModel(this),
                msgs, local, new ApiClient.Callback() {
                    private String buf = "";

                    @Override
                    public void onChunk(String fullText) {
                        buf = fullText;
                    }

                    @Override
                    public void onDone(boolean error, String errMsg) {
                        ui.post(() -> {
                            status.setText("通话中 · 转录已开启");
                            if (error || buf == null || buf.trim().isEmpty()) {
                                speak("……（信号不太好）");
                            } else {
                                // 通话里绝不该出现括号动作，兜底剥一层
                                speak(buf.trim().replaceAll("（[^）]*）", "")
                                        .replaceAll("\\([^)]*\\)", "").trim());
                            }
                        });
                    }
                });
    }

    private void hangUp() {
        connected = false;
        // 立刻掐掉正在播的话
        if (tts != null) {
            tts.stop();
            talking = false;
        }
        if (ticker != null) {
            ui.removeCallbacks(ticker);
        }
        if (!turns.isEmpty()) {
            // 通话记录写进手机聊天，并跑状态分析
            List<Message> existing = PhoneStore.list(this, card.id);
            existing.addAll(turns);
            PhoneStore.save(this, card.id, existing);
            StateAnalyzer.analyze(this, card, persona,
                    ProviderStore.effectiveBaseUrl(this),
                    SecureStore.get(this) == null ? "" : SecureStore.get(this),
                    ProviderStore.effectiveModel(this), turns);
            Toast.makeText(this, "通话结束 · 已保存到聊天记录", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connected = false;
        if (ticker != null) {
            ui.removeCallbacks(ticker);
        }
        ui.removeCallbacksAndMessages(null);
        // 必须停掉语音：否则退出通话页后声音还在放
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 切到后台就停下正在播的那句 —— 打电话时切走却还有声音很奇怪
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            talking = false;
        }
    }
}