package com.echoflow.chat;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 模拟手机的「系统层"。
 *
 * 让手机模式像真手机的关键不是画个手机壳，而是：
 *  · 状态栏显示**真实时间**（跟着系统走，不是写死的）
 *  · 电量读**真实电量**（BatteryManager）
 *  · 信号强度用随机但稳定的值（模拟，不读真实信号——没意义且需要权限）
 *  · 所有页面共用同一条状态栏，切页面不闪
 */
public class PhoneSystem {

    public static String clock() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    /** 真实电量百分比 */
    public static int battery(Context ctx) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent status = ctx.registerReceiver(null, ifilter);
            if (status == null) {
                return 100;
            }
            int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) {
                return 100;
            }
            return Math.round(level * 100f / scale);
        } catch (Exception e) {
            return 100;
        }
    }

    public static boolean charging(Context ctx) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent status = ctx.registerReceiver(null, ifilter);
            if (status == null) {
                return false;
            }
            int s = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return s == BatteryManager.BATTERY_STATUS_CHARGING
                    || s == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 信号格数 0~4。
     * 用分钟数做种子——同一分钟内稳定，跨分钟缓慢变化，看起来像真的在动。
     */
    public static int signalBars() {
        long seed = System.currentTimeMillis() / 60000L;
        int v = (int) (seed % 5);
        // 大部分时候是 3~4 格，偶尔掉到 2
        return v == 0 ? 2 : (v == 1 ? 3 : 4);
    }

    /** 网络制式标签 */
    public static String carrier() {
        long seed = System.currentTimeMillis() / 600000L;
        String[] kinds = {"5G", "5G", "4G", "5G"};
        return kinds[(int) (seed % kinds.length)];
    }

    /** 电量图标：根据电量和充电状态返回 unicode */
    public static String batteryGlyph(Context ctx) {
        if (charging(ctx)) {
            return "⚡";
        }
        int b = battery(ctx);
        if (b >= 90) return "▮";
        if (b >= 60) return "▯";
        if (b >= 30) return "▭";
        return "▬";
    }

    /** 状态栏完整文本，例如 "17:21" */
    public static String statusTime() {
        return clock();
    }
}