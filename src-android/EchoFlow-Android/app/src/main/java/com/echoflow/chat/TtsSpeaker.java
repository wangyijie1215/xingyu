package com.echoflow.chat;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * 说话 —— 把角色的台词读出来。
 *
 * 设计要点：
 *
 *  1. **缓存优先**。同一句话只合一次。这在通话场景下很关键：
 *     角色常说的「嗯」「我在」「怎么了」如果每次都请求一次，
 *     延迟会很烦人。缓存后是即时的。
 *
 *  2. **网络失败要降级，不能哑掉**。远端 TTS 连不上时自动退回手机自带 TTS。
 *     自带音色机械，但比"点了没反应"好。
 *
 *  3. **说完要回调**。通话界面靠这个回调推进下一句，
 *     所以无论走哪条路径都必须回调，包括失败路径。
 *
 *  4. **可打断**。用户按挂断或角色说下一句时，正在播的必须立刻停。
 */
public class TtsSpeaker {

    private static final String TAG = "TtsSpeaker";

    public interface Callback {
        /** 播完了（或失败但已经"结束"了）——两种都要回调，否则界面会卡住 */
        void onDone();

        /** 开始播了，可以在这里做口型/动画 */
        void onStart();
    }

    private final Context ctx;
    private final Handler ui = new Handler(Looper.getMainLooper());

    private MediaPlayer player;
    private TextToSpeech systemTts;
    private boolean systemTtsReady = false;
    private boolean systemTtsInit = false;
    private Callback pending;

    /** 当前是否在播 */
    private volatile boolean speaking = false;

    public TtsSpeaker(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public boolean isSpeaking() {
        return speaking;
    }

    // ==================================================================
    // 主入口
    // ==================================================================

    /**
     * 说一句话。回调保证被调用一次。
     *
     * @param text  要念的文字（会自动去掉括号里的动作描写）
     * @param voice 音色 id；空则用配置里的
     */
    public void speak(String text, String voice, Callback cb) {
        TtsProvider.Config cfg = TtsProvider.load(ctx);
        if (!cfg.enabled) {
            if (cb != null) {
                cb.onDone();
            }
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            if (cb != null) {
                cb.onDone();
            }
            return;
        }

        stop();   // 先停掉上一句
        this.pending = cb;

        final String v = (voice == null || voice.isEmpty()) ? cfg.voice : voice;
        final String key = cacheKey(text, v, cfg);
        final File cached = cacheFile(key);

        // ---- 1) 缓存命中：直接播 ----
        if (cached.exists() && cached.length() > 0) {
            playFile(cached, cb);
            return;
        }

        // ---- 2) 系统 TTS：不需要网络，走另一条路 ----
        if (TtsProvider.KIND_SYSTEM.equals(cfg.kind)) {
            speakSystem(text, cb);
            return;
        }

        // ---- 3) 远端合成：后台线程，失败退回系统 TTS ----
        new Thread(() -> {
            try {
                byte[] audio = TtsProvider.synthesize(ctx, cfg, text, v);
                if (audio == null || audio.length == 0) {
                    throw new Exception("返回空音频");
                }
                try (FileOutputStream fos = new FileOutputStream(cached)) {
                    fos.write(audio);
                }
                ui.post(() -> playFile(cached, cb));
            } catch (Exception e) {
                Log.w(TAG, "远端 TTS 失败，退回系统 TTS：" + e.getMessage());
                ui.post(() -> speakSystem(text, cb));
            }
        }).start();
    }

    // ==================================================================
    // 播放文件
    // ==================================================================

    private void playFile(File f, Callback cb) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mp.setDataSource(f.getAbsolutePath());
            mp.setOnPreparedListener(m -> {
                player = mp;
                speaking = true;
                if (cb != null) {
                    cb.onStart();
                }
                mp.start();
            });
            mp.setOnCompletionListener(m -> {
                speaking = false;
                releasePlayer();
                if (cb != null) {
                    cb.onDone();
                }
            });
            mp.setOnErrorListener((m, what, extra) -> {
                Log.w(TAG, "播放失败 what=" + what + " extra=" + extra);
                speaking = false;
                releasePlayer();
                if (cb != null) {
                    cb.onDone();
                }
                return true;
            });
            mp.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "MediaPlayer 创建失败：" + e.getMessage());
            speaking = false;
            if (cb != null) {
                cb.onDone();
            }
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignore) {
            }
            player = null;
        }
    }

    // ==================================================================
    // 系统 TTS（兜底）
    // ==================================================================

    private void speakSystem(String text, Callback cb) {
        final String clean = strip(text);
        if (systemTts == null) {
            systemTts = new TextToSpeech(ctx, status -> {
                systemTtsReady = (status == TextToSpeech.SUCCESS);
                if (systemTtsReady) {
                    systemTts.setLanguage(Locale.CHINA);
                    systemTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String id) {
                            // 不需要额外处理
                        }

                        @Override
                        public void onDone(String id) {
                            ui.post(() -> {
                                speaking = false;
                                Callback c = pending;
                                pending = null;
                                if (c != null) {
                                    c.onDone();
                                }
                            });
                        }

                        @Override
                        public void onError(String id) {
                            onDone(id);
                        }
                    });
                } else {
                    // 系统 TTS 也不可用 —— 还是要回调，否则界面卡死
                    Log.w(TAG, "系统 TTS 初始化失败");
                    ui.post(() -> {
                        speaking = false;
                        if (cb != null) {
                            cb.onDone();
                        }
                    });
                }
            });
        }

        // 初始化是异步的，这里轮询等它就绪（最多 2 秒）
        waitThenSpeak(clean, cb, 0);
    }

    private void waitThenSpeak(String text, Callback cb, int tries) {
        if (systemTtsReady) {
            speaking = true;
            if (cb != null) {
                cb.onStart();
            }
            pending = cb;
            systemTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ef_" + System.nanoTime());
            return;
        }
        if (tries > 20) {
            // 等不到就放弃，但必须回调
            speaking = false;
            if (cb != null) {
                cb.onDone();
            }
            return;
        }
        ui.postDelayed(() -> waitThenSpeak(text, cb, tries + 1), 100);
    }

    // ==================================================================
    // 停止 / 释放
    // ==================================================================

    /** 立刻停止当前播放（不触发 onDone —— 调用方正在做别的事） */
    public void stop() {
        speaking = false;
        releasePlayer();
        if (systemTts != null) {
            try {
                systemTts.stop();
            } catch (Exception ignore) {
            }
        }
        pending = null;
    }

    public void shutdown() {
        stop();
        if (systemTts != null) {
            try {
                systemTts.shutdown();
            } catch (Exception ignore) {
            }
            systemTts = null;
        }
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** 去掉不该念出来的部分：括号里的动作、星号包裹的旁白 */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("（[^）]*）", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\*[^*]*\\*", "")
                .replaceAll("\\[[^\\]]*\\]", "")
                .replaceAll("[「」『』]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cacheKey(String text, String voice, TtsProvider.Config cfg) {
        return voice + "|" + cfg.rate + "|" + cfg.pitch + "|" + strip(text);
    }

    private File cacheFile(String key) {
        File dir = new File(ctx.getFilesDir(), "tts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safe = Integer.toHexString(key.hashCode()) + "_" + (key.length() % 997);
        return new File(dir, safe + ".mp3");
    }

    /** 清缓存，返回清掉的个数 */
    public static int clearCache(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "tts");
        if (!dir.exists()) {
            return 0;
        }
        File[] fs = dir.listFiles();
        int n = 0;
        if (fs != null) {
            for (File f : fs) {
                if (f.delete()) {
                    n++;
                }
            }
        }
        return n;
    }

    public static long cacheSizeKB(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "tts");
        if (!dir.exists()) {
            return 0;
        }
        File[] fs = dir.listFiles();
        long total = 0;
        if (fs != null) {
            for (File f : fs) {
                total += f.length();
            }
        }
        return total / 1024;
    }
}
