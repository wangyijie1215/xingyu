package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 语音识别（你说话 → 文字）。
 *
 * 四个渠道，按「省事程度」排序，自动降级：
 *
 *   1. **手机自带**（system）
 *      Android 的 SpeechRecognizer。不用装东西、不用 key、不用联网
 *      （取决于手机是否有离线包）。**默认就该用这个。**
 *
 *   2. **本地 whisper 服务**（local）
 *      电脑跑 tools/stt-server.py。识别最准，且完全不出本机。
 *      中英混说、方言、专有名词都明显好于手机自带。
 *
 *   3. **阿里云百炼**（dashscope）
 *      paraformer 语音识别。国内直连，按量计费。
 *
 *   4. **OpenAI 兼容**（openai）
 *      /v1/audio/transcriptions。中转站如果开了 whisper 渠道就能用。
 *
 * 设计取向：**手机自带优先**。
 * 因为它零配置、零延迟、离线可用 —— 绝大多数场景够用。
 * 只有在识别质量明显不够时（比如经常听错人名、中英混杂），
 * 才值得开电脑跑 whisper。
 */
public class SttProvider {

    public static final String KIND_SYSTEM = "system";
    public static final String KIND_LOCAL = "local";
    public static final String KIND_DASHSCOPE = "dashscope";
    public static final String KIND_OPENAI = "openai";

    private static final String PREFS = "echoflow_stt";

    public static class Config {
        /** 默认用手机自带的 */
        public String kind = KIND_SYSTEM;
        /** 本地 whisper 服务地址 */
        public String localHost = "10.0.2.2:5002";
        /** 本地服务用的模型名（tiny/base/small/medium/large-v3） */
        public String localModel = "small";
        /** 识别语言，空 = 自动 */
        public String language = "zh";
        /** OpenAI 兼容转写 */
        public String openaiUrl = "https://aibridgea.com";
        public String openaiModel = "whisper-1";
        public boolean enabled = true;
    }

    public static Config load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Config c = new Config();
        c.kind = p.getString("kind", KIND_SYSTEM);
        c.localHost = p.getString("localHost", "10.0.2.2:5002");
        c.localModel = p.getString("localModel", "small");
        c.language = p.getString("language", "zh");
        c.openaiUrl = p.getString("openaiUrl", "https://aibridgea.com");
        c.openaiModel = p.getString("openaiModel", "whisper-1");
        c.enabled = p.getBoolean("enabled", true);
        return c;
    }

    public static void save(Context ctx, Config c) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("kind", c.kind)
                .putString("localHost", c.localHost)
                .putString("localModel", c.localModel)
                .putString("language", c.language)
                .putString("openaiUrl", c.openaiUrl)
                .putString("openaiModel", c.openaiModel)
                .putBoolean("enabled", c.enabled)
                .apply();
    }

    public static String kindName(String kind) {
        switch (kind) {
            case KIND_LOCAL: return "本地 whisper";
            case KIND_DASHSCOPE: return "阿里云百炼";
            case KIND_OPENAI: return "OpenAI 兼容";
            default: return "手机自带";
        }
    }

    public static String kindHint(String kind) {
        switch (kind) {
            case KIND_LOCAL:
                return "电脑上运行 python tools/stt-server.py\n"
                        + "识别最准，且完全不出本机。需要电脑开着。";
            case KIND_DASHSCOPE:
                return "用百炼的 paraformer，国内直连，按量计费。\n"
                        + "和 CosyVoice 共用一个 Key。";
            case KIND_OPENAI:
                return "任何兼容 /v1/audio/transcriptions 的服务。\n"
                        + "中转站需要先给这个分组开 whisper 渠道。";
            default:
                return "用手机系统自带的语音识别。\n"
                        + "零配置、不用联网、不用花钱 —— 默认就用这个。";
        }
    }

    // ==================================================================
    // 直连渠道的转写实现（手机自带走 SpeechRecognizer，不在这里）
    // ==================================================================

    /**
     * 把一段音频转成文字。同步阻塞，调用方放后台线程。
     *
     * @param audio  录音数据（wav/m4a 字节）
     * @param format 文件扩展名，例如 "m4a"
     */
    public static String transcribe(Context ctx, Config cfg, byte[] audio, String format)
            throws Exception {
        if (audio == null || audio.length == 0) {
            throw new Exception("没有录到声音");
        }
        switch (cfg.kind) {
            case KIND_LOCAL:
                return sttLocal(cfg, audio, format);
            case KIND_DASHSCOPE:
                return sttDashscope(ctx, cfg, audio, format);
            case KIND_OPENAI:
                return sttOpenAi(ctx, cfg, audio, format);
            default:
                throw new Exception("SYSTEM");   // 手机自带不走这条路
        }
    }

    /** 本地 whisper 服务 */
    private static String sttLocal(Config cfg, byte[] audio, String format) throws Exception {
        String host = ProviderStore.cleanHost(cfg.localHost);
        if (host.isEmpty()) {
            host = "10.0.2.2:5002";
        }
        String url = "http://" + host + "/transcribe"
                + "?model=" + java.net.URLEncoder.encode(cfg.localModel, "UTF-8")
                + "&language=" + java.net.URLEncoder.encode(cfg.language, "UTF-8");

        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Expect", "");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(180000);
        conn.setFixedLengthStreamingMode(audio.length);
        try {
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(audio);
                os.flush();
            }
            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = is == null ? "" : Http.readLines(is);
            if (code != 200) {
                throw new Exception("whisper 服务返回 HTTP " + code
                        + (body.isEmpty() ? "" : "：" + cut(body)));
            }
            return new org.json.JSONObject(body).optString("text", "").trim();
        } finally {
            conn.disconnect();
        }
    }

    /** 阿里云百炼 paraformer */
    private static String sttDashscope(Context ctx, Config cfg, byte[] audio, String format)
            throws Exception {
        String key = SecureStore.get(ctx);
        if (key == null || key.isEmpty()) {
            throw new Exception("需要百炼 API Key（在设置页填）");
        }
        // DashScope 的录音文件识别是异步 API：先提交拿 task_id，再轮询结果。
        // 这里用「一句话识别」的同步形式（paraformer-realtime-v2 的 HTTP 版本）
        String url = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription";
        String dataUri = "data:audio/" + format + ";base64,"
                + android.util.Base64.encodeToString(audio, android.util.Base64.NO_WRAP);

        org.json.JSONObject input = new org.json.JSONObject();
        input.put("file_urls", new org.json.JSONArray().put(dataUri));
        org.json.JSONObject body = new org.json.JSONObject();
        body.put("model", "paraformer-v2");
        body.put("input", input);

        String resp = Http.postJson(url, body.toString(), key,
                "X-DashScope-Async:enable", 15000, 120000).orThrow("语音识别").body;
        org.json.JSONObject root = new org.json.JSONObject(resp);
        String taskId = root.optJSONObject("output") == null
                ? "" : root.getJSONObject("output").optString("task_id", "");
        if (taskId.isEmpty()) {
            // 有的部署直接同步返回结果
            String direct = deepFindText(root);
            if (!direct.isEmpty()) {
                return direct;
            }
            throw new Exception("没有拿到 task_id：" + cut(resp));
        }

        // 轮询
        long deadline = System.currentTimeMillis() + 120000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(1500);
            String q = Http.get("https://dashscope.aliyuncs.com/api/v1/tasks/" + taskId,
                    key, 10000, 30000).orThrow("查询识别结果").body;
            org.json.JSONObject qr = new org.json.JSONObject(q);
            org.json.JSONObject out = qr.optJSONObject("output");
            if (out == null) {
                continue;
            }
            String status = out.optString("task_status", "");
            if ("SUCCEEDED".equals(status)) {
                String t = deepFindText(out);
                if (!t.isEmpty()) {
                    return t;
                }
                throw new Exception("识别完成但没拿到文本");
            }
            if ("FAILED".equals(status)) {
                throw new Exception("识别失败：" + cut(q));
            }
        }
        throw new Exception("识别超时（120 秒）");
    }

    /** OpenAI 兼容的 /v1/audio/transcriptions（multipart/form-data） */
    private static String sttOpenAi(Context ctx, Config cfg, byte[] audio, String format)
            throws Exception {
        String key = SecureStore.get(ctx);
        if (key == null || key.isEmpty()) {
            throw new Exception("需要 API Key（在设置页填）");
        }
        String base = cfg.openaiUrl;
        if (base == null || base.trim().isEmpty()) {
            throw new Exception("还没填转写接口地址");
        }
        String url = base.replaceAll("/+$", "") + "/v1/audio/transcriptions";

        String boundary = "----EfBoundary" + System.currentTimeMillis();
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        // file 字段
        bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"audio."
                + format + "\"\r\n").getBytes("UTF-8"));
        bos.write(("Content-Type: audio/" + format + "\r\n\r\n").getBytes("UTF-8"));
        bos.write(audio);
        bos.write("\r\n".getBytes("UTF-8"));
        // model 字段
        bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        bos.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n".getBytes("UTF-8"));
        bos.write(cfg.openaiModel.getBytes("UTF-8"));
        bos.write("\r\n".getBytes("UTF-8"));
        // language 字段
        if (cfg.language != null && !cfg.language.isEmpty()) {
            bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            bos.write("Content-Disposition: form-data; name=\"language\"\r\n\r\n"
                    .getBytes("UTF-8"));
            bos.write(cfg.language.getBytes("UTF-8"));
            bos.write("\r\n".getBytes("UTF-8"));
        }
        bos.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        byte[] payload = bos.toByteArray();

        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Expect", "");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        conn.setFixedLengthStreamingMode(payload.length);
        try {
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = is == null ? "" : Http.readLines(is);
            if (code != 200) {
                throw new Exception("转写接口返回 HTTP " + code
                        + (body.isEmpty() ? "" : "：" + cut(body)));
            }
            // 有的返回 {"text":"..."}，有的直接返回纯文本
            String trimmed = body.trim();
            if (trimmed.startsWith("{")) {
                return new org.json.JSONObject(trimmed).optString("text", "").trim();
            }
            return trimmed;
        } finally {
            conn.disconnect();
        }
    }

    /** 在未知层级的 JSON 里找第一个 text / transcription 字段 */
    private static String deepFindText(Object node) {
        if (node instanceof org.json.JSONObject) {
            org.json.JSONObject o = (org.json.JSONObject) node;
            String t = o.optString("text", "");
            if (!t.isEmpty()) {
                return t;
            }
            String tr = o.optString("transcription", "");
            if (!tr.isEmpty()) {
                return tr;
            }
            for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) {
                String found = deepFindText(o.opt(it.next()));
                if (!found.isEmpty()) {
                    return found;
                }
            }
        } else if (node instanceof org.json.JSONArray) {
            org.json.JSONArray a = (org.json.JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                String found = deepFindText(a.opt(i));
                if (!found.isEmpty()) {
                    return found;
                }
            }
        }
        return "";
    }

    /** 探测本地 whisper 服务 */
    public static String probeLocal(String rawHost) {
        String host = ProviderStore.cleanHost(rawHost);
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (!host.isEmpty()) {
            candidates.add(host);
        }
        candidates.add("10.0.2.2:5002");
        candidates.add("127.0.0.1:5002");
        candidates.add("localhost:5002");
        for (String c : candidates) {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                        new java.net.URL("http://" + c + "/health").openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(6000);
                try {
                    if (conn.getResponseCode() == 200) {
                        return "✓ 已连接 " + c;
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignore) {
            }
        }
        return "✗ 连不上。请在电脑上运行：\n"
                + "python tools/stt-server.py\n"
                + "（模拟器填 10.0.2.2:5002，真机填电脑局域网 IP）";
    }

    private static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}
