package com.echoflow.chat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 统一的 HTTP 客户端。
 *
 * 为什么要抽这一层：这个项目里有 16 处手写 HttpURLConnection，
 * 结果同一个坑犯了两遍 —— 加 CosyVoice 时我在 ImageProvider 里
 * 刚修好 `Expect: 100-continue` 导致的 POST 永久挂起，
 * 转头在 TtsProvider 里又原样写了一遍。
 *
 * 集中到一处之后，这类修复只需要做一次。
 *
 * ── 两个必须记住的坑 ──────────────────────────────
 *
 * 1. **`Expect: ""` 不能省。**
 *    Android 对较大的 POST 会自动加 `Expect: 100-continue`，
 *    先等服务器回 `100 Continue` 再发正文。有些服务端（或中间的代理）
 *    根本不回，请求就永远卡在"发送中"——而且 **setReadTimeout 管不到这一段**，
 *    表现是"没有报错、也没有结果"，排查起来很费劲。
 *
 * 2. **要显式设 Content-Length。**
 *    走固定长度模式，避免分块传输再引入一层不确定性。
 */
public class Http {

    /** 默认连接超时：够局域网握手，也不至于让用户等太久 */
    public static final int CONNECT_TIMEOUT = 10000;
    /** 默认读取超时：生图/大模型可能很慢，给足 */
    public static final int READ_TIMEOUT = 120000;

    public static class Response {
        public final int code;
        public final String body;
        public final byte[] bytes;

        Response(int code, String body, byte[] bytes) {
            this.code = code;
            this.body = body;
            this.bytes = bytes;
        }

        public boolean ok() {
            return code >= 200 && code < 300;
        }

        /** 非 2xx 时抛异常，异常信息里带服务端返回的内容 */
        public Response orThrow(String what) throws Exception {
            if (!ok()) {
                String detail = body == null ? "" : body;
                if (detail.length() > 300) {
                    detail = detail.substring(0, 300);
                }
                throw new Exception(what + " 返回 HTTP " + code
                        + (detail.isEmpty() ? "" : "：" + detail));
            }
            return this;
        }
    }

    // ==================================================================
    // GET
    // ==================================================================

    public static Response get(String url) throws Exception {
        return get(url, null, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    public static Response get(String url, String bearer, int connectMs, int readMs)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);
        conn.setRequestProperty("Expect", "");
        if (bearer != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        try {
            return read(conn);
        } finally {
            conn.disconnect();
        }
    }

    // ==================================================================
    // POST
    // ==================================================================

    public static Response postJson(String url, String json) throws Exception {
        return postJson(url, json, null, null, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    public static Response postJson(String url, String json, String bearer) throws Exception {
        return postJson(url, json, bearer, null, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * @param bearer      可空
     * @param extraHeader 可空，格式 "Key: Value"（目前只需要一个，写简单点）
     */
    public static Response postJson(String url, String json, String bearer,
                                    String extraHeader, int connectMs, int readMs)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        // 坑 1：不加这行，大 POST 会永远挂起
        conn.setRequestProperty("Expect", "");
        if (bearer != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        if (extraHeader != null && !extraHeader.isEmpty()) {
            int c = extraHeader.indexOf(':');
            if (c > 0) {
                conn.setRequestProperty(extraHeader.substring(0, c).trim(),
                        extraHeader.substring(c + 1).trim());
            }
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectMs);
        conn.setReadTimeout(readMs);

        byte[] payload = json.getBytes("UTF-8");
        // 坑 2：显式长度，走固定长度模式
        conn.setFixedLengthStreamingMode(payload.length);
        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }
            return read(conn);
        } finally {
            conn.disconnect();
        }
    }

    // ==================================================================
    // 读响应
    // ==================================================================

    private static Response read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        byte[] bytes = new byte[0];
        if (is != null) {
            try {
                bytes = readAll(is);
            } finally {
                is.close();
            }
        }

        // 音频之类的二进制不要试图转成字符串（会破坏数据）
        String contentType = conn.getContentType();
        String body = null;
        if (bytes.length > 0) {
            if (contentType != null && (contentType.startsWith("audio")
                    || contentType.startsWith("image")
                    || contentType.contains("octet-stream"))) {
                body = null;
            } else {
                body = new String(bytes, "UTF-8");
            }
        }
        return new Response(code, body, bytes);
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /** 读文本行，给需要逐行解析的场合用 */
    public static String readLines(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
