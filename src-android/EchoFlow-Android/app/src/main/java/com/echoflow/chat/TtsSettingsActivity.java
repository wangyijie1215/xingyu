package com.echoflow.chat;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 语音设置。
 *
 * 三块内容，按"用户最可能想改的顺序"排：
 *   1. 开关 —— 不想听声音的人第一件事就是关掉
 *   2. 渠道 —— 决定用什么引擎（本地 / OpenAI / 自定义 / 手机自带）
 *   3. 音色 —— 每个角色分别选，这是"她听起来像她"的关键
 */
public class TtsSettingsActivity extends AppCompatActivity {

    private LinearLayout body;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView root = new ScrollView(this);
        root.setBackgroundColor(0xFF0E0A18);
        // ScrollView 自己也要设底色，否则内容不满屏时下方会露出主题默认色
        root.setFillViewport(true);

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(0xFF0E0A18);
        body.setPadding(dp(18), dp(24), dp(18), dp(30));
        root.addView(body);

        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, body, false);
        body.addView(sb);
        new PhoneStatusBar(this, sb).bind();

        setContentView(root);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        body.removeAllViews();
        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, body, false);
        body.addView(sb);
        new PhoneStatusBar(this, sb).bind();

        title("语音", "让角色在通话里真的说话。\n说出来的内容会自动去掉括号里的动作描写。");

        final TtsProvider.Config cfg = TtsProvider.load(this);

        // ---------- 1. 开关 ----------
        body.addView(row("启用语音", cfg.enabled ? "开" : "关", v -> {
            cfg.enabled = !cfg.enabled;
            TtsProvider.save(this, cfg);
            build();
        }));

        // ---------- 2. 渠道 ----------
        section("合成渠道");
        body.addView(row("当前渠道", channelName(cfg.kind), v -> pickChannel(cfg)));
        body.addView(hint(channelHint(cfg.kind)));

        // ---------- 3. 音色 ----------
        section("角色音色");
        body.addView(hint("每个角色可以选不同音色。没选的会用按角色分配的默认值。"));

        java.util.List<CharacterCard> cards = PhoneStore.recentCards(this);
        if (cards == null || cards.isEmpty()) {
            body.addView(hint("还没有角色。去角色库创建一个。"));
        } else {
            for (CharacterCard c : cards) {
                String saved = TtsStore.voiceFor(this, c.id);
                String cur = (saved == null || saved.isEmpty())
                        ? TtsProvider.defaultVoiceFor(c.id, cfg.kind)
                        : saved;
                String label = TtsProvider.voiceLabel(cfg.kind, cur);
                if (saved == null || saved.isEmpty()) {
                    label = label + "（默认）";
                }
                body.addView(row(c.name, label, v -> pickVoice(c, cfg)));
            }
        }

        // ---------- 4. 语速音调 ----------
        section("语速与音调");
        if (TtsProvider.KIND_QWEN.equals(cfg.kind)) {
            body.addView(row("语速倍率", String.valueOf(cfg.speed), v -> editSpeed(cfg)));
        } else {
            body.addView(row("语速", cfg.rate, v -> editRate(cfg)));
            body.addView(row("音调", cfg.pitch, v -> editPitch(cfg)));
        }

        // ---------- 5. 连接设置 ----------
        section("连接");
        if (TtsProvider.KIND_QWEN.equals(cfg.kind)) {
            boolean hasKey = SecureStore.get(this) != null && !SecureStore.get(this).isEmpty();
            body.addView(row("API Key", hasKey ? "已配置" : "未配置（去设置页填）", v -> {
                Toast.makeText(this, hasKey ? "已在设置页配置" : "请到「设置」里填 API Key",
                        Toast.LENGTH_SHORT).show();
            }));
            body.addView(row("试听一句", "用当前音色", v -> {
                new TtsSpeaker(this).speak("今天又下雨了，你那边呢？", cfg.voice, null);
            }));
        } else if (TtsProvider.KIND_LOCAL.equals(cfg.kind)) {
            body.addView(row("服务地址", ProviderStore.cleanHost(cfg.localHost), v -> editHost(cfg)));
            body.addView(row("检测连接", "点一下试试", v -> probe(cfg)));
        } else if (TtsProvider.KIND_OPENAI.equals(cfg.kind)) {
            body.addView(row("接口地址", cfg.openaiUrl, v -> editOpenAiUrl(cfg)));
            body.addView(hint("API Key 用设置页里那一个。"));
        } else if (TtsProvider.KIND_CUSTOM.equals(cfg.kind)) {
            body.addView(row("端点模板", cfg.customUrl.isEmpty() ? "（未填）" : cfg.customUrl,
                    v -> editCustom(cfg)));
            body.addView(hint("可用占位符：{text} {voice}"));
        }

        statusText = new TextView(this);
        statusText.setTextSize(12);
        statusText.setLineSpacing(dp(4), 1f);
        statusText.setPadding(dp(14), dp(12), dp(14), dp(12));
        statusText.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(12)));
        statusText.setText("缓存：" + TtsSpeaker.cacheSizeKB(this) + " KB");
        statusText.setTextColor(0xFF8B84A8);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(14);
        statusText.setLayoutParams(slp);
        body.addView(statusText);

        body.addView(row("清空语音缓存", TtsSpeaker.cacheSizeKB(this) + " KB", v -> {
            int n = TtsSpeaker.clearCache(this);
            Toast.makeText(this, "已清除 " + n + " 段语音", Toast.LENGTH_SHORT).show();
            build();
        }));
    }

    // ==================================================================

    private String channelName(String kind) {
        switch (kind) {
            case TtsProvider.KIND_LOCAL: return "本地 TTS 服务";
            case TtsProvider.KIND_QWEN: return "阿里云百炼 CosyVoice";
            case TtsProvider.KIND_OPENAI: return "OpenAI 兼容接口";
            case TtsProvider.KIND_CUSTOM: return "自定义端点";
            default: return "手机自带 TTS";
        }
    }

    private String channelHint(String kind) {
        switch (kind) {
            case TtsProvider.KIND_LOCAL:
                return "电脑上运行 python tools/tts-server.py，音质最好，322 个音色。\n"
                        + "模拟器填 10.0.2.2:5001，真机填电脑局域网 IP。";
            case TtsProvider.KIND_QWEN:
                return "国内直连，不需要代理。2 元/万字符，音质比本地 edge-tts 好一档。\n"
                        + "用设置页里那个 API Key。";
            case TtsProvider.KIND_OPENAI:
                return "任何兼容 /v1/audio/speech 的服务都行，音色用 nova / shimmer 等。";
            case TtsProvider.KIND_CUSTOM:
                return "给以后留的口子：任何「给文本返回音频」的 HTTP 服务。";
            default:
                return "离线可用，但中文只有机械音，而且所有角色都是同一个声音。\n"
                        + "作为兜底存在。";
        }
    }

    private void editSpeed(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(String.valueOf(cfg.speed));
        in.setHint("1.0");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("语速倍率")
                .setMessage("0.5 ~ 2.0。1.0 是正常，0.9 稍慢（更沉稳）。")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    try {
                        cfg.speed = Float.parseFloat(in.getText().toString().trim());
                    } catch (Exception e) {
                        cfg.speed = 1.0f;
                    }
                    if (cfg.speed < 0.5f) cfg.speed = 0.5f;
                    if (cfg.speed > 2.0f) cfg.speed = 2.0f;
                    TtsProvider.save(this, cfg);
                    build();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void pickChannel(TtsProvider.Config cfg) {
        String[] items = {
                "阿里云百炼 CosyVoice（推荐，音质好）",
                "本地 TTS 服务（免费，需开电脑）",
                "OpenAI 兼容接口",
                "自定义端点",
                "手机自带 TTS（离线兜底）"
        };
        new AlertDialog.Builder(this)
                .setTitle("合成渠道")
                .setItems(items, (d, w) -> {
                    switch (w) {
                        case 0: cfg.kind = TtsProvider.KIND_QWEN; break;
                        case 1: cfg.kind = TtsProvider.KIND_LOCAL; break;
                        case 2: cfg.kind = TtsProvider.KIND_OPENAI; break;
                        case 3: cfg.kind = TtsProvider.KIND_CUSTOM; break;
                        default: cfg.kind = TtsProvider.KIND_SYSTEM; break;
                    }
                    // 换渠道后音色表变了，重置成该渠道的默认音色
                    java.util.List<TtsProvider.Voice> vs = TtsProvider.voicesFor(cfg.kind);
                    if (!vs.isEmpty()) {
                        cfg.voice = vs.get(0).id;
                    }
                    TtsProvider.save(this, cfg);
                    build();
                })
                .show();
    }

    private void pickVoice(final CharacterCard card, final TtsProvider.Config cfg) {
        final java.util.List<TtsProvider.Voice> vs = TtsProvider.voicesFor(cfg.kind);
        final String[] labels = new String[vs.size() + 1];
        labels[0] = "跟随默认";
        for (int i = 0; i < vs.size(); i++) {
            TtsProvider.Voice v = vs.get(i);
            labels[i + 1] = v.label + " —— " + v.hint;
        }
        new AlertDialog.Builder(this)
                .setTitle(card.name + " 的音色")
                .setItems(labels, (d, w) -> {
                    if (w == 0) {
                        TtsStore.setVoice(this, card.id, null);
                    } else {
                        TtsStore.setVoice(this, card.id, vs.get(w - 1).id);
                    }
                    // 立刻试听一句，选音色最需要"听到"
                    new TtsSpeaker(this).speak("嗯，是我。", TtsProvider.voiceLabel(
                            cfg.kind, w == 0 ? TtsProvider.defaultVoiceFor(card.id, cfg.kind)
                                    : vs.get(w - 1).id), null);
                    build();
                })
                .show();
    }

    private void editRate(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.rate);
        in.setHint("+0%");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("语速")
                .setMessage("格式如 +0% / -10% / +20%\n负数更慢，听起来更沉稳。")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.rate = in.getText().toString().trim();
                    if (cfg.rate.isEmpty()) {
                        cfg.rate = "+0%";
                    }
                    TtsProvider.save(this, cfg);
                    build();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void editPitch(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.pitch);
        in.setHint("+0Hz");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("音调")
                .setMessage("格式如 +0Hz / -10Hz / +20Hz\n负数更低沉。")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.pitch = in.getText().toString().trim();
                    if (cfg.pitch.isEmpty()) {
                        cfg.pitch = "+0Hz";
                    }
                    TtsProvider.save(this, cfg);
                    build();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void editHost(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.localHost);
        in.setHint("192.168.1.100:5001");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("TTS 服务地址")
                .setMessage("模拟器：10.0.2.2:5001\n真机：电脑的局域网 IP")
                .setView(in)
                .setPositiveButton("保存并检测", (d, w) -> {
                    cfg.localHost = in.getText().toString().trim();
                    TtsProvider.save(this, cfg);
                    build();
                    probe(cfg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void editOpenAiUrl(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.openaiUrl);
        in.setHint("https://api.openai.com");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("接口地址")
                .setMessage("会自动拼上 /v1/audio/speech")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.openaiUrl = in.getText().toString().trim();
                    TtsProvider.save(this, cfg);
                    build();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void editCustom(TtsProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.customUrl);
        in.setHint("https://your-tts/say?t={text}&v={voice}");
        in.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("自定义端点")
                .setMessage("占位符：{text} {voice}")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.customUrl = in.getText().toString().trim();
                    TtsProvider.save(this, cfg);
                    build();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void probe(final TtsProvider.Config cfg) {
        if (statusText == null) {
            return;
        }
        statusText.setText("正在检测…");
        statusText.setTextColor(0xFFE8C87A);
        new Thread(() -> {
            final String msg = TtsProvider.probeLocal(cfg.localHost);
            runOnUiThread(() -> {
                statusText.setText(msg);
                if (msg.startsWith("✓")) {
                    statusText.setTextColor(0xFF7FE0C4);
                    // 探测成功后试听
                    new TtsSpeaker(this).speak("喂，能听到吗？", cfg.voice, null);
                } else {
                    statusText.setTextColor(0xFFFF9BB0);
                }
            });
        }).start();
    }

    // ==================================================================
    // 小部件
    // ==================================================================

    private void title(String t, String sub) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(22);
        tv.setTextColor(0xFFF4F2FF);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dp(16), 0, dp(6));
        body.addView(tv);

        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextSize(13);
        s.setTextColor(0xFF8B84A8);
        s.setLineSpacing(dp(4), 1f);
        s.setPadding(0, 0, 0, dp(18));
        body.addView(s);
    }

    private void section(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(12);
        tv.setTextColor(0xFF8B84A8);
        tv.setPadding(dp(2), dp(20), 0, dp(8));
        body.addView(tv);
    }

    private TextView hint(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(12);
        tv.setTextColor(0xFF6F6A8B);
        tv.setLineSpacing(dp(4), 1f);
        tv.setPadding(dp(4), dp(4), dp(4), dp(10));
        return tv;
    }

    private View row(String label, String value, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        r.setLayoutParams(lp);
        r.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(12)));
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        r.setOnClickListener(click);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(0xFFF4F2FF);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(l);

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13);
        v.setTextColor(0xFF8B84A8);
        v.setMaxLines(1);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        r.addView(v);

        TextView arrow = new TextView(this);
        arrow.setText("  ›");
        arrow.setTextSize(14);
        arrow.setTextColor(0xFF5A5478);
        r.addView(arrow);
        return r;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
