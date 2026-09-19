package com.echoflow.chat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * 通知辅助：桌宠前台服务需要一个常驻通知，否则系统会杀掉它。
 *
 * 关键点：**通知渠道的优先级必须低**（IMPORTANCE_LOW），否则会有声音和横幅，
 * 用户会以为收到了消息 —— 桌宠的通知只是个「我还活着"的凭证。
 */
public class NotificationHelper {

    private static final String CH_PET = "pet_service";
    private static final int ID_PET = 9001;

    public static void startPetForeground(Service svc, String cardName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;   // 低版本不需要前台服务也基本能活
        }
        NotificationManager nm = (NotificationManager)
                svc.getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm.getNotificationChannel(CH_PET) == null) {
            NotificationChannel ch = new NotificationChannel(
                    CH_PET, "桌宠运行中", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("保持桌宠在桌面上活动");
            ch.setShowBadge(false);
            ch.enableVibration(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(svc, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(svc, 0, open, flags);

        // 点通知里的「关掉"直接停服务
        Intent stop = new Intent(svc, PetService.class);
        stop.setAction("com.echoflow.chat.STOP_PET");
        PendingIntent stopPi = PendingIntent.getService(svc, 1, stop, flags);

        Notification n = new NotificationCompat.Builder(svc, CH_PET)
                .setContentTitle("桌宠在活动")
                .setContentText(cardName + " 正在桌面上弹来弹去")
                .setSmallIcon(R.drawable.ic_stat_pet)
                .setContentIntent(pi)
                .addAction(0, "关掉", stopPi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();

        svc.startForeground(ID_PET, n);
    }

    public static void stopPetForeground(Service svc) {
        try {
            svc.stopForeground(true);
        } catch (Exception ignore) {
        }
    }
}