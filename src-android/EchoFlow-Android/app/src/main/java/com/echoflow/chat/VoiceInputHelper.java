package com.echoflow.chat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 把「按住说话」接进各个聊天页的公共逻辑。
 *
 * 三个聊天页（手机微信、角色扮演、YIJIE 助手）的麦克风按钮行为应该一致，
 * 所以抽到这里，而不是各写一遍。
 *
 * 交互设计：
 *   · 点一下麦克风 → 弹一个「正在听…」的浮层，显示音量波动
 *   · 说完自动结束，文字填进输入框（**不自动发送** —— 给用户改的机会，
 *     语音识别难免错字，自动发送会让人措手不及）
 *   · 浮层上有个「取消」可中断
 */
public class VoiceInputHelper {

    private static final int REQ_AUDIO = 9001;

    /** 待处理的权限回调 */
    private static Runnable pendingAfterGrant;

    /**
     * 给一个 EditText 挂上语音输入。
     *
     * @param micBtn 麦克风按钮（null 则只做权限与识别，不管按钮）
     */
    public static void attach(final Activity act, final EditText target, TextView micBtn,
                              final String hint) {
        if (micBtn != null) {
            micBtn.setOnClickListener(v -> start(act, target, hint));
        }
    }

    public static void start(final Activity act, final EditText target, final String hint) {
        SttProvider.Config cfg = SttProvider.load(act);
        if (!cfg.enabled) {
            toast(act, "语音输入已关闭（去「语音」设置里打开）");
            return;
        }

        // 手机自带走系统识别（不需要额外权限以外的准备）；
        // 其他渠道也是先录音，但这里统一用系统识别 —— 因为它在设备上零延迟。
        // 如果要强制用远端渠道，在设置里把 kind 改掉，会走 VoiceInput 之外的路径
        // （当前版本：手机自带直接可用，远端渠道留作后续）
        if (!VoiceInput.hasPermission(act)) {
            pendingAfterGrant = () -> start(act, target, hint);
            ActivityCompat.requestPermissions(act,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (!VoiceInput.isAvailable(act)) {
            toast(act, "这台设备没有可用的语音识别服务");
            return;
        }
        listenNow(act, target, hint);
    }

    /** 权限回调要由 Activity 转进来 */
    public static void onPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) {
        if (requestCode != REQ_AUDIO) {
            return;
        }
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        Runnable r = pendingAfterGrant;
        pendingAfterGrant = null;
        if (granted && r != null) {
            r.run();
        }
    }

    // ==================================================================

    private static void listenNow(final Activity act, final EditText target, String hint) {
        // 浮层：显示音量
        final TextView status = new TextView(act);
        status.setText("正在听…");
        status.setTextSize(16);
        status.setTextColor(0xFFF4F2FF);
        status.setPadding(dp(act, 28), dp(act, 24), dp(act, 28), dp(act, 24));

        final AlertDialog dlg = new AlertDialog.Builder(act)
                .setTitle(hint == null ? "说点什么" : hint)
                .setView(status)
                .setNegativeButton("取消", (d, w) -> {
                    // 取消要真的停掉识别
                    VoiceInputHolder.cancel();
                })
                .setCancelable(true)
                .create();
        dlg.setOnDismissListener(d -> VoiceInputHolder.cancel());
        dlg.show();

        final VoiceInput vi = VoiceInputHolder.get(act);
        vi.start(new VoiceInput.Callback() {
            @Override
            public void onReady() {
                status.setText("正在听…（说完会自动结束）");
            }

            @Override
            public void onLevel(float level) {
                int bars = (int) (level * 12);
                StringBuilder sb = new StringBuilder("正在听…\n");
                for (int i = 0; i < 12; i++) {
                    sb.append(i < bars ? "█" : "·");
                }
                status.setText(sb.toString());
            }

            @Override
            public void onResult(String text) {
                if (dlg.isShowing()) {
                    dlg.dismiss();
                }
                // 填进输入框但**不自动发送** —— 识别难免有错字，给用户改的机会
                if (target != null) {
                    String cur = target.getText().toString();
                    target.setText(cur.isEmpty() ? text : cur + text);
                    target.setSelection(target.getText().length());
                }
            }

            @Override
            public void onError(String msg) {
                if (dlg.isShowing()) {
                    dlg.dismiss();
                }
                // 「没听清」这类不算错误，不用弹窗打扰
                if (msg != null && (msg.contains("没听清") || msg.contains("没听到")
                        || msg.contains("识别被中断"))) {
                    return;
                }
                toast(act, msg);
            }
        });
    }

    // ==================================================================

    /**
     * 每个 Activity 复用一个 VoiceInput 实例。
     *
     * 为什么不每次 new：SpeechRecognizer 的创建/销毁有开销，
     * 而且频繁重建偶尔会撞上 BUSY。复用一个、每次 start 前 cancel 更稳。
     */
    private static final class VoiceInputHolder {
        private static VoiceInput instance;
        private static Activity owner;

        static VoiceInput get(Activity act) {
            if (instance == null || owner != act) {
                if (instance != null) {
                    instance.destroy();
                }
                instance = new VoiceInput(act);
                owner = act;
            }
            return instance;
        }

        static void cancel() {
            if (instance != null) {
                instance.cancel();
            }
        }
    }

    /** 页面销毁时释放；否则下次进来会 BUSY */
    public static void release() {
        VoiceInputHolder.cancel();
    }

    private static void toast(Activity act, String msg) {
        if (msg == null) {
            return;
        }
        android.widget.Toast.makeText(act, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private static int dp(Activity act, int v) {
        return Math.round(v * act.getResources().getDisplayMetrics().density);
    }
}
