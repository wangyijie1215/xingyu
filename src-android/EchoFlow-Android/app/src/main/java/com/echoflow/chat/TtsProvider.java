package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * TTS 渠道抽象 —— 让「打电话」真的出声。
 *
 * 四个渠道，优先级从高到低，都带音色选择：
 *
 *   1. **本地 TTS 服务**（local）
 *      电脑上跑一个 edge-tts 服务（`tools/tts-server.py`）。
 *      音质最好 —— 322 个神经音色，中文有晓晓（温柔）和晓伊（活泼），
 *      日文有 Nanami（更贴二次元角色）。每个角色能分到不同声音。
 *
 *   2. **OpenAI 兼容 TTS**（openai）
 *      `/v1/audio/speech`，音色用 alloy/echo/fable/nova/onyx/shimmer。
 *      很多中转也支持，填 baseUrl + key + voice 即可。
 *
 *   3. **自定义 HTTP 端点**（custom）
 *      任何「POST 文本返回音频」的服务。给以后留的口子。
 *
 *   4. **手机自带 TTS**（system）
 *      零依赖，离线可用。缺点是中文只有「辉辉」这种机械音，
 *      而且**所有角色都会是同一个声音**。作为兜底存在。
 *
 * 为什么不做成"自动选最好的"：可用性是环境性的（电脑开没开、有没有网），
 * 程序猜不准。让用户明确选，比猜错了好。
 */
public class TtsProvider {

    /** 本地 edge-tts 服务：免费，322 音色 */
    public static final String KIND_LOCAL = "local";
    /** 阿里云百炼 CosyVoice：国内直连，2 元/万字符，音质明显更好 */
    public static final String KIND_QWEN = "qwen";
    public static final String KIND_OPENAI = "openai";
    public static final String KIND_CUSTOM = "custom";
    public static final String KIND_SYSTEM = "system";

    private static final String PREFS = "echoflow_tts";

    /** 百炼的语音合成端点（实测：返回一个 24 小时有效的音频 URL） */
    public static final String DASHSCOPE_TTS_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer";

    public static class Config {
        public String kind = KIND_LOCAL;
        /** 本地 TTS 服务地址，例如 192.168.1.100:5001 */
        public String localHost = "10.0.2.2:5001";
        /** 当前音色（channel 相关，切换渠道时会被重置为默认） */
        public String voice = "zh-CN-XiaoxiaoNeural";
        /** 语速，例如 -10% / +0% / +20%（本地渠道用） */
        public String rate = "+0%";
        /** 音调，例如 +0Hz / -5Hz（本地渠道用） */
        public String pitch = "+0Hz";
        /** CosyVoice 的语速倍率 0.5~2.0 */
        public float speed = 1.0f;
        /** OpenAI 兼容端点的 baseUrl 与 key */
        public String openaiUrl = "https://api.openai.com";
        /** 自定义端点模板，含 {text} {voice} */
        public String customUrl = "";
        /** 是否启用（关闭时通话不发声，只显示字幕） */
        public boolean enabled = true;
    }

    public static Config load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Config c = new Config();
        c.kind = p.getString("kind", KIND_LOCAL);
        c.localHost = p.getString("localHost", "10.0.2.2:5001");
        c.voice = p.getString("voice", "zh-CN-XiaoxiaoNeural");
        c.rate = p.getString("rate", "+0%");
        c.pitch = p.getString("pitch", "+0Hz");
        c.openaiUrl = p.getString("openaiUrl", "https://api.openai.com");
        c.customUrl = p.getString("customUrl", "");
        c.speed = p.getFloat("speed", 1.0f);
        c.enabled = p.getBoolean("enabled", true);
        return c;
    }

    public static void save(Context ctx, Config c) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("kind", c.kind)
                .putString("localHost", c.localHost)
                .putString("voice", c.voice)
                .putString("rate", c.rate)
                .putString("pitch", c.pitch)
                .putString("openaiUrl", c.openaiUrl)
                .putString("customUrl", c.customUrl)
                .putFloat("speed", c.speed)
                .putBoolean("enabled", c.enabled)
                .apply();
    }

    // ==================================================================
    // 音色表
    // ==================================================================

    public static class Voice {
        public String id;
        public String label;
        /** 适合什么样的角色 */
        public String hint;
        /** 属于哪个渠道 */
        public String kind;

        public Voice(String kind, String id, String label, String hint) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.hint = hint;
        }
    }

    /** 内置音色表。edge-tts 的部分来自 `edge-tts --list-voices` 实测。 */
    public static java.util.List<Voice> voicesFor(String kind) {
        java.util.List<Voice> out = new java.util.ArrayList<>();
        switch (kind) {
            case KIND_QWEN:
                // 全部实测可用（10 个）。标注按试听感受写的，供选型参考。
                out.add(new Voice(KIND_QWEN, "longxiaochun_v2",
                        "龙小淳 · 知性", "温和、知性，最稳的默认音"));
                out.add(new Voice(KIND_QWEN, "longxiaoxia_v2",
                        "龙小夏 · 沉稳", "低沉一些，适合成熟角色"));
                out.add(new Voice(KIND_QWEN, "longwan_v2",
                        "龙婉 · 温柔", "软，适合治愈系"));
                out.add(new Voice(KIND_QWEN, "longmiao_v2",
                        "龙嫇 · 少女", "年轻，适合少女角色"));
                out.add(new Voice(KIND_QWEN, "longyue_v2",
                        "龙悦 · 活泼", "明快，话多的角色"));
                out.add(new Voice(KIND_QWEN, "loongbella_v2",
                        "Bella · 元气", "元气感强"));
                out.add(new Voice(KIND_QWEN, "longjing_v2",
                        "龙婧 · 清冷", "冷静，适合疏离型角色"));
                out.add(new Voice(KIND_QWEN, "longcheng_v2",
                        "龙橙 · 男声", "青年男声"));
                out.add(new Voice(KIND_QWEN, "longhua_v2",
                        "龙华 · 男声", "少年男声"));
                out.add(new Voice(KIND_QWEN, "longshu_v2",
                        "龙书 · 男声", "沉稳男声"));
                break;
            case KIND_LOCAL:
                // 中文只有两个女声，所以把日文的 Nanami 也算进来 ——
                // 二次元角色用日语音色反而更贴，何况很多角色台词短
                out.add(new Voice(KIND_LOCAL, "zh-CN-XiaoxiaoNeural",
                        "晓晓 · 温柔", "成熟、安静、治愈系"));
                out.add(new Voice(KIND_LOCAL, "zh-CN-XiaoyiNeural",
                        "晓伊 · 活泼", "年轻、话多、有元气"));
                out.add(new Voice(KIND_LOCAL, "zh-CN-YunxiNeural",
                        "云希 · 青年男", "少年角色"));
                out.add(new Voice(KIND_LOCAL, "zh-CN-YunxiaNeural",
                        "云夏 · 少年", "更年轻的男声"));
                out.add(new Voice(KIND_LOCAL, "ja-JP-NanamiNeural",
                        "七海 · 日文女", "二次元感最强，适合少女角色"));
                out.add(new Voice(KIND_LOCAL, "ja-JP-KeitaNeural",
                        "圭太 · 日文男", "日系少年"));
                out.add(new Voice(KIND_LOCAL, "zh-CN-liaoning-XiaobeiNeural",
                        "小北 · 东北话", "有个性的角色"));
                out.add(new Voice(KIND_LOCAL, "zh-CN-shaanxi-XiaoniNeural",
                        "小妮 · 陕西话", "方言角色"));
                break;
            case KIND_OPENAI:
                out.add(new Voice(KIND_OPENAI, "nova", "Nova", "明亮、年轻，最接近少女音"));
                out.add(new Voice(KIND_OPENAI, "shimmer", "Shimmer", "柔和、温暖"));
                out.add(new Voice(KIND_OPENAI, "alloy", "Alloy", "中性、平稳"));
                out.add(new Voice(KIND_OPENAI, "echo", "Echo", "偏低沉"));
                out.add(new Voice(KIND_OPENAI, "fable", "Fable", "叙述感强"));
                out.add(new Voice(KIND_OPENAI, "onyx", "Onyx", "低沉男声"));
                break;
            case KIND_CUSTOM:
                out.add(new Voice(KIND_CUSTOM, "default", "默认音色", "由你的服务决定"));
                break;
            default:
                out.add(new Voice(KIND_SYSTEM, "", "系统默认", "手机自带，中文通常是机械音"));
                break;
        }
        return out;
    }

    /** 按角色分配默认音色 —— 六个内置角色各有不同 */
    public static String defaultVoiceFor(String cardId, String kind) {
        // CosyVoice 也按角色区分
        if (KIND_QWEN.equals(kind)) {
            if (cardId == null) {
                return "longxiaochun_v2";
            }
            switch (cardId) {
                case "builtin_alice":   return "longwan_v2";       // 温柔寡言 → 龙婉
                case "builtin_rin":     return "longyue_v2";       // 傲娇、语速快 → 龙悦
                case "builtin_hakuyo":  return "longjing_v2";      // 安静清冷 → 龙婧
                case "builtin_rocco":   return "longmiao_v2";      // 话密少女 → 龙嫇
                case "builtin_elian":   return "longxiaoxia_v2";   // 成熟治愈 → 龙小夏
                case "builtin_yueling": return "longjing_v2";      // 神秘疏离 → 龙婧
                default: return "longxiaochun_v2";
            }
        }
        if (!KIND_LOCAL.equals(kind)) {
            if (!voicesFor(kind).isEmpty()) {
                return voicesFor(kind).get(0).id;
            }
            return "";
        }
        // 本地：用不同音色把角色区分开
        if (cardId == null) {
            return "zh-CN-XiaoxiaoNeural";
        }
        switch (cardId) {
            case "builtin_alice":   return "zh-CN-XiaoxiaoNeural";   // 温柔寡言
            case "builtin_rin":     return "zh-CN-XiaoyiNeural";     // 傲娇、语速快
            case "builtin_hakuyo":  return "zh-CN-XiaoxiaoNeural";   // 安静
            case "builtin_rocco":   return "zh-CN-XiaoyiNeural";     // 话密
            case "builtin_elian":   return "zh-CN-XiaoxiaoNeural";   // 成熟治愈
            case "builtin_yueling": return "ja-JP-NanamiNeural";     // 神秘少女
            default: return "zh-CN-XiaoxiaoNeural";
        }
    }

    public static String voiceLabel(String kind, String id) {
        for (Voice v : voicesFor(kind)) {
            if (v.id.equals(id)) {
                return v.label;
            }
        }
        return id;
    }

    // ==================================================================
    // 合成
    // ==================================================================

    /**
     * 把文字合成成音频字节（mp3 或 wav）。同步阻塞，调用方负责放后台线程。
     *
     * @return 音频数据；失败抛异常
     */
    public static byte[] synthesize(Context ctx, Config cfg, String text, String voice)
            throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new Exception("没有可读的文本");
        }
        // 去掉括号里的动作描写 —— 那些不该被念出来
        String speak = text
                .replaceAll("（[^）]*）", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\*[^*]*\\*", "")
                .trim();
        if (speak.isEmpty()) {
            speak = text.trim();
        }

        switch (cfg.kind) {
            case KIND_LOCAL:
                return ttsLocal(cfg, speak, voice);
            case KIND_QWEN:
                return ttsQwen(ctx, cfg, speak, voice);
            case KIND_OPENAI:
                return ttsOpenAi(ctx, cfg, speak, voice);
            case KIND_CUSTOM:
                return ttsCustom(cfg, speak, voice);
            default:
                throw new Exception("SYSTEM");   // 交给系统 TTS 处理，不走这里
        }
    }

    /**
     * 阿里云百炼 CosyVoice。
     *
     * 这一步和别的渠道不一样：**它返回的不是音频，是一个 URL**。
     * 响应长这样：
     *   {"output":{"audio":{"url":"http://dashscope-result-....mp3?Expires=...",
     *                       "expires_at":1789875292,"id":"audio_xxx"}}}
     * 所以要两步：先拿到 url，再去下载。URL 24 小时有效。
     *
     * 实测：一句话约 32 KB / 128 kbps / 2 秒，音质比 edge-tts 的 48 kbps 高一倍多。
     */
    private static byte[] ttsQwen(Context ctx, Config cfg, String text, String voice)
            throws Exception {
        String key = SecureStore.get(ctx);
        if (key == null || key.isEmpty()) {
            throw new Exception("CosyVoice 需要 API Key，请到设置页填写");
        }
        String v = (voice == null || voice.isEmpty()) ? "longxiaochun_v2" : voice;

        JSONObject input = new JSONObject();
        input.put("text", text);
        input.put("voice", v);
        input.put("format", "mp3");
        input.put("sample_rate", 22050);
        // 语速：CosyVoice 用倍率，不是百分号
        if (cfg.speed != 1.0f) {
            input.put("speed", cfg.speed);
        }
        JSONObject body = new JSONObject();
        body.put("model", "cosyvoice-v2");
        body.put("input", input);

        String resp = postJsonAuth(DASHSCOPE_TTS_URL, body.toString(), key);
        JSONObject root = new JSONObject(resp);
        JSONObject audio = root.optJSONObject("output");
        if (audio == null) {
            throw new Exception("返回格式异常：" + cut(resp));
        }
        audio = audio.optJSONObject("audio");
        if (audio == null) {
            throw new Exception("返回里没有 audio：" + cut(resp));
        }
        String url = audio.optString("url", "");
        if (url.isEmpty()) {
            // 有的模型走 data 字段（base64/hex），这里按需兜底
            String data = audio.optString("data", "");
            if (!data.isEmpty() && data.startsWith("http")) {
                url = data;
            } else {
                throw new Exception("没有拿到音频地址：" + cut(resp));
            }
        }
        return downloadBytes(url);
    }

    /** 本地 edge-tts 服务 */
    private static byte[] ttsLocal(Config cfg, String text, String voice) throws Exception {
        String host = ProviderStore.cleanHost(cfg.localHost);
        if (host.isEmpty()) {
            host = "10.0.2.2:5001";
        }
        String v = (voice == null || voice.isEmpty()) ? cfg.voice : voice;
        String url = "http://" + host + "/tts?text=" + URLEncoder.encode(text, "UTF-8")
                + "&voice=" + URLEncoder.encode(v, "UTF-8")
                + "&rate=" + URLEncoder.encode(cfg.rate, "UTF-8")
                + "&pitch=" + URLEncoder.encode(cfg.pitch, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(60000);
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new Exception("TTS 服务返回 HTTP " + code
                        + "（确认电脑上 tts-server.py 在运行）");
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /** OpenAI 兼容的 /v1/audio/speech */
    private static byte[] ttsOpenAi(Context ctx, Config cfg, String text, String voice)
            throws Exception {
        String base = cfg.openaiUrl;
        if (base == null || base.isEmpty()) {
            throw new Exception("还没填 OpenAI 兼容地址");
        }
        String key = SecureStore.get(ctx);
        if (key == null || key.isEmpty()) {
            throw new Exception("OpenAI TTS 需要 API Key，请到设置页填写");
        }
        String url = base.replaceAll("/+$", "") + "/v1/audio/speech";
        String v = (voice == null || voice.isEmpty()) ? "nova" : voice;

        JSONObject body = new JSONObject();
        body.put("model", "tts-1");
        body.put("input", text);
        body.put("voice", v);
        body.put("response_format", "mp3");

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Expect", "");   // 同 ImageProvider，避免 100-continue 卡死
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        byte[] payload = body.toString().getBytes("UTF-8");
        conn.setFixedLengthStreamingMode(payload.length);
        try {
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                String err = "";
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        err += line;
                    }
                } catch (Exception ignore) {
                }
                if (err.length() > 200) {
                    err = err.substring(0, 200);
                }
                throw new Exception("TTS 返回 HTTP " + code + (err.isEmpty() ? "" : "：" + err));
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /** 自定义端点 */
    private static byte[] ttsCustom(Config cfg, String text, String voice) throws Exception {
        if (cfg.customUrl == null || cfg.customUrl.trim().isEmpty()) {
            throw new Exception("还没填自定义 TTS 端点");
        }
        String url = cfg.customUrl
                .replace("{text}", URLEncoder.encode(text, "UTF-8"))
                .replace("{voice}", URLEncoder.encode(voice == null ? "" : voice, "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        try {
            if (conn.getResponseCode() != 200) {
                throw new Exception("端点返回 HTTP " + conn.getResponseCode());
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /** 探测本地 TTS 服务并列出它支持的音色 */
    public static String probeLocal(String rawHost) {
        String host = ProviderStore.cleanHost(rawHost);
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (!host.isEmpty()) {
            candidates.add(host);
        }
        candidates.add("10.0.2.2:5001");
        candidates.add("127.0.0.1:5001");
        candidates.add("localhost:5001");

        for (String c : candidates) {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL("http://" + c + "/health").openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(6000);
                try {
                    if (conn.getResponseCode() == 200) {
                        return "✓ 已连接 " + c + "\n（用这个地址）";
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignore) {
            }
        }
        return "✗ 连不上。请在电脑上运行：\n"
                + "python tools/tts-server.py\n"
                + "（模拟器填 10.0.2.2:5001，真机填电脑局域网 IP）";
    }

    private static byte[] readAll(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /**
     * 带 Bearer 的 JSON POST，返回响应文本。
     *
     * 注意 `Expect: ""` —— Android 对较大的 POST 会自动加
     * `Expect: 100-continue`，服务端不回的话请求会永远卡住，
     * 而且 setReadTimeout 管不到那一段。这个坑在 ImageProvider 里踩过一次。
     */
    private static String postJsonAuth(String urlStr, String json, String key)
            throws Exception {
        // 统一走 Http —— Expect/超时这些坑在那一层处理掉了
        return Http.postJson(urlStr, json, key).orThrow("语音合成").body;
    }

    private static byte[] downloadBytes(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        try {
            if (conn.getResponseCode() != 200) {
                throw new Exception("下载音频失败 HTTP " + conn.getResponseCode());
            }
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /** 错误信息截断，避免把一整页 HTML 塞进 UI */
    private static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
