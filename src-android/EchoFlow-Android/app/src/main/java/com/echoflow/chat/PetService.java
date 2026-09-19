package com.echoflow.chat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 桌宠：把角色的 **Q 版形象**悬浮在桌面上，会自己弹来弹去。
 *
 * 技术要点：
 *  1. **悬浮窗权限**（SYSTEM_ALERT_WINDOW）必须由用户手动授予，
 *     不能用运行时权限弹窗申请 —— 只能跳系统设置页。
 *  2. Android 8+ 必须用**前台服务**，否则系统会在几分钟后杀掉。
 *     通知设成最低优先级 + 静音，它只是「我还活着"的凭证。
 *  3. **物理**：重力 + 边界反弹 + 能量衰减，手写，不引引擎。
 *  4. **形变**：落地压扁、起跳拉长。没有这个，图片看起来就只是「在平移"。
 *  5. **接触阴影**：贴地时浓、飞起时淡，这是「她在桌面上"的关键。
 */
public class PetService extends Service {

    private static final String TAG = "PetService";
    public static final String EXTRA_CARD_ID = "card_id";

    private WindowManager wm;
    private ImageView petView;
    private WindowManager.LayoutParams params;

    private float x, y;
    private float vx, vy;
    private float size;
    private boolean running = true;
    private float squashY = 1f;
    /** 0 = 贴地，1 = 高飞（控制阴影浓度） */
    private float grounded = 0f;

    private final PetBall ball = new PetBall();
    private final PetAvatarRenderer renderer = new PetAvatarRenderer();
    private String cardId;
    private String cardName;

    /** 手指是否正按着球（拖拽时物理让位） */
    private boolean dragging = false;
    /** 进入静止状态的时间戳，0 = 没静止。用于静止后降帧省电 */
    private long idleSince = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastFrame;

    private static final float GRAVITY = 1400f;
    private static final float BOUNCE = 0.72f;
    private static final float FRICTION = 0.995f;
    private static final float MAX_SPEED = 2600f;

    private int hitCount = 0;

    private final Runnable step = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.currentTimeMillis();
            float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
            lastFrame = now;
            physics(dt);
            push();
            // 静止时降到 4fps：球不动就没必要每 16ms 重绘一次
            long delay = (idleSince != 0) ? 250 : 16;
            handler.postDelayed(this, delay);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.i(TAG, "onCreate");
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.echoflow.chat.STOP_PET".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            cardId = intent.getStringExtra(EXTRA_CARD_ID);
        }
        if (cardId == null) {
            cardId = PetStore.cardId(this);
        }
        if (cardId == null) {
            java.util.List<CharacterCard> cards = CardStore.listCards(this);
            if (!cards.isEmpty()) {
                cardId = cards.get(0).id;
            }
        }
        if (cardId == null) {
            Toast.makeText(this, "还没有角色，先创建一个再开桌宠", Toast.LENGTH_SHORT).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        PetStore.setCardId(this, cardId);
        PetStore.setRunning(this, true);
        CharacterCard c = CardStore.getCard(this, cardId);
        cardName = c == null ? "桌宠" : c.name;
        ball.baseColor = PetBall.colorFor(cardId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.startPetForeground(this, cardName);
        }

        if (petView == null) {
            showPet();
            startPhysics();
        }
        return START_STICKY;
    }

    // ==================================================================
    // 悬浮窗
    // ==================================================================

    private void showPet() {
        float d = getResources().getDisplayMetrics().density;
        size = 92 * d;   // 比原来的球大一些，Q 版图要看清脸

        petView = new ImageView(this);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                (int) size, (int) size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        x = dm.widthPixels * 0.35f;
        y = dm.heightPixels * 0.3f;
        vx = 620f;
        vy = 0f;
        params.x = (int) x;
        params.y = (int) y;

        attachAvatar();
        redraw(1f);

        petView.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY, startX, startY;
            float lastX, lastY;
            long lastT;
            float dragVx, dragVy;
            long downTime;
            // 注意：dragging 用的是外层字段，这里不再重复声明 ——
            // 之前那个局部 boolean dragging 把字段遮蔽了，
            // 导致物理线程读到的永远是 false，拖拽时物理还在跑。

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dragging = false;
                        idleSince = 0;   // 手指一按就恢复全帧率
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = params.x;
                        startY = params.y;
                        lastX = downX;
                        lastY = downY;
                        lastT = System.currentTimeMillis();
                        dragVx = dragVy = 0;
                        dragging = false;
                        downTime = lastT;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - downX;
                        float dy = e.getRawY() - downY;
                        if (!dragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
                            dragging = true;
                        }
                        if (dragging) {
                            params.x = (int) (startX + dx);
                            params.y = (int) (startY + dy);
                            x = params.x;
                            y = params.y;
                            long t = System.currentTimeMillis();
                            float dt2 = (t - lastT) / 1000f;
                            if (dt2 > 0.01f) {
                                dragVx = (e.getRawX() - lastX) / dt2;
                                dragVy = (e.getRawY() - lastY) / dt2;
                                lastX = e.getRawX();
                                lastY = e.getRawY();
                                lastT = t;
                            }
                            redraw(1.10f);   // 被拎起来时纵向拉长
                            try {
                                wm.updateViewLayout(petView, params);
                            } catch (Exception ignore) {
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 先解除拖拽状态，物理才能接管
                        boolean wasDragging = dragging;
                        dragging = false;
                        idleSince = 0;
                        if (wasDragging) {
                            vx = clamp(dragVx, -MAX_SPEED, MAX_SPEED);
                            vy = clamp(dragVy, -MAX_SPEED, MAX_SPEED);
                            lastFrame = 0;
                            redraw(1f);
                        } else if (System.currentTimeMillis() - downTime < 300) {
                            onTap();
                        }
                        return true;
                }
                return false;
            }
        });

        petView.setOnLongClickListener(v -> {
            showMenu();
            return true;
        });

        try {
            wm.addView(petView, params);
            android.util.Log.i(TAG, "pet view added, size=" + size);
        } catch (Exception e) {
            android.util.Log.e(TAG, "addView failed", e);
            Toast.makeText(this, "无法显示桌宠：" + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    /**
     * 挂上角色形象。
     *
     * 用**角色原图**，不再用 AI 生成的 Q 版 ——
     * 生成出来和角色本人不像，反而丢掉了"这是她"的感觉。
     * 原图虽然比例不是 Q 版，但至少一眼认得出是谁。
     *
     * 已有 Q 版图的用户不必手动清理：原图优先，旧文件留着也不影响。
     */
    private void attachAvatar() {
        Bitmap raw = CardStore.loadAvatar(this, cardId);
        if (raw != null) {
            renderer.setBitmap(raw);
            return;
        }
        // 连原图都没有（理论上不会），退回到 Q 版缓存
        Bitmap q = PetAvatar.load(this, cardId);
        if (q != null) {
            renderer.setBitmap(q);
        }
    }

    /** 重绘悬浮窗内容 */
    private void redraw(float squash) {
        if (petView == null) {
            return;
        }
        int w = (int) size;
        int h = (int) size;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        renderer.draw(c, squash, PetBall.colorFor(cardId), grounded);
        petView.setImageBitmap(bmp);
    }

    private void onTap() {
        // 点一下：往上一跳表示回应
        vy = -900f;
        lastFrame = 0;
        Intent i = new Intent(this, PhoneChatActivity.class);
        i.putExtra("card_id", cardId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void showMenu() {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert);
        b.setTitle(cardName + " · 桌宠");
        b.setItems(new String[]{"让她跳一下", "回到原位", "换一个角色", "关掉桌宠"},
                (d, which) -> {
                    switch (which) {
                        case 0:
                            vy = -1600f;
                            vx += (Math.random() - 0.5) * 400;
                            lastFrame = 0;
                            idleSince = 0;
                            break;
                        case 1:
                            // 拖到屏幕外或者卡住了，恢复到一个安全位置
                            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                            x = dm.widthPixels * 0.3f;
                            y = dm.heightPixels * 0.25f;
                            vx = 0f;
                            vy = 0f;
                            params.x = (int) x;
                            params.y = (int) y;
                            lastFrame = 0;
                            idleSince = 0;
                            try {
                                wm.updateViewLayout(petView, params);
                            } catch (Exception ignore) {
                            }
                            break;
                        case 2:
                            startActivity(new Intent(this, PetSettingsActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            break;
                        case 3:
                            stopSelf();
                            break;
                    }
                });
        b.show();
    }

    // ==================================================================
    // 物理
    // ==================================================================

    private void startPhysics() {
        lastFrame = 0;
        running = true;
        handler.post(step);
    }

    private void physics(float dt) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;

        // 用户正在拖拽时，物理完全让位，不要跟手指抢位置
        if (dragging) {
            return;
        }

        vy += GRAVITY * dt;
        vx *= FRICTION;
        vy = clamp(vy, -MAX_SPEED, MAX_SPEED);
        vx = clamp(vx, -MAX_SPEED, MAX_SPEED);

        // ---- 能量衰减：让它最终能真的停下来 ----
        // 原实现的问题：每次落地都检查 vx 是否低于 MIN_SPEED，
        // 低于就把它"补"回来，于是球永远停不下来，一直左右平移。
        // 正确做法是让水平速度自然衰减到 0，并且落地时也不再补速。
        boolean onGround = y + size >= screenH - 2f;
        if (onGround && Math.abs(vy) < 120f) {
            // 贴地且几乎没在弹跳 → 施加地面摩擦，把水平速度磨掉
            vx *= 0.92f;
            if (Math.abs(vx) < 12f) {
                vx = 0f;   // 足够小就直接归零，避免永远做无谓的亚像素移动
            }
        }

        // 水平墙：不损耗到 0，但要限制最大速度
        if (x < 0) {
            x = 0;
            vx = Math.abs(vx) * 0.75f;
        } else if (x + size > screenW) {
            x = screenW - size;
            vx = -Math.abs(vx) * 0.75f;
        }

        x += vx * dt;
        y += vy * dt;

        boolean bounced = false;

        if (y + size > screenH) {
            y = screenH - size;
            // 竖直方向的能量衰减：弹几次就基本不弹了
            vy = -Math.abs(vy) * BOUNCE;
            if (Math.abs(vy) < 90f) {
                vy = 0f;   // 不再弹
            }
            bounced = Math.abs(vy) > 0f;
        }
        if (y < 0) {
            y = 0;
            vy = Math.abs(vy) * BOUNCE;
        }

        // 完全静止时降低刷新率，省电
        boolean idle = vx == 0f && vy == 0f && Math.abs(y + size - screenH) < 2f;

        // 接触阴影：离地越近越浓
        float distToGround = screenH - (y + size);
        grounded = 1f - Math.min(1f, distToGround / (screenH * 0.5f));

        if (bounced) {
            squashY = 0.80f;
        } else {
            squashY += (1f - squashY) * Math.min(1f, dt * 14f);
        }

        params.x = (int) x;
        params.y = (int) y;
        redraw(squashY);

        // 静止后把帧率降下来（下面 step 里用 idleUntil 控制）
        idleSince = idle ? (idleSince == 0 ? System.currentTimeMillis() : idleSince) : 0;
    }

    private void kicked() {
        hitCount++;
        if (hitCount % 6 == 0) {
            vx += (Math.random() - 0.5) * 700f;
        }
    }

    private void push() {
        try {
            if (petView != null && petView.isAttachedToWindow()) {
                wm.updateViewLayout(petView, params);
            }
        } catch (Exception ignore) {
        }
    }

    // ==================================================================

    private float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, PetService.class));
    }

    public static boolean canDraw(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(ctx);
        }
        return true;
    }

    public static void requestPermission(android.app.Activity act, int reqCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + act.getPackageName()));
            act.startActivityForResult(i, reqCode);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (petView != null && wm != null) {
            try {
                wm.removeView(petView);
            } catch (Exception ignore) {
            }
            petView = null;
        }
        NotificationHelper.stopPetForeground(this);
        PetStore.setRunning(this, false);
    }
}