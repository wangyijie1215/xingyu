package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provider 配置：支持云端 OpenAI 兼容服务与**本地 Ollama**。
 *
 * 为什么要单独处理 Ollama：
 *  1. 它是本地服务，没有 API Key，走 http 而不是 https（原 ApiClient 强制 https）
 *  2. 端口固定 11434，聊天端点是 /v1/chat/completions（OpenAI 兼容层）
 *  3. 模型名要动态从 /api/tags 拉，不能写死
 *  4. 手机与电脑不在同一台机器时，baseUrl 要能填局域网 IP
 */
public class ProviderStore {

    private static final String PREFS = "echoflow_provider";
    private static final String KEY_KIND = "provider_kind";
    private static final String KEY_BASE = "provider_base";
    private static final String KEY_MODEL = "provider_model";

    /** cloud = 云端 OpenAI 兼容；ollama = 本地 Ollama */
    public static final String KIND_CLOUD = "cloud";
    public static final String KIND_OLLAMA = "ollama";

    /** Ollama 默认监听地址（模拟器访问宿主机用 10.0.2.2） */
    public static final String OLLAMA_HOST_DEFAULT = "127.0.0.1:11434";
    public static final String OLLAMA_HOST_EMULATOR = "10.0.2.2:11434";

    public static String kind(Context ctx) {
        return prefs(ctx).getString(KEY_KIND, KIND_CLOUD);
    }

    public static boolean isOllama(Context ctx) {
        return KIND_OLLAMA.equals(kind(ctx));
    }

    public static void setKind(Context ctx, String kind) {
        prefs(ctx).edit().putString(KEY_KIND, kind).apply();
    }

    public static String baseUrl(Context ctx) {
        return prefs(ctx).getString(KEY_BASE, "");
    }

    public static String model(Context ctx) {
        return prefs(ctx).getString(KEY_MODEL, "");
    }

    public static void save(Context ctx, String kind, String baseUrl, String model) {
        prefs(ctx).edit()
                .putString(KEY_KIND, kind)
                .putString(KEY_BASE, baseUrl == null ? "" : baseUrl.trim())
                .putString(KEY_MODEL, model == null ? "" : model.trim())
                .apply();
    }

    /** 当前生效的 baseUrl：Provider 未配置时回退到旧的全局配置 */
    public static String effectiveBaseUrl(Context ctx) {
        String b = baseUrl(ctx);
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return isOllama(ctx) ? OLLAMA_HOST_DEFAULT : ChatStore.getBaseUrl(ctx);
    }

    public static String effectiveModel(Context ctx) {
        String m = model(ctx);
        if (m != null && !m.isEmpty()) {
            return m;
        }
        return isOllama(ctx) ? "qwen2.5:7b" : ChatStore.getModel(ctx);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ==================================================================
    // Ollama 探测
    // ==================================================================

    public static class Probe {
        public boolean ok;
        public String message = "";
        /** 可用模型名列表 */
        public List<String> models = new ArrayList<>();
        /** 实际使用的 host（可能做了 10.0.2.2 回退） */
        public String host = "";
    }

    /**
     * 探测本机 / 局域网上的 Ollama。
     *
     * 依次尝试候选地址，任何一个能返回模型列表就算成功。
     * 注意必须在后台线程调用。
     *
     * @param host 用户填的 host（可含 http:// 前缀或末尾斜杠，会被清洗）
     */
    public static Probe probe(String host) {
        Probe p = new Probe();
        List<String> candidates = new ArrayList<>();
        String h = cleanHost(host);
        if (!h.isEmpty()) {
            candidates.add(h);
        }
        // 常见的兜底地址：模拟器宿主机、真机本机
        candidates.add(OLLAMA_HOST_EMULATOR);
        candidates.add(OLLAMA_HOST_DEFAULT);
        candidates.add("localhost:11434");

        for (String c : candidates) {
            try {
                List<String> models = fetchModels(c);
                if (!models.isEmpty()) {
                    p.ok = true;
                    p.host = c;
                    p.models = models;
                    p.message = "已连接 · 发现 " + models.size() + " 个模型";
                    return p;
                }
            } catch (Exception e) {
                // 换下一个候选
            }
        }
        p.ok = false;
        p.message = "连不上 Ollama。请确认电脑上已运行 `ollama serve`，"
                + "且手机与电脑在同一网络（真机需填电脑局域网 IP）。";
        return p;
    }

    /** 拉取模型列表：GET http://host/api/tags */
    public static List<String> fetchModels(String host) throws Exception {
        String url = "http://" + cleanHost(host) + "/api/tags";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(8000);
        try {
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP " + code);
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
            JSONArray arr = root.optJSONArray("models");
            List<String> out = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.optJSONObject(i);
                    if (m == null) {
                        continue;
                    }
                    String name = m.optString("name", "");
                    if (name.isEmpty()) {
                        name = m.optString("model", "");
                    }
                    if (!name.isEmpty()) {
                        out.add(name);
                    }
                }
            }
            return out;
        } finally {
            conn.disconnect();
        }
    }

    /** 清洗用户输入的 host：去空白、去协议前缀、去尾部斜杠 */
    public static String cleanHost(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replaceAll("[\\p{Cntrl}\\s\\u00a0\\u200b\\ufeff]", "");
        s = s.replaceAll("^(https?://)+", "");
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        // 去掉误填的路径（Ollama 只要 host:port）
        int slash = s.indexOf('/');
        if (slash > 0) {
            s = s.substring(0, slash);
        }
        return s;
    }
}
