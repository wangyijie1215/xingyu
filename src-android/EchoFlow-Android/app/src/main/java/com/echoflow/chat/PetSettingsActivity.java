package com.echoflow.chat;

/* loaded from: classes.dex */
public class PetSettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private static final int REQ_OVERLAY = 1001;
    private android.widget.LinearLayout cardRow;
    private final java.util.List<com.echoflow.chat.CharacterCard> cards = new java.util.ArrayList();
    private android.widget.TextView previewBall;
    private java.lang.String selectedId;
    private android.widget.TextView statusText;
    private android.widget.TextView toggleBtn;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-15857128);
        linearLayout.setPadding(dp(18), dp(24), dp(18), dp(24));
        android.view.View inflate = getLayoutInflater().inflate(com.echoflow.chat.R.layout.view_phone_statusbar, (android.view.ViewGroup) linearLayout, false);
        linearLayout.addView(inflate);
        new com.echoflow.chat.PhoneStatusBar(this, inflate).bind();
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText("桌宠");
        textView.setTextSize(22.0f);
        textView.setTextColor(-724225);
        textView.setTypeface(null, 1);
        textView.setPadding(0, dp(16), 0, dp(6));
        linearLayout.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(this);
        textView2.setText("把角色变成一颗 Q 版小球，悬浮在手机桌面上弹来弹去。\n点小球进聊天，长按弹菜单，拖它能甩出去。");
        textView2.setTextSize(13.0f);
        textView2.setTextColor(-7633752);
        textView2.setLineSpacing(dp(4), 1.0f);
        textView2.setPadding(0, 0, 0, dp(18));
        linearLayout.addView(textView2);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setGravity(17);
        linearLayout2.setPadding(0, dp(10), 0, dp(10));
        android.widget.TextView textView3 = new android.widget.TextView(this);
        this.previewBall = textView3;
        textView3.setGravity(17);
        this.previewBall.setTextSize(40.0f);
        this.previewBall.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(96), dp(96)));
        linearLayout2.addView(this.previewBall);
        linearLayout.addView(linearLayout2);
        android.widget.TextView textView4 = new android.widget.TextView(this);
        this.statusText = textView4;
        textView4.setTextSize(13.0f);
        this.statusText.setLineSpacing(dp(4), 1.0f);
        this.statusText.setPadding(dp(14), dp(12), dp(14), dp(12));
        this.statusText.setBackground(com.echoflow.chat.EfUi.roundRectPx(452984831, 587202559, dp(12)));
        linearLayout.addView(this.statusText);
        android.widget.TextView textView5 = new android.widget.TextView(this);
        textView5.setText("选择哪个角色当桌宠");
        textView5.setTextSize(13.0f);
        textView5.setTextColor(-7633752);
        textView5.setPadding(0, dp(20), 0, dp(8));
        linearLayout.addView(textView5);
        android.widget.HorizontalScrollView horizontalScrollView = new android.widget.HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        this.cardRow = linearLayout3;
        linearLayout3.setOrientation(0);
        horizontalScrollView.addView(this.cardRow);
        linearLayout.addView(horizontalScrollView);
        android.widget.TextView textView6 = new android.widget.TextView(this);
        this.toggleBtn = textView6;
        textView6.setTextSize(16.0f);
        this.toggleBtn.setGravity(17);
        this.toggleBtn.setTextColor(-1);
        this.toggleBtn.setTypeface(null, 1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, dp(52));
        layoutParams.topMargin = dp(24);
        this.toggleBtn.setLayoutParams(layoutParams);
        this.toggleBtn.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PetSettingsActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.PetSettingsActivity.this.lambda$onCreate$0(view);
            }
        });
        linearLayout.addView(this.toggleBtn);
        android.widget.TextView textView7 = new android.widget.TextView(this);
        textView7.setText("提示：桌宠需要一个常驻通知才不会系统被清掉，那个通知是静音的，不会打扰你。");
        textView7.setTextSize(11.0f);
        textView7.setTextColor(-10857352);
        textView7.setLineSpacing(dp(3), 1.0f);
        textView7.setPadding(0, dp(14), 0, 0);
        linearLayout.addView(textView7);
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        // 关键：ScrollView 自己也要有背景色。
        // 只给里面的 LinearLayout 设底色是不够的 —— 内容不满一屏时，
        // 下方露出的部分是 ScrollView 自己的背景（主题默认白）。
        scrollView.setBackgroundColor(-15857128);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
        this.cards.addAll(com.echoflow.chat.PhoneStore.recentCards(this));
        java.lang.String cardId = com.echoflow.chat.PetStore.cardId(this);
        this.selectedId = cardId;
        if (cardId == null && !this.cards.isEmpty()) {
            this.selectedId = this.cards.get(0).id;
        }
        buildCardRow();
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        onToggle();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refresh() {
        boolean canDraw = com.echoflow.chat.PetService.canDraw(this);
        boolean isRunning = com.echoflow.chat.PetStore.isRunning(this);
        if (!canDraw) {
            this.statusText.setText("⚠️ 还没有悬浮窗权限\n点下面的按钮会跳到系统设置，找到「星语」打开「显示在其他应用上层」。");
            this.statusText.setTextColor(-18325);
            this.toggleBtn.setText("去开启悬浮窗权限");
        } else if (isRunning) {
            this.statusText.setText("✅ 桌宠正在运行\n它现在应该在你桌面上弹。想停就在下面关掉。");
            this.statusText.setTextColor(-8396604);
            this.toggleBtn.setText("关掉桌宠");
        } else {
            this.statusText.setText("✅ 权限已就绪\n点下面的按钮让她上桌面。");
            this.statusText.setTextColor(-8396604);
            this.toggleBtn.setText("放她到桌面");
        }
        this.previewBall.setBackground(com.echoflow.chat.EfUi.roundRectPx(com.echoflow.chat.PetBall.colorFor(this.selectedId), 0, dp(48)));
        this.previewBall.setText("● ●\n\u3000⌣");
        this.previewBall.setTextColor(-14017974);
    }

    private void onToggle() {
        boolean canDraw = com.echoflow.chat.PetService.canDraw(this);
        boolean isRunning = com.echoflow.chat.PetStore.isRunning(this);
        if (!canDraw) {
            android.widget.Toast.makeText(this, "请在接下来的页面里打开「显示在其他应用上层」", 1).show();
            com.echoflow.chat.PetService.requestPermission(this, 1001);
            return;
        }
        if (isRunning) {
            com.echoflow.chat.PetService.stop(this);
            com.echoflow.chat.PetStore.setRunning(this, false);
            android.widget.Toast.makeText(this, "桌宠已关掉", 0).show();
            refresh();
            return;
        }
        java.lang.String str = this.selectedId;
        if (str == null) {
            android.widget.Toast.makeText(this, "先创建一个角色", 0).show();
            return;
        }
        com.echoflow.chat.PetStore.setCardId(this, str);
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.PetService.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.selectedId);
        startForegroundService(intent);
        android.widget.Toast.makeText(this, "桌宠已上场", 0).show();
        this.toggleBtn.postDelayed(new java.lang.Runnable() { // from class: com.echoflow.chat.PetSettingsActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.PetSettingsActivity.this.refresh();
            }
        }, 600L);
    }

    private void buildCardRow() {
        this.cardRow.removeAllViews();
        for (final com.echoflow.chat.CharacterCard characterCard : this.cards) {
            android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
            linearLayout.setOrientation(1);
            linearLayout.setGravity(1);
            linearLayout.setPadding(dp(6), dp(4), dp(6), dp(4));
            linearLayout.setClickable(true);
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            int dp = dp(54);
            imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, characterCard.id);
            if (loadAvatar != null) {
                imageView.setImageBitmap(loadAvatar);
            } else {
                imageView.setImageResource(com.echoflow.chat.R.drawable.app_icon);
            }
            boolean equals = characterCard.id.equals(this.selectedId);
            int i = -6587393;
            imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(0, equals ? -6587393 : 872415231, dp(27)));
            imageView.setClipToOutline(true);
            linearLayout.addView(imageView);
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText(characterCard.name);
            textView.setTextSize(11.0f);
            if (!equals) {
                i = -7633752;
            }
            textView.setTextColor(i);
            textView.setPadding(0, dp(4), 0, 0);
            linearLayout.addView(textView);
            linearLayout.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PetSettingsActivity$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.PetSettingsActivity.this.lambda$buildCardRow$1(characterCard, view);
                }
            });
            this.cardRow.addView(linearLayout);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildCardRow$1(com.echoflow.chat.CharacterCard characterCard, android.view.View view) {
        this.selectedId = characterCard.id;
        com.echoflow.chat.PetStore.setCardId(this, characterCard.id);
        buildCardRow();
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onActivityResult(int i, int i2, android.content.Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1001) {
            refresh();
        }
    }

    private int dp(int i) {
        return java.lang.Math.round(i * getResources().getDisplayMetrics().density);
    }
}
