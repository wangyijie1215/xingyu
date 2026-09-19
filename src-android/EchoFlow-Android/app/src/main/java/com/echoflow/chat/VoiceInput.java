package com.echoflow.chat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * 按住说话 —— 把语音转成文字。
 *
 * 默认走 **Android 自带的 SpeechRecognizer**：
 *   · 不用装东西、不用 API Key、不用连电脑
 *   · 取决于手机是否装了离线语言包；没有的话会走 Google 云端
 *   · 识别质量日常够用
 *
 * 如果不满意（经常听错人名、中英混说），可以在设置里切到本地 whisper。
 *
 * ── 几个实践要点 ────────────────────────────────
 *
 * 1. **必须动态申请 RECORD_AUDIO**。它是危险权限，清单里声明了也还要运行时问。
 * 2. **每次说完要 destroy() 再重建**。SpeechRecognizer 用完不释放，
 *    下一次 startListening 会直接回调 onError(ERROR_RECOGNIZER_BUSY)。
 * 3. **模拟器上没有麦克风输入**，会停在「准备好了」不动 —— 这是正常的，
 *    真机上才能测出实际效果。
 */
public class VoiceInput {

    private static final String TAG = "VoiceInput";

    public interface Callback {
        /** 开始录音（用于界面反馈） */
        void onReady();

        /** 用户正在说话，level 是音量 0~1 */
        void onLevel(float level);

        /** 识别出结果 */
        void onResult(String text);

        /** 失败或用户取消 */
        void onError(String msg);
    }

    private final Context ctx;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private SpeechRecognizer recognizer;
    private Callback callback;
    private boolean listening = false;

    public VoiceInput(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public boolean isListening() {
        return listening;
    }

    /** 有没有录音权限 */
    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** 这台设备能不能做语音识别（有服务且支持中文） */
    public static boolean isAvailable(Context ctx) {
        try {
            return SpeechRecognizer.isRecognitionAvailable(ctx);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================================================================

    /** 开始听。调用前务必确认有权限。 */
    public void start(Callback cb) {
        this.callback = cb;
        if (!hasPermission(ctx)) {
            fail("没有录音权限");
            return;
        }
        if (!isAvailable(ctx)) {
            fail("这台设备没有可用的语音识别服务");
            return;
        }

        // 关键：每次都重建。复用同一个实例会有 RECOGNIZER_BUSY 问题
        destroy();

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(ctx);
        } catch (Exception e) {
            fail("无法创建语音识别：" + e.getMessage());
            return;
        }

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                listening = true;
                ui.post(() -> {
                    if (callback != null) {
                        callback.onReady();
                    }
                });
            }

            @Override
            public void onBeginningOfSpeech() {
                // 忽略
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // rmsdB 大致在 -2 ~ 10 之间，归一化成 0~1
                final float level = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
                ui.post(() -> {
                    if (callback != null) {
                        callback.onLevel(level);
                    }
                });
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 忽略
            }

            @Override
            public void onEndOfSpeech() {
                listening = false;
            }

            @Override
            public void onError(int error) {
                listening = false;
                ui.post(() -> {
                    if (callback != null) {
                        callback.onError(describe(error));
                    }
                });
            }

            @Override
            public void onResults(Bundle results) {
                listening = false;
                ArrayList<String> list =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (list == null || list.isEmpty()) ? "" : list.get(0);
                ui.post(() -> {
                    if (callback != null) {
                        if (text == null || text.trim().isEmpty()) {
                            callback.onError("没听清，再说一次？");
                        } else {
                            callback.onResult(text.trim());
                        }
                    }
                });
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // 可以在这里做实时预览，现在用不上
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // 忽略
            }
        });

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            SttProvider.Config cfg = SttProvider.load(ctx);
            if (cfg.language != null && !cfg.language.isEmpty()) {
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, cfg.language);
                // 中文识别时把语言偏好也带上，权重更高
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, cfg.language);
            }
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            // 说完自动停，不用手动点结束
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500L);
            recognizer.startListening(intent);
            listening = true;
        } catch (Exception e) {
            fail("启动识别失败：" + e.getMessage());
        }
    }

    /** 用户提前松手：告诉识别器说完了，等 onResults */
    public void stop() {
        if (recognizer != null && listening) {
            try {
                recognizer.stopListening();
            } catch (Exception e) {
                Log.w(TAG, "stopListening 失败：" + e.getMessage());
            }
        }
    }

    /** 取消（不要结果） */
    public void cancel() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (Exception ignore) {
            }
        }
        destroy();
    }

    /** 释放。用完必须调，否则下次 startListening 会报 BUSY */
    public void destroy() {
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (Exception ignore) {
            }
            recognizer = null;
        }
    }

    // ==================================================================

    private void fail(String msg) {
        listening = false;
        ui.post(() -> {
            if (callback != null) {
                callback.onError(msg);
            }
        });
    }

    /** 把错误码翻译成人能看懂的话 */
    private String describe(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "录音出错";
            case SpeechRecognizer.ERROR_CLIENT:
                return "识别被中断";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "没有录音权限";
            case SpeechRecognizer.ERROR_NETWORK:
                return "网络错误（在线识别需要联网）";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "网络超时";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "没听清，再说一次？";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "识别服务忙，稍后再试";
            case SpeechRecognizer.ERROR_SERVER:
                return "识别服务出错";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "没听到声音";
            default:
                return "识别失败（错误码 " + error + "）";
        }
    }
}
