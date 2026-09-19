package com.echoflow.chat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 手机模式的时间格式：模仿微信的习惯用法。
 */
public class PhoneFormat {

    /** 会话列表的时间：今天显示 HH:mm，昨天显示「昨天"，本周显示星期，更早显示日期 */
    public static String sessionTime(long ts) {
        if (ts <= 0) {
            return "";
        }
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ts);

        if (isSameDay(now, then)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(now, then)) {
            return "昨天";
        }
        now.add(Calendar.DAY_OF_YEAR, 6);
        if (then.after(now)) {
            String[] week = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return week[then.get(Calendar.DAY_OF_WEEK)];
        }
        return new SimpleDateFormat("yyyy/M/d", Locale.getDefault()).format(new Date(ts));
    }

    /** 聊天页顶部的时间 */
    public static String chatTime(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ts);
        String hm = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
        if (isSameDay(now, then)) {
            return hm;
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(now, then)) {
            return "昨天 " + hm;
        }
        return new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(new Date(ts));
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}