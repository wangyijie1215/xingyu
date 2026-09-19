package com.echoflow.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OpenAI 兼容 SSE 流式客户端。
 *
 * 同时支持两种目标：
 *  · 云端（Grok / OpenAI / 中转）—— 强制 https，需要 Bearer Key
 *  · 本地 Ollama —— http 明文，无需 Key，端点同为 /v1/chat/completions
 *
 * 原实现无条件拼 "https://"，因此本地 Ollama 永远连不上（http 服务）。
 * 这里改为：只有当用户没写协议时才补默认协议，并按 Provider 类型决定默认值。
 */
public class ApiClient {

    public interface Callback {
        void onChunk(String fullText);

        void onDone(boolean error, String errMsg);
    }

    /** 兼容旧签名：默认走 https（云端） */
    public static void streamChat(String baseUrl, String apiKey, String model,
                                  List<Message> messages, Callback cb) {
        streamChat(baseUrl, apiKey, model, messages, false, cb);
    }

    /**
     * @param local true = 本地 Ollama（http、可无 Key）；false = 云端（https）
     */
    public static void streamChat(String baseUrl, String apiKey, String model,
                                  List<Message> messages, boolean local, Callback cb) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(buildEndpoint(baseUrl, local, "/v1/chat/completions"));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "text/event-stream");
                if (apiKey != null && !apiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                conn.setDoOutput(true);
                conn.setConnectTimeout(local ? 15000 : 30000);
                // 本地模型首包较慢（要加载权重），读超时给足
                conn.setReadTimeout(local ? 300000 : 60000);

                JSONObject body = new JSONObject();
                body.put("model", model);
                JSONArray arr = new JSONArray();
                for (Message m : messages) {
                    JSONObject item = new JSONObject();
                    item.put("role", m.role);
                    item.put("content", m.content);
                    arr.put(item);
                }
                body.put("messages", arr);
                body.put("stream", true);
                body.put("temperature", 0.7);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    String err = readAll(conn.getErrorStream());
                    cb.onDone(true, "HTTP " + code + (err.isEmpty() ? "" : ": " + err));
                    return;
                }

                StringBuilder full = new StringBuilder();
                boolean gotData = false;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String t = line.trim();
                        if (!t.startsWith("data:")) {
                            continue;
                        }
                        String data = t.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            JSONObject obj = new JSONObject(data);
                            JSONArray choices = obj.optJSONArray("choices");
                            if (choices != null && choices.length() > 0) {
                                JSONObject deltaObj = choices.optJSONObject(0);
                                if (deltaObj != null) {
                                    JSONObject delta = deltaObj.optJSONObject("delta");
                                    String content = delta == null ? "" : delta.optString("content", "");
                                    if (!content.isEmpty()) {
                                        full.append(content);
                                        gotData = true;
                                        cb.onChunk(full.toString());
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                            // 跳过不完整 JSON 分片
                        }
                    }
                }
                if (gotData) {
                    cb.onDone(false, null);
                } else {
                    cb.onDone(true, local
                            ? "本地模型没有返回内容。可能模型还在加载，或该模型不支持流式输出。"
                            : "接口未返回流式数据，请检查 API 地址是否正确（应为根域名，不要带 /keys）");
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "网络异常" : e.getMessage();
                if (local) {
                    msg = "连接本地 Ollama 失败：" + msg
                            + "\n请确认电脑上已运行 ollama serve，且地址可达。";
                }
                cb.onDone(true, msg);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    /**
     * 拼接请求端点。
     *
     * 规则：
     *  · 用户写了协议（http:// 或 https://）→ 原样保留
     *  · 没写 → 本地补 http://，云端补 https://
     *  · baseUrl 已经带了完整路径 → 不再追加
     */
    static String buildEndpoint(String baseUrl, boolean local, String path) {
        String clean = baseUrl == null ? "" : baseUrl;
        clean = clean.replaceAll("[\\p{Cntrl}\\s\\u00a0\\u200b\\u200c\\u200d\\ufeff]", "");

        boolean hasScheme = clean.matches("(?i)^https?://.*");
        String scheme;
        String host;
        if (hasScheme) {
            int idx = clean.indexOf("://");
            scheme = clean.substring(0, idx + 3);
            host = clean.substring(idx + 3);
        } else {
            scheme = local ? "http://" : "https://";
            host = clean;
        }
        while (host.startsWith("/")) {
            host = host.substring(1);
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.endsWith("/v1/chat/completions")) {
            return scheme + host;
        }
        if (host.endsWith("/v1")) {
            return scheme + host + "/chat/completions";
        }
        return scheme + host + path;
    }

    private static String readAll(InputStream is) {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignore) {
        }
        return sb.toString();
    }
}
