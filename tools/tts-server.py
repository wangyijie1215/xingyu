#!/usr/bin/env python3
"""
星语 TTS 服务 —— 在电脑上跑，给手机提供语音合成。

为什么需要一个服务：
    edge-tts 是 Python 库，手机上没有。让电脑跑一个极小的 HTTP 服务，
    App 通过局域网请求，就能用上电脑上的 322 个神经音色。

用法：
    python tts-server.py                  # 监听 0.0.0.0:5001
    python tts-server.py --port 8080
    python tts-server.py --host 0.0.0.0

依赖：
    pip install edge-tts aiohttp

接口：
    GET /health
        返回 {"ok": true, "voices": 322}

    GET /voices
        返回可用音色列表

    GET /tts?text=你好&voice=zh-CN-XiaoxiaoNeural&rate=+0%&pitch=+0Hz
        返回 audio/mpeg 音频字节

注意：必须监听 0.0.0.0 而不是 127.0.0.1，否则手机连不上。
这一点和 ComfyUI 是同一个坑。
"""

import argparse
import asyncio
import json
import sys
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    import edge_tts
except ImportError:
    print("缺少依赖。请先运行：pip install edge-tts")
    sys.exit(1)

DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"

# 预热的音色清单（避免每次请求都去查）
VOICE_CACHE = None


async def list_voices():
    global VOICE_CACHE
    if VOICE_CACHE is None:
        voices = await edge_tts.list_voices()
        VOICE_CACHE = [
            {
                "id": v.get("ShortName", ""),
                "gender": v.get("Gender", ""),
                "locale": v.get("Locale", ""),
                "friendly": v.get("FriendlyName", ""),
            }
            for v in voices
        ]
    return VOICE_CACHE


async def synth(text, voice, rate, pitch):
    """合成音频，返回 bytes"""
    communicate = edge_tts.Communicate(text, voice, rate=rate, pitch=pitch)
    buf = bytearray()
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            buf.extend(chunk["data"])
    return bytes(buf)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        # 只打一行简洁的日志
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _send(self, code, body, ctype="application/json; charset=utf-8"):
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        qs = urllib.parse.parse_qs(parsed.query)

        # ---- 健康检查 ----
        if path == "/health":
            try:
                voices = asyncio.run(list_voices())
                n = len(voices)
            except Exception:
                n = -1
            self._send(200, json.dumps({"ok": True, "voices": n}))
            return

        # ---- 音色列表 ----
        if path == "/voices":
            try:
                voices = asyncio.run(list_voices())
                self._send(200, json.dumps(voices, ensure_ascii=False))
            except Exception as e:
                self._send(500, json.dumps({"error": str(e)}))
            return

        # ---- 合成 ----
        if path == "/tts":
            text = (qs.get("text") or [""])[0].strip()
            if not text:
                self._send(400, json.dumps({"error": "missing text"}))
                return
            voice = (qs.get("voice") or [DEFAULT_VOICE])[0]
            rate = (qs.get("rate") or ["+0%"])[0]
            pitch = (qs.get("pitch") or ["+0Hz"])[0]

            # 长度保护：太长的文本截断，避免手机侧等太久
            if len(text) > 500:
                text = text[:500]

            try:
                audio = asyncio.run(synth(text, voice, rate, pitch))
                if not audio:
                    self._send(500, json.dumps({"error": "empty audio"}))
                    return
                self._send(200, audio, "audio/mpeg")
            except Exception as e:
                self._send(500, json.dumps({"error": str(e)}, ensure_ascii=False))
            return

        self._send(404, json.dumps({"error": "not found"}))


def main():
    ap = argparse.ArgumentParser(description="星语 TTS 服务")
    ap.add_argument("--host", default="0.0.0.0",
                    help="监听地址，默认 0.0.0.0（手机才能连上）")
    ap.add_argument("--port", type=int, default=5001)
    args = ap.parse_args()

    # 预热音色清单
    try:
        voices = asyncio.run(list_voices())
        print("已加载 %d 个音色" % len(voices))
        cn = [v for v in voices if v["locale"].startswith("zh-CN")]
        print("中文音色：")
        for v in cn:
            print("  %s  %s" % (v["id"], v["gender"]))
    except Exception as e:
        print("警告：拉取音色列表失败（%s），合成时可能也会失败" % e)
        print("请检查网络：edge-tts 需要访问微软的在线服务")

    srv = ThreadingHTTPServer((args.host, args.port), Handler)
    port = srv.server_address[1]
    print()
    print("星语 TTS 服务已启动")
    print("  监听：%s:%d" % (args.host, port))
    print("  模拟器填：10.0.2.2:%d" % port)
    print("  真机填：<本机局域网IP>:%d" % port)
    print()
    print("按 Ctrl+C 停止")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")


if __name__ == "__main__":
    main()
