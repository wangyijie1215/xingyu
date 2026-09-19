#!/usr/bin/env python3
"""
星语 语音识别服务 —— 在电脑上跑，给手机提供语音转文字。

为什么需要：
    手机上用**系统自带**的语音识别就够了（零配置、离线可用）。
    这个服务是给"识别质量不够"的场景准备的 ——
    比如经常听错人名、中英混说、有专有名词。
    whisper 在这些方面明显好于系统自带。

用法：
    python stt-server.py                  # 监听 0.0.0.0:5002
    python stt-server.py --model medium   # 换更大的模型（更准、更慢）
    python stt-server.py --port 8080

依赖：
    pip install faster-whisper

    faster-whisper 比 openai-whisper 快好几倍，且显存占用更低。
    有 NVIDIA 显卡时会自动用 CUDA，没有就用 CPU。

接口：
    GET  /health
        返回 {"ok": true, "model": "small", "device": "cuda"}

    POST /transcribe?model=small&language=zh
        body: 原始音频字节（wav / m4a / mp3 都行）
        返回 {"text": "识别出来的文字"}

模型大小参考（中文）：
    tiny    ~75MB   快但常错，只适合试通
    base    ~145MB  勉强能用
    small   ~490MB  **推荐**，准确度和速度平衡
    medium  ~1.5GB  更准，慢一倍
    large-v3 ~3GB   最准，需要好显卡

注意：必须监听 0.0.0.0 而不是 127.0.0.1，否则手机连不上。
"""

import argparse
import io
import json
import os
import sys
import tempfile
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    from faster_whisper import WhisperModel
except ImportError:
    print("缺少依赖。请先运行：pip install faster-whisper")
    sys.exit(1)

MODEL = None
MODEL_NAME = "small"
DEVICE = "cpu"


def load_model(name):
    """加载模型。优先用 CUDA，失败退回 CPU。"""
    global MODEL, MODEL_NAME, DEVICE
    if MODEL is not None:
        return MODEL

    try:
        MODEL = WhisperModel(name, device="cuda", compute_type="float16")
        DEVICE = "cuda"
        print("使用 GPU（CUDA float16）")
    except Exception as e:
        print("GPU 不可用（%s），改用 CPU" % str(e)[:80])
        MODEL = WhisperModel(name, device="cpu", compute_type="int8")
        DEVICE = "cpu"
    MODEL_NAME = name
    return MODEL


def transcribe(audio_bytes, fmt, language):
    """把音频字节转成文字"""
    m = load_model(MODEL_NAME)
    # faster-whisper 需要文件路径或 file-like，写个临时文件最省事
    suffix = "." + (fmt or "m4a")
    fd, path = tempfile.mkstemp(suffix=suffix)
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(audio_bytes)
        segments, info = m.transcribe(
            path,
            language=language if language and language != "auto" else None,
            beam_size=5,
            vad_filter=True,   # 去掉静音段，明显提速
        )
        text = "".join(seg.text for seg in segments).strip()
        return text
    finally:
        try:
            os.unlink(path)
        except Exception:
            pass


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
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
        if parsed.path == "/health":
            self._send(200, json.dumps({
                "ok": True, "model": MODEL_NAME, "device": DEVICE,
                "loaded": MODEL is not None,
            }))
            return
        self._send(404, json.dumps({"error": "not found"}))

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        qs = urllib.parse.parse_qs(parsed.query)

        if parsed.path != "/transcribe":
            self._send(404, json.dumps({"error": "not found"}))
            return

        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            self._send(400, json.dumps({"error": "empty body"}))
            return
        if length > 40 * 1024 * 1024:
            self._send(413, json.dumps({"error": "音频太大（上限 40MB）"}))
            return

        audio = self.rfile.read(length)
        fmt = (qs.get("format") or ["m4a"])[0]
        language = (qs.get("language") or ["zh"])[0]

        # 允许每次请求指定模型（首次会触发加载，比较慢）
        want = (qs.get("model") or [None])[0]
        if want and want != MODEL_NAME:
            global MODEL
            MODEL = None
            load_model(want)

        try:
            text = transcribe(audio, fmt, language)
            self._send(200, json.dumps({"text": text}, ensure_ascii=False))
        except Exception as e:
            self._send(500, json.dumps({"error": str(e)}, ensure_ascii=False))


def main():
    ap = argparse.ArgumentParser(description="星语 语音识别服务")
    ap.add_argument("--host", default="0.0.0.0",
                    help="监听地址，默认 0.0.0.0（手机才能连上）")
    ap.add_argument("--port", type=int, default=5002)
    ap.add_argument("--model", default="small",
                    help="tiny / base / small / medium / large-v3")
    ap.add_argument("--no-preload", action="store_true",
                    help="启动时不预加载模型（第一次请求会慢）")
    args = ap.parse_args()

    if not args.no_preload:
        print("正在加载模型 %s …（第一次会下载，几百 MB）" % args.model)
        load_model(args.model)
        print("模型就绪")

    srv = ThreadingHTTPServer((args.host, args.port), Handler)
    port = srv.server_address[1]
    print()
    print("星语 语音识别服务已启动")
    print("  监听：%s:%d" % (args.host, port))
    print("  模型：%s（%s）" % (MODEL_NAME, DEVICE))
    print("  模拟器填：10.0.2.2:%d" % port)
    print("  真机填：<本机局域网IP>:%d" % port)
    print()
    print("提示：手机上其实用「系统自带」的语音识别通常就够。")
    print("      这个服务是给识别质量要求更高的场景准备的。")
    print()
    print("按 Ctrl+C 停止")
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")


if __name__ == "__main__":
    main()
