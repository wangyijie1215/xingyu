package com.echoflow.chat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 状态栏控制器：把真实时间/电量灌进 view_phone_statusbar，
 * 并起一个每分钟刷新的定时器（时钟变化 + 电量变化）。
 *
 * 为什么用 Timer 而不是 BroadcastReceiver：手机模式是短时界面，
 * 每分钟一次的系统时钟刷新用定时器更简单、生命周期更好控制。
 */
public class PhoneStatusBar {

    private final Context ctx;
    private final View root;
    private Timer timer;

    public PhoneStatusBar(Context ctx, View root) {
        this.ctx = ctx;
        this.root = root;
    }

    public void bind() {
        refresh();
        startTicking();
    }

    public void refresh() {
        if (root == null) {
            return;
        }
        TextView time = root.findViewById(R.id.sb_time);
        TextView carrier = root.findViewById(R.id.sb_carrier);
        TextView signal = root.findViewById(R.id.sb_signal);
        TextView battery = root.findViewById(R.id.sb_battery);
        TextView battIcon = root.findViewById(R.id.sb_battery_icon);

        if (time != null) {
            time.setText(PhoneSystem.statusTime());
        }
        if (carrier != null) {
            carrier.setText(PhoneSystem.carrier());
        }
        if (signal != null) {
            int bars = PhoneSystem.signalBars();
            StringBuilder sb = new StringBuilder();
            String[] glyphs = {"▁", "▃", "▅", "▇"};
            for (int i = 0; i < 4; i++) {
                sb.append(i < bars ? glyphs[i] : "▁");
            }
            signal.setText(sb.toString());
            signal.setAlpha(bars >= 3 ? 1f : 0.75f);
        }
        if (battery != null) {
            battery.setText(String.valueOf(PhoneSystem.battery(ctx)));
        }
        if (battIcon != null) {
            battIcon.setText(PhoneSystem.batteryGlyph(ctx));
        }
    }

    private void startTicking() {
        stopTicking();
        timer = new Timer("phone-statusbar", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (root == null) {
                    return;
                }
                root.post(PhoneStatusBar.this::refresh);
            }
        }, 10000, 10000);   // 每 10 秒刷一次，分钟跳变最迟 10 秒内反映
    }

    public void stopTicking() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
