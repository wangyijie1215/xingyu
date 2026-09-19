package com.echoflow.chat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 每个角色用哪个音色。
 *
 * 单独存一层，而不是塞进 CharacterCard —— 理由是：
 *   · 音色是**本机设备**上的偏好，不该跟着角色卡导出/导入走
 *   · 换手机时音色名可能不存在（不同 TTS 服务音色表不同），
 *     跟着卡走会带来一堆无效值
 */
public class TtsStore {

    private static final String PREFS = "echoflow_tts_voices";

    public static String voiceFor(Context ctx, String cardId) {
        if (cardId == null) {
            return null;
        }
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("v_" + cardId, null);
    }

    public static void setVoice(Context ctx, String cardId, String voice) {
        if (cardId == null) {
            return;
        }
        SharedPreferences.Editor e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (voice == null || voice.isEmpty()) {
            e.remove("v_" + cardId);
        } else {
            e.putString("v_" + cardId, voice);
        }
        e.apply();
    }
}
