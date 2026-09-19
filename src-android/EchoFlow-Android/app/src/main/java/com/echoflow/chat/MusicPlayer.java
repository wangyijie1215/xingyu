package com.echoflow.chat;

import android.media.MediaPlayer;

/**
 * 音乐播放器 —— 内置一首曲子。
 *
 * 为什么用 MediaPlayer 而不是引入 ExoPlayer：
 * 只有一首内置的本地 mp3，没有播放列表、没有流媒体、没有后台播放需求。
 * MediaPlayer 是平台自带的，零依赖，够用。
 *
 * 生命周期要小心：MediaPlayer 不会自己释放，必须在 onDestroy 里 release，
 * 否则多次进出页面会泄漏多个实例（每个都占着解码器）。
 */
public class MusicPlayer {

    private MediaPlayer player;
    private boolean prepared = false;
    private String currentTitle = "";
    private OnStateListener listener;

    public interface OnStateListener {
        void onStateChanged(boolean playing, int positionMs, int durationMs);
    }

    public void setListener(OnStateListener l) {
        this.listener = l;
    }

    /** 播放指定的 raw 资源 */
    public void play(android.content.Context ctx, int rawResId, String title) {
        try {
            if (player == null) {
                player = MediaPlayer.create(ctx, rawResId);
                if (player == null) {
                    return;
                }
                prepared = true;
                currentTitle = title;
                player.setOnCompletionListener(mp -> {
                    // 放完回到开头，不自动重播
                    mp.seekTo(0);
                    notifyState();
                });
            }
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.start();
            }
            notifyState();
        } catch (Exception ignore) {
        }
    }

    public void toggle() {
        if (player == null || !prepared) {
            return;
        }
        try {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.start();
            }
            notifyState();
        } catch (Exception ignore) {
        }
    }

    public void seekTo(int ms) {
        if (player == null) {
            return;
        }
        try {
            player.seekTo(ms);
            notifyState();
        } catch (Exception ignore) {
        }
    }

    public boolean isPlaying() {
        try {
            return player != null && prepared && player.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public int position() {
        try {
            return player == null ? 0 : player.getCurrentPosition();
        } catch (Exception e) {
            return 0;
        }
    }

    public int duration() {
        try {
            return player == null ? 0 : player.getDuration();
        } catch (Exception e) {
            return 0;
        }
    }

    public String title() {
        return currentTitle;
    }

    private void notifyState() {
        if (listener != null) {
            listener.onStateChanged(isPlaying(), position(), duration());
        }
    }

    /** 必须在 Activity.onDestroy 调用，否则泄漏 */
    public void release() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignore) {
            }
            player.release();
            player = null;
            prepared = false;
        }
    }

    /** 内置曲目 */
    public static String builtinTitle() {
        return "雨夜 · 内置曲";
    }

    public static int builtinRes() {
        return R.raw.builtin_song;
    }
}
