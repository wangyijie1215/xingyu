package com.echoflow.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * 生图渠道抽象。
 *
 * 三个渠道，各有取舍，都做成可选：
 *
 *   1. **Pollinations**（cloud）
 *      免费、不需要 key、任何网络都能用。缺点是只有一个模型（sana），
 *      画质一般，而且画风和角色卡对不上。
 *
 *   2. **ComfyUI 本地**（comfy）
 *      质量最好 —— 能用上 Pony V6 XL 这类真·动漫模型，以及本机已有的 LoRA。
 *      代价：必须电脑开着，手机和电脑在同一网络。对桌宠/立绘这类
 *      "要像她本人"的需求，这是唯一现实的选择。
 *
 *   3. **自定义 HTTP 端点**（custom）
 *      给以后留的口子：任何接受 {prompt} 返回图片的 HTTP 服务。
 *
 * 为什么不做成"自动选最好的"：三个渠道的可用性差异是**环境性的**
 * （家里有网 vs 在外面），程序猜不准。让用户明确选，比猜错了好。
 */
public class ImageProvider {

    public static final String KIND_CLOUD = "cloud";
    public static final String KIND_COMFY = "comfy";
    public static final String KIND_CUSTOM = "custom";
    /** OpenAI 兼容的 /v1/images/generations（Grok / DALL·E / 各种中转站） */
    public static final String KIND_OPENAI = "openai";

    private static final String PREFS = "echoflow_image";

    public static class Config {
        public String kind = KIND_CLOUD;
        /** ComfyUI 地址，例如 192.168.1.100:8188 */
        public String comfyHost = "10.0.2.2:8188";
        public String comfyCkpt = "ponyDiffusionV6XL_v6.safetensors";
        /** OpenAI 兼容生图：接口地址、模型名、尺寸 */
        public String openaiUrl = "https://aibridgea.com";
        public String openaiModel = "grok-imagine-image-2.0";
        public String openaiSize = "1024x1024";
        /** 复用设置页里那个 API Key */
        public boolean openaiUseSharedKey = true;
        public String openaiKey = "";
        public int steps = 26;
        public float cfg = 7.0f;
        /** 自定义端点的 URL 模板，含 {prompt} 占位符 */
        public String customUrl = "";
        /** 用户自己写的画风后缀，追加到每个提示词后面 */
        public String styleSuffix = "";
    }

    public static Config load(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Config c = new Config();
        c.kind = p.getString("kind", KIND_CLOUD);
        c.comfyHost = p.getString("comfyHost", "10.0.2.2:8188");
        c.comfyCkpt = p.getString("comfyCkpt", "ponyDiffusionV6XL_v6.safetensors");
        c.steps = p.getInt("steps", 26);
        c.cfg = p.getFloat("cfg", 7.0f);
        c.customUrl = p.getString("customUrl", "");
        c.styleSuffix = p.getString("styleSuffix", "");
        c.openaiUrl = p.getString("openaiUrl", "https://aibridgea.com");
        c.openaiModel = p.getString("openaiModel", "grok-imagine-image-2.0");
        c.openaiSize = p.getString("openaiSize", "1024x1024");
        c.openaiUseSharedKey = p.getBoolean("openaiUseSharedKey", true);
        c.openaiKey = p.getString("openaiKey", "");
        return c;
    }

    public static void save(Context ctx, Config c) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("kind", c.kind)
                .putString("comfyHost", c.comfyHost)
                .putString("comfyCkpt", c.comfyCkpt)
                .putInt("steps", c.steps)
                .putFloat("cfg", c.cfg)
                .putString("customUrl", c.customUrl)
                .putString("styleSuffix", c.styleSuffix)
                .putString("openaiUrl", c.openaiUrl)
                .putString("openaiModel", c.openaiModel)
                .putString("openaiSize", c.openaiSize)
                .putBoolean("openaiUseSharedKey", c.openaiUseSharedKey)
                .putString("openaiKey", c.openaiKey)
                .apply();
    }

    // ==================================================================
    // ComfyUI 探测
    // ==================================================================

    public static class Probe {
        public boolean ok;
        public String message = "";
        public String host = "";
        public java.util.List<String> checkpoints = new java.util.ArrayList<>();
    }

    /** 探测 ComfyUI 并拉 checkpoint 列表。必须在后台线程调用。 */
    public static Probe probeComfy(String rawHost) {
        Probe p = new Probe();
        String h = ProviderStore.cleanHost(rawHost);
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (!h.isEmpty()) {
            candidates.add(h);
        }
        candidates.add("10.0.2.2:8188");
        candidates.add("127.0.0.1:8188");
        candidates.add("localhost:8188");

        for (String c : candidates) {
            try {
                String url = "http://" + c + "/object_info/CheckpointLoaderSimple";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(8000);
                try {
                    if (conn.getResponseCode() != 200) {
                        continue;
                    }
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    JSONObject root = new JSONObject(sb.toString());
                    JSONArray arr = root.getJSONObject("CheckpointLoaderSimple")
                            .getJSONObject("input").getJSONObject("required")
                            .getJSONArray("ckpt_name").getJSONArray(0);
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        names.add(arr.getString(i));
                    }
                    if (!names.isEmpty()) {
                        p.ok = true;
                        p.host = c;
                        p.checkpoints = names;
                        p.message = "已连接 · 发现 " + names.size() + " 个模型";
                        return p;
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignore) {
            }
        }
        p.ok = false;
        // 连不上时给出可操作的原因，而不是一句"网络异常"。
        // 实测最常见的原因是 ComfyUI 只监听 127.0.0.1（默认行为），
        // 手机侧无论怎么填都连不上 —— 必须让用户知道要改启动参数。
        p.message = "连不上 ComfyUI。可能是这几种情况：\n"
                + "1. 电脑上没启动 ComfyUI\n"
                + "2. ComfyUI 只监听了 127.0.0.1 —— 默认就是这样，手机连不上。\n"
                + "   解决：启动时加 --listen 参数，即 python main.py --listen 0.0.0.0\n"
                + "3. 防火墙挡了 8188 端口\n"
                + "4. 手机和电脑不在同一网络\n"
                + "（模拟器填 10.0.2.2:8188，真机填电脑的局域网 IP）";
        return p;
    }

    // ==================================================================
    // 生成
    // ==================================================================

    /**
     * 提交生成任务（同步阻塞，调用方负责放后台线程）。
     *
     * @param fullPrompt 完整提示词（调用方已拼好风格词）
     * @param seed       随机种子；同一个 seed 出同一张图
     */
    /** 兼容旧签名 */
    public static Bitmap generate(Context ctx, Config cfg, String fullPrompt, int seed,
                                  int width, int height) throws Exception {
        return generate(ctx, cfg, fullPrompt, null, seed, width, height);
    }

    public static Bitmap generate(Context ctx, Config cfg, String fullPrompt,
                                  String negative, int seed,
                                  int width, int height) throws Exception {
        switch (cfg.kind) {
            case KIND_COMFY:
                return genComfy(cfg, fullPrompt, negative, seed, width, height);
            case KIND_OPENAI:
                return genOpenAi(ctx, cfg, fullPrompt, width, height);
            case KIND_CUSTOM:
                return genCustom(cfg, fullPrompt, width, height);
            case KIND_CLOUD:
            default:
                return genPollinations(fullPrompt, seed, width, height);
        }
    }

    // ---------------- Pollinations ----------------

    private static Bitmap genPollinations(String prompt, int seed, int w, int h)
            throws Exception {
        String url = "https://image.pollinations.ai/prompt/"
                + java.net.URLEncoder.encode(prompt, "UTF-8")
                + "?width=" + w + "&height=" + h
                + "&seed=" + Math.abs(seed)
                + "&nologo=true";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(180000);
        conn.setRequestProperty("User-Agent", "EchoFlow/1.0 (Android)");
        try {
            if (conn.getResponseCode() != 200) {
                throw new Exception("Pollinations 返回 HTTP " + conn.getResponseCode()
                        + "（公共免费服务，可能被限流，稍后重试）");
            }
            try (InputStream is = conn.getInputStream()) {
                Bitmap b = android.graphics.BitmapFactory.decodeStream(is);
                if (b == null) {
                    throw new Exception("返回的不是有效图片");
                }
                return b;
            }
        } finally {
            conn.disconnect();
        }
    }

    // ---------------- OpenAI 兼容生图 ----------------

    /**
     * OpenAI 兼容的 /v1/images/generations。
     *
     * 适用于 Grok 画图、DALL·E、以及各种中转站。
     * 请求：POST {base}/v1/images/generations
     *   {"model":"grok-imagine-image-2.0","prompt":"...","n":1,"size":"1024x1024"}
     * 响应：{"data":[{"url":"https://..."}]} 或 {"data":[{"b64_json":"..."}]}
     *
     * 两种返回都支持 —— 有的中转给 URL，有的给 base64。
     */
    private static Bitmap genOpenAi(Context ctx, Config cfg, String prompt,
                                    int w, int h) throws Exception {
        String key = cfg.openaiUseSharedKey ? SecureStore.get(ctx) : cfg.openaiKey;
        if (key == null || key.isEmpty()) {
            throw new Exception("OpenAI 兼容生图需要 API Key（在设置页填，或在本页单独填）");
        }
        String base = cfg.openaiUrl;
        if (base == null || base.trim().isEmpty()) {
            throw new Exception("还没填生图接口地址");
        }
        String url = base.replaceAll("/+$", "") + "/v1/images/generations";

        JSONObject body = new JSONObject();
        body.put("model", cfg.openaiModel);
        body.put("prompt", prompt);
        body.put("n", 1);
        // 尺寸用配置里的；ComfyUI 走的宽高参数在这里不适用
        if (cfg.openaiSize != null && !cfg.openaiSize.isEmpty()) {
            body.put("size", cfg.openaiSize);
        }

        HttpResponse resp = postJsonWithDetail(url, body.toString(), key);
        if (resp.code != 200) {
            throw new Exception("生图接口返回 HTTP " + resp.code
                    + (resp.body.isEmpty() ? "" : "：" + cut(resp.body)));
        }

        JSONObject root = new JSONObject(resp.body);
        JSONArray data = root.optJSONArray("data");
        if (data == null || data.length() == 0) {
            throw new Exception("返回里没有图片数据：" + cut(resp.body));
        }
        JSONObject first = data.getJSONObject(0);

        // 情况一：给了 URL
        String u = first.optString("url", "");
        if (!u.isEmpty()) {
            return downloadImage(u);
        }
        // 情况二：给了 base64
        String b64 = first.optString("b64_json", "");
        if (!b64.isEmpty()) {
            byte[] raw = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
            Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (bmp == null) {
                throw new Exception("base64 解码后不是有效图片");
            }
            return bmp;
        }
        throw new Exception("返回里既没有 url 也没有 b64_json：" + cut(resp.body));
    }

    /**
     * 带响应码和响应体的 POST。
     *
     * 不能直接用 Http.postJson —— 它在非 2xx 时会抛异常，
     * 而这里需要拿到**原始错误内容**展示给用户
     * （比如中转站会回「API Key 所属分组已删除」这种具体原因）。
     */
    private static HttpResponse postJsonWithDetail(String urlStr, String json, String key)
            throws Exception {
        java.net.HttpURLConnection conn =
                (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Expect", "");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(180000);
        byte[] payload = json.getBytes("UTF-8");
        conn.setFixedLengthStreamingMode(payload.length);
        try {
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
            int code = conn.getResponseCode();
            java.io.InputStream is = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String text = "";
            if (is != null) {
                try {
                    text = Http.readLines(is);
                } finally {
                    is.close();
                }
            }
            return new HttpResponse(code, text);
        } finally {
            conn.disconnect();
        }
    }

    private static class HttpResponse {
        final int code;
        final String body;

        HttpResponse(int code, String body) {
            this.code = code;
            this.body = body == null ? "" : body;
        }
    }
    // ---------------- 自定义端点 ----------------

    private static Bitmap genCustom(Config cfg, String prompt, int w, int h) throws Exception {
        if (cfg.customUrl == null || cfg.customUrl.trim().isEmpty()) {
            throw new Exception("还没填自定义端点地址");
        }
        String url = cfg.customUrl
                .replace("{prompt}", java.net.URLEncoder.encode(prompt, "UTF-8"))
                .replace("{width}", String.valueOf(w))
                .replace("{height}", String.valueOf(h));
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(180000);
        try {
            if (conn.getResponseCode() != 200) {
                throw new Exception("端点返回 HTTP " + conn.getResponseCode());
            }
            try (InputStream is = conn.getInputStream()) {
                Bitmap b = android.graphics.BitmapFactory.decodeStream(is);
                if (b == null) {
                    throw new Exception("返回的不是有效图片");
                }
                return b;
            }
        } finally {
            conn.disconnect();
        }
    }

    // ---------------- ComfyUI ----------------

    /**
     * 走 ComfyUI 的 /prompt 接口，然后轮询 /history 取图。
     *
     * 这是 ComfyUI 的标准编程调用方式：
     *   1. POST /prompt 提交 workflow，拿到 prompt_id
     *   2. 轮询 GET /history/{prompt_id} 直到有产物
     *   3. 从产物里取 filename，再 GET /view 拿图片字节
     */
    private static Bitmap genComfy(Config cfg, String prompt, String negative,
                                   int seed, int w, int h)
            throws Exception {
        String host = ProviderStore.cleanHost(cfg.comfyHost);
        if (host.isEmpty()) {
            host = "10.0.2.2:8188";
        }
        String base = "http://" + host;

        // ---- 0) 先做一次轻量 GET，确认能连通 ----
        // 这样失败时能立刻定位是"网络不通"还是"任务被拒"，
        // 而不是卡在 POST 上一声不响。
        try {
            HttpURLConnection probe = (HttpURLConnection)
                    new URL(base + "/system_stats").openConnection();
            probe.setConnectTimeout(8000);
            probe.setReadTimeout(8000);
            int pc = probe.getResponseCode();
            android.util.Log.i("ImageGen", "probe " + base + " -> HTTP " + pc);
            probe.disconnect();
            if (pc != 200) {
                throw new Exception("ComfyUI 探测返回 HTTP " + pc);
            }
        } catch (Exception pe) {
            throw new Exception("连不上 ComfyUI（" + base + "）："
                    + (pe.getMessage() == null ? "网络错误" : pe.getMessage()));
        }

        // ---- 1) 构造 workflow ----
        JSONObject wf = new JSONObject();
        wf.put("4", node("CheckpointLoaderSimple",
                new String[]{"ckpt_name"}, new Object[]{cfg.comfyCkpt}));
        wf.put("6", node("CLIPTextEncode",
                new String[]{"text", "clip"}, new Object[]{prompt, new Object[]{"4", 1}}));
        String neg = (negative == null || negative.trim().isEmpty()) ? NEGATIVE : negative;
        wf.put("7", node("CLIPTextEncode",
                new String[]{"text", "clip"},
                new Object[]{neg, new Object[]{"4", 1}}));
        wf.put("5", node("EmptyLatentImage",
                new String[]{"width", "height", "batch_size"},
                new Object[]{w, h, 1}));
        wf.put("3", node("KSampler",
                new String[]{"seed", "steps", "cfg", "sampler_name", "scheduler",
                        "denoise", "model", "positive", "negative", "latent_image"},
                new Object[]{
                        Math.abs(seed), cfg.steps, cfg.cfg, "euler", "normal", 1.0,
                        new Object[]{"4", 0},
                        new Object[]{"6", 0},
                        new Object[]{"7", 0},
                        new Object[]{"5", 0}
                }));
        wf.put("8", node("VAEDecode",
                new String[]{"samples", "vae"}, new Object[]{new Object[]{"3", 0}, new Object[]{"4", 2}}));
        wf.put("9", node("SaveImage",
                new String[]{"images", "filename_prefix"},
                new Object[]{new Object[]{"8", 0}, "echoflow"}));

        JSONObject body = new JSONObject();
        body.put("prompt", wf);
        body.put("client_id", UUID.randomUUID().toString());

        // ---- 2) 提交 ----
        android.util.Log.i("ImageGen", "posting to " + base + "/prompt len="
                + body.toString().length());
        String promptId = postJson(base + "/prompt", body.toString());
        android.util.Log.i("ImageGen", "posted, promptId=" + promptId);

        // ---- 3) 轮询历史 ----
        long deadline = System.currentTimeMillis() + 180000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(900);
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(base + "/history/" + promptId).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                try {
                    if (conn.getResponseCode() != 200) {
                        continue;
                    }
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    if (sb.length() <= 2) {
                        continue;   // 还没完成
                    }
                    JSONObject root = new JSONObject(sb.toString());
                    JSONObject job = root.optJSONObject(promptId);
                    if (job == null) {
                        continue;
                    }
                    JSONObject outputs = job.optJSONObject("outputs");
                    if (outputs == null) {
                        continue;
                    }
                    java.util.Iterator<String> it = outputs.keys();
                    while (it.hasNext()) {
                        JSONObject out = outputs.optJSONObject(it.next());
                        if (out == null) {
                            continue;
                        }
                        JSONArray imgs = out.optJSONArray("images");
                        if (imgs == null || imgs.length() == 0) {
                            continue;
                        }
                        JSONObject img = imgs.getJSONObject(0);
                        String filename = img.getString("filename");
                        String subfolder = img.optString("subfolder", "");
                        String type = img.optString("type", "output");
                        String view = base + "/view?filename="
                                + java.net.URLEncoder.encode(filename, "UTF-8")
                                + "&subfolder=" + java.net.URLEncoder.encode(subfolder, "UTF-8")
                                + "&type=" + type;
                        return downloadImage(view);
                    }
                } finally {
                    conn.disconnect();
                }
            } catch (Exception ignore) {
                // 继续轮询
            }
        }
        throw new Exception("ComfyUI 生成超时（180 秒）。显卡可能被占满，或模型太大。");
    }

    /** 负面提示词：Pony 系列需要这一串才能稳定出好图 */
    private static final String NEGATIVE =
            "score_4, score_5, score_6, worst quality, low quality, "
            + "blurry, jpeg artifacts, watermark, signature, text, "
            + "bad anatomy, bad hands, extra fingers, deformed";

    /**
     * 构造一个 ComfyUI 节点。
     *
     * 关键：节点之间的连线必须写成 JSONArray（`["8", 0]`），
     * 不能塞 Object[] —— org.json 的 put(key, Object) 遇到裸数组
     * 会把它当成字符串处理，于是服务端收到 "[\"8\",0]" 这种字符串，
     * 报 `'str' object has no attribute 'shape'`。
     *
     * vals 里凡是 Object[] 都自动转成 JSONArray。
     */
    private static JSONObject node(String classType, String[] keys, Object[] vals)
            throws Exception {
        JSONObject n = new JSONObject();
        n.put("class_type", classType);
        JSONObject inputs = new JSONObject();
        for (int i = 0; i < keys.length; i++) {
            Object v = vals[i];
            if (v instanceof Object[]) {
                JSONArray arr = new JSONArray();
                for (Object o : (Object[]) v) {
                    arr.put(o);
                }
                inputs.put(keys[i], arr);
            } else {
                inputs.put(keys[i], v);
            }
        }
        n.put("inputs", inputs);
        return n;
    }

    /**
     * 向 ComfyUI 提交任务。
     *
     * 原实现把异常吞掉只返回 null，调用方只能报一句"模型名不对" ——
     * 真正的原因（超时 / 连接被拒 / JSON 错误）全被埋掉，排查全靠猜。
     * 现在把失败原因抛出来，让用户看到具体问题。
     */
    /**
     * 提交任务到 ComfyUI。
     *
     * 走统一的 Http 层 —— `Expect: 100-continue` 导致的 POST 永久挂起
     * 和超时设置都在那里处理了，这里只关心业务语义。
     */
    private static String postJson(String urlStr, String json) throws Exception {
        String body = Http.postJson(urlStr, json).orThrow("ComfyUI").body;
        if (body == null || body.trim().isEmpty()) {
            throw new Exception("ComfyUI 返回了空响应");
        }
        try {
            JSONObject root = new JSONObject(body);
            String id = root.optString("prompt_id", "");
            if (id.isEmpty()) {
                throw new Exception("ComfyUI 没有返回 prompt_id");
            }
            return id;
        } catch (org.json.JSONException e) {
            throw new Exception("无法解析 ComfyUI 响应：" + cut(body));
        }
    }
    /** 错误信息截断，避免把一整页 HTML 塞进 UI */
    private static String cut(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
    private static Bitmap downloadImage(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        try {
            try (InputStream is = conn.getInputStream()) {
                Bitmap b = android.graphics.BitmapFactory.decodeStream(is);
                if (b == null) {
                    throw new Exception("取回的图无效");
                }
                return b;
            }
        } finally {
            conn.disconnect();
        }
    }

    /** 缓存文件路径（三个渠道共用） */
    public static File cacheFile(Context ctx, String key) {
        File dir = new File(ctx.getFilesDir(), "genimg");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safe = key.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safe.length() > 60) {
            safe = safe.substring(0, 60);
        }
        return new File(dir, safe + ".jpg");
    }

    public static int clearCache(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "genimg");
        if (!dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        int n = 0;
        if (files != null) {
            for (File f : files) {
                if (f.delete()) {
                    n++;
                }
            }
        }
        return n;
    }

    public static long cacheSizeKB(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "genimg");
        if (!dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles();
        long total = 0;
        if (files != null) {
            for (File f : files) {
                total += f.length();
            }
        }
        return total / 1024;
    }
}
