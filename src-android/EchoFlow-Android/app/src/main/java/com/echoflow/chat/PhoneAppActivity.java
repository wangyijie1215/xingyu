package com.echoflow.chat;

/* loaded from: classes.dex */
public class PhoneAppActivity extends androidx.appcompat.app.AppCompatActivity {
    public static final java.lang.String EXTRA_APP = "app";
    private android.widget.TextView calcDisplay;
    private android.widget.TextView durText;
    private com.echoflow.chat.MusicPlayer musicPlayer;
    private android.widget.TextView playBtn;
    private android.widget.TextView posText;
    private android.os.Handler progressHandler;
    private android.widget.SeekBar seekBar;
    private boolean dragging = false;
    private final java.lang.StringBuilder calcExpr = new java.lang.StringBuilder();

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        char c;
        android.view.View musicBody;
        super.onCreate(bundle);
        java.lang.String stringExtra = getIntent().getStringExtra(EXTRA_APP);
        if (stringExtra == null) {
            stringExtra = "gallery";
        }
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-1184275);
        android.view.View inflate = getLayoutInflater().inflate(com.echoflow.chat.R.layout.view_phone_statusbar, (android.view.ViewGroup) linearLayout, false);
        linearLayout.addView(inflate);
        new com.echoflow.chat.PhoneStatusBar(this, inflate).bind();
        linearLayout.addView(topBar(titleOf(stringExtra)));
        switch (stringExtra.hashCode()) {
            case -196315310:
                if (stringExtra.equals("gallery")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 3045973:
                if (stringExtra.equals("calc")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 104263205:
                if (stringExtra.equals("music")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 105008833:
                if (stringExtra.equals("notes")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1223440372:
                if (stringExtra.equals("weather")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        if (c == 0) {
            musicBody = musicBody();
        } else if (c == 1) {
            musicBody = weatherBody();
        } else if (c == 2) {
            musicBody = notesBody();
        } else if (c == 3) {
            musicBody = calcBody();
        } else {
            musicBody = galleryBody();
        }
        if (!"calc".equals(stringExtra)) {
            musicBody.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
        }
        linearLayout.addView(musicBody);
        setContentView(linearLayout);
    }

    private java.lang.String titleOf(java.lang.String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case 3045973:
                if (str.equals("calc")) {
                    c = 0;
                    break;
                }
                break;
            case 104263205:
                if (str.equals("music")) {
                    c = 1;
                    break;
                }
                break;
            case 105008833:
                if (str.equals("notes")) {
                    c = 2;
                    break;
                }
                break;
            case 1223440372:
                if (str.equals("weather")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return "计算器";
            case 1:
                return "音乐";
            case 2:
                return "备忘录";
            case 3:
                return "天气";
            default:
                return "相册";
        }
    }

    private android.view.View topBar(java.lang.String str) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.wx_topbar);
        linearLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, dp(52)));
        linearLayout.setPadding(dp(12), 0, dp(12), 0);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText("‹");
        textView.setTextSize(26.0f);
        textView.setTextColor(-15198184);
        textView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(40), -1));
        textView.setGravity(16);
        textView.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.PhoneAppActivity.this.lambda$topBar$0(view);
            }
        });
        linearLayout.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(this);
        textView2.setText(str);
        textView2.setTextSize(17.0f);
        textView2.setTextColor(-15198184);
        textView2.setTypeface(null, 1);
        textView2.setGravity(17);
        textView2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout.addView(textView2);
        android.view.View view = new android.view.View(this);
        view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(40), 1));
        linearLayout.addView(view);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$0(android.view.View view) {
        finish();
    }

    private android.view.View galleryBody() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        int i = 10;
        linearLayout.setPadding(dp(10), dp(10), dp(10), dp(20));
        scrollView.addView(linearLayout);
        java.util.List<com.echoflow.chat.CharacterCard> recentCards = com.echoflow.chat.PhoneStore.recentCards(this);
        if (recentCards.isEmpty()) {
            linearLayout.addView(hint("还没有照片。先去角色库创建角色。"));
            return scrollView;
        }
        for (com.echoflow.chat.CharacterCard characterCard : recentCards) {
            android.widget.TextView textView = new android.widget.TextView(this);
            textView.setText(characterCard.name + " 的照片");
            textView.setTextSize(13.0f);
            textView.setTextColor(-10066330);
            textView.setPadding(dp(6), dp(i), 0, dp(6));
            linearLayout.addView(textView);
            android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
            linearLayout2.setOrientation(0);
            int dp = dp(androidx.core.location.LocationRequestCompat.QUALITY_LOW_POWER);
            int[] iArr = {0, sceneFor(characterCard.id), com.echoflow.chat.R.drawable.ef_shrine_hero};
            int i2 = 0;
            while (i2 < 3) {
                android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
                android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(dp, dp);
                layoutParams.setMargins(0, 0, dp(6), 0);
                frameLayout.setLayoutParams(layoutParams);
                android.widget.ImageView imageView = new android.widget.ImageView(this);
                imageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(dp, dp));
                imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                if (i2 == 0) {
                    android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, characterCard.id);
                    if (loadAvatar != null) {
                        imageView.setImageBitmap(loadAvatar);
                    } else {
                        imageView.setImageResource(com.echoflow.chat.R.drawable.app_icon);
                    }
                } else {
                    int i3 = iArr[i2];
                    if (i3 == 0) {
                        i3 = com.echoflow.chat.R.drawable.ef_rainy_night;
                    }
                    imageView.setImageResource(i3);
                }
                frameLayout.addView(imageView);
                final int i4 = i2 == 0 ? 0 : iArr[i2];
                final java.lang.String str = characterCard.id;
                final java.lang.String str2 = characterCard.name;
                frameLayout.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(android.view.View view) {
                        com.echoflow.chat.PhoneAppActivity.this.lambda$galleryBody$1(str2, str, i4, view);
                    }
                });
                linearLayout2.addView(frameLayout);
                i2++;
            }
            linearLayout.addView(linearLayout2);
            i = 10;
        }
        return scrollView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$galleryBody$1(java.lang.String str, java.lang.String str2, int i, android.view.View view) {
        showPhoto(str, str2, i);
    }

    private int sceneFor(java.lang.String str) {
        if (str == null) {
            return com.echoflow.chat.R.drawable.ef_rainy_night;
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -307444203:
                if (str.equals("builtin_hakuyo")) {
                    c = 0;
                    break;
                }
                break;
            case -102216965:
                if (str.equals("builtin_rin")) {
                    c = 1;
                    break;
                }
                break;
            case 538132324:
                if (str.equals("builtin_alice")) {
                    c = 2;
                    break;
                }
                break;
            case 541826355:
                if (str.equals("builtin_elian")) {
                    c = 3;
                    break;
                }
                break;
            case 553915798:
                if (str.equals("builtin_rocco")) {
                    c = 4;
                    break;
                }
                break;
            case 1828583459:
                if (str.equals("builtin_yueling")) {
                    c = 5;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return com.echoflow.chat.R.drawable.ef_shrine_water;
            case 1:
                return com.echoflow.chat.R.drawable.ef_campus;
            case 2:
                return com.echoflow.chat.R.drawable.ef_rainy_night;
            case 3:
                return com.echoflow.chat.R.drawable.ef_chat_bg;
            case 4:
                return com.echoflow.chat.R.drawable.ef_ema_rack;
            case 5:
                return com.echoflow.chat.R.drawable.ef_shrine_hero;
            default:
                return com.echoflow.chat.R.drawable.ef_campus;
        }
    }

    private android.view.View musicBody() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-15462369);
        linearLayout.setPadding(dp(20), dp(20), dp(20), dp(24));
        scrollView.addView(linearLayout);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(1);
        linearLayout2.setPadding(dp(18), dp(22), dp(18), dp(20));
        linearLayout2.setBackground(com.echoflow.chat.EfUi.roundRectPx(654311423, 536870911, dp(18)));
        android.widget.ImageView imageView = new android.widget.ImageView(this);
        int dp = dp(150);
        imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(570425344, 587202559, dp(16)));
        imageView.setClipToOutline(true);
        java.util.List<com.echoflow.chat.CharacterCard> recentCards = com.echoflow.chat.PhoneStore.recentCards(this);
        if (!recentCards.isEmpty()) {
            android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, recentCards.get(0).id);
            if (loadAvatar != null) {
                imageView.setImageBitmap(loadAvatar);
            } else {
                imageView.setImageResource(com.echoflow.chat.R.drawable.app_icon);
            }
        } else {
            imageView.setImageResource(com.echoflow.chat.R.drawable.app_icon);
        }
        linearLayout2.addView(imageView);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(com.echoflow.chat.MusicPlayer.builtinTitle());
        textView.setTextSize(17.0f);
        textView.setTextColor(-724225);
        textView.setTypeface(null, 1);
        textView.setPadding(0, dp(16), 0, 0);
        linearLayout2.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(this);
        textView2.setText("内置曲 · 182 秒 · 96kbps");
        textView2.setTextSize(11.0f);
        textView2.setTextColor(-7633752);
        textView2.setPadding(0, dp(4), 0, 0);
        linearLayout2.addView(textView2);
        this.seekBar = new android.widget.SeekBar(this);
        int i = -1;
        int i2 = -2;
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(16);
        this.seekBar.setLayoutParams(layoutParams);
        this.seekBar.setMax(1000);
        this.seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() { // from class: com.echoflow.chat.PhoneAppActivity.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(android.widget.SeekBar seekBar, int i3, boolean z) {
                if (!z || com.echoflow.chat.PhoneAppActivity.this.musicPlayer == null || com.echoflow.chat.PhoneAppActivity.this.musicPlayer.duration() <= 0) {
                    return;
                }
                com.echoflow.chat.PhoneAppActivity.this.musicPlayer.seekTo((i3 * com.echoflow.chat.PhoneAppActivity.this.musicPlayer.duration()) / 1000);
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                com.echoflow.chat.PhoneAppActivity.this.dragging = true;
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                com.echoflow.chat.PhoneAppActivity.this.dragging = false;
            }
        });
        linearLayout2.addView(this.seekBar);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        android.widget.TextView textView3 = new android.widget.TextView(this);
        this.posText = textView3;
        textView3.setText("0:00");
        this.posText.setTextSize(11.0f);
        this.posText.setTextColor(-7633752);
        this.posText.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout3.addView(this.posText);
        android.widget.TextView textView4 = new android.widget.TextView(this);
        this.durText = textView4;
        textView4.setText("3:02");
        this.durText.setTextSize(11.0f);
        this.durText.setTextColor(-7633752);
        linearLayout3.addView(this.durText);
        linearLayout2.addView(linearLayout3);
        android.widget.TextView textView5 = new android.widget.TextView(this);
        this.playBtn = textView5;
        textView5.setText("▶");
        this.playBtn.setTextSize(24.0f);
        this.playBtn.setGravity(17);
        this.playBtn.setTextColor(-1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(dp(64), dp(64));
        layoutParams2.topMargin = dp(14);
        this.playBtn.setLayoutParams(layoutParams2);
        this.playBtn.setBackground(com.echoflow.chat.EfUi.roundRectPx(-6587393, 0, dp(32)));
        this.playBtn.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.PhoneAppActivity.this.lambda$musicBody$4(view);
            }
        });
        linearLayout2.addView(this.playBtn);
        linearLayout.addView(linearLayout2);
        android.widget.TextView textView6 = new android.widget.TextView(this);
        textView6.setText("这是应用内置的曲子，不需要联网，也不消耗任何额度。\n下面是她会在听这首歌时说的一句话。");
        textView6.setTextSize(12.0f);
        textView6.setTextColor(-7633752);
        textView6.setLineSpacing(dp(4), 1.0f);
        textView6.setPadding(dp(4), dp(18), dp(4), dp(10));
        linearLayout.addView(textView6);
        int i3 = 0;
        while (i3 < recentCards.size()) {
            com.echoflow.chat.CharacterCard characterCard = recentCards.get(i3);
            com.echoflow.chat.Schedule.Now now = com.echoflow.chat.Schedule.now(com.echoflow.chat.Schedule.load(this, characterCard.id));
            com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, characterCard.id);
            android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
            linearLayout4.setOrientation(0);
            linearLayout4.setGravity(16);
            android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(i, i2);
            layoutParams3.bottomMargin = dp(8);
            linearLayout4.setLayoutParams(layoutParams3);
            linearLayout4.setBackground(com.echoflow.chat.EfUi.roundRectPx(352321535, 0, dp(12)));
            linearLayout4.setPadding(dp(12), dp(10), dp(12), dp(10));
            android.widget.ImageView imageView2 = new android.widget.ImageView(this);
            imageView2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(34), dp(34)));
            imageView2.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            android.graphics.Bitmap loadAvatar2 = com.echoflow.chat.CardStore.loadAvatar(this, characterCard.id);
            if (loadAvatar2 != null) {
                imageView2.setImageBitmap(loadAvatar2);
            } else {
                imageView2.setImageResource(com.echoflow.chat.R.drawable.app_icon);
            }
            linearLayout4.addView(imageView2);
            android.widget.TextView textView7 = new android.widget.TextView(this);
            textView7.setText(characterCard.name + "：" + songLine(characterCard.id, now, loadEmotion));
            textView7.setTextSize(12.0f);
            textView7.setTextColor(-2238224);
            textView7.setPadding(dp(10), 0, 0, 0);
            textView7.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            linearLayout4.addView(textView7);
            final java.lang.String str = characterCard.id;
            linearLayout4.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.PhoneAppActivity.this.lambda$musicBody$5(str, view);
                }
            });
            linearLayout.addView(linearLayout4);
            i3++;
            i2 = -2;
            i = -1;
        }
        return scrollView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$musicBody$4(android.view.View view) {
        if (this.musicPlayer == null) {
            com.echoflow.chat.MusicPlayer musicPlayer = new com.echoflow.chat.MusicPlayer();
            this.musicPlayer = musicPlayer;
            musicPlayer.setListener(new com.echoflow.chat.MusicPlayer.OnStateListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda3
                @Override // com.echoflow.chat.MusicPlayer.OnStateListener
                public final void onStateChanged(boolean z, int i, int i2) {
                    com.echoflow.chat.PhoneAppActivity.this.lambda$musicBody$3(z, i, i2);
                }
            });
        }
        this.musicPlayer.play(this, com.echoflow.chat.MusicPlayer.builtinRes(), com.echoflow.chat.MusicPlayer.builtinTitle());
        startProgressTimer();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$musicBody$3(final boolean z, int i, int i2) {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.PhoneAppActivity.this.lambda$musicBody$2(z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$musicBody$2(boolean z) {
        this.playBtn.setText(z ? "❚❚" : "▶");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$musicBody$5(java.lang.String str, android.view.View view) {
        android.widget.Toast.makeText(this, com.echoflow.chat.CardStore.getCard(this, str) == null ? "" : com.echoflow.chat.CardStore.getCard(this, str).name + " 也在听这首", 0).show();
    }

    private java.lang.String songLine(java.lang.String str, com.echoflow.chat.Schedule.Now now, com.echoflow.chat.EmotionState emotionState) {
        if (str == null) {
            return "这首还行。";
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -307444203:
                if (str.equals("builtin_hakuyo")) {
                    c = 0;
                    break;
                }
                break;
            case -102216965:
                if (str.equals("builtin_rin")) {
                    c = 1;
                    break;
                }
                break;
            case 538132324:
                if (str.equals("builtin_alice")) {
                    c = 2;
                    break;
                }
                break;
            case 541826355:
                if (str.equals("builtin_elian")) {
                    c = 3;
                    break;
                }
                break;
            case 553915798:
                if (str.equals("builtin_rocco")) {
                    c = 4;
                    break;
                }
                break;
            case 1828583459:
                if (str.equals("builtin_yueling")) {
                    c = 5;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return "像长曝光的时候，等快门的那段时间。";
            case 1:
                return "节奏不错。跑步能跟上。";
            case 2:
                return "巡夜的时候听。这首正好。";
            case 3:
                return "店里放这首，客人会坐久一点。";
            case 4:
                return "这首我循环了一下午，零件都装错两次。";
            case 5:
                return "下雨的晚上听正合适。";
            default:
                return "这首还行。";
        }
    }

    private void startProgressTimer() {
        if (this.progressHandler == null) {
            this.progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        this.progressHandler.removeCallbacksAndMessages(null);
        this.progressHandler.post(new java.lang.Runnable() { // from class: com.echoflow.chat.PhoneAppActivity.2
            @Override // java.lang.Runnable
            public void run() {
                if (com.echoflow.chat.PhoneAppActivity.this.musicPlayer != null && com.echoflow.chat.PhoneAppActivity.this.seekBar != null && !com.echoflow.chat.PhoneAppActivity.this.dragging) {
                    int duration = com.echoflow.chat.PhoneAppActivity.this.musicPlayer.duration();
                    int position = com.echoflow.chat.PhoneAppActivity.this.musicPlayer.position();
                    if (duration > 0) {
                        com.echoflow.chat.PhoneAppActivity.this.seekBar.setProgress((position * 1000) / duration);
                        if (com.echoflow.chat.PhoneAppActivity.this.posText != null) {
                            com.echoflow.chat.PhoneAppActivity.this.posText.setText(com.echoflow.chat.PhoneAppActivity.this.fmt(position));
                        }
                        if (com.echoflow.chat.PhoneAppActivity.this.durText != null) {
                            com.echoflow.chat.PhoneAppActivity.this.durText.setText(com.echoflow.chat.PhoneAppActivity.this.fmt(duration));
                        }
                    }
                }
                com.echoflow.chat.PhoneAppActivity.this.progressHandler.postDelayed(this, 500L);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public java.lang.String fmt(int i) {
        int i2 = i / 1000;
        java.lang.StringBuilder append = new java.lang.StringBuilder().append(i2 / 60).append(":");
        int i3 = i2 % 60;
        return append.append(i3 < 10 ? "0" : "").append(i3).toString();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        android.os.Handler handler = this.progressHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        com.echoflow.chat.MusicPlayer musicPlayer = this.musicPlayer;
        if (musicPlayer != null) {
            musicPlayer.release();
            this.musicPlayer = null;
        }
    }

    private java.lang.String[] songsFor(java.lang.String str) {
        if (str == null) {
            return new java.lang.String[]{"无人知晓", "雨", "晚安"};
        }
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case -307444203:
                if (str.equals("builtin_hakuyo")) {
                    c = 0;
                    break;
                }
                break;
            case -102216965:
                if (str.equals("builtin_rin")) {
                    c = 1;
                    break;
                }
                break;
            case 538132324:
                if (str.equals("builtin_alice")) {
                    c = 2;
                    break;
                }
                break;
            case 541826355:
                if (str.equals("builtin_elian")) {
                    c = 3;
                    break;
                }
                break;
            case 553915798:
                if (str.equals("builtin_rocco")) {
                    c = 4;
                    break;
                }
                break;
            case 1828583459:
                if (str.equals("builtin_yueling")) {
                    c = 5;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return new java.lang.String[]{"长曝光", "银河系漫游", "凌晨四点"};
            case 1:
                return new java.lang.String[]{"竹刀与晨光", "不服气", "最后一届"};
            case 2:
                return new java.lang.String[]{"夜巡", "铁与花", "回到营房的路"};
            case 3:
                return new java.lang.String[]{"旧书页", "下午三点", "拾光"};
            case 4:
                return new java.lang.String[]{"齿轮转起来", "修理铺的下午", "油渍与咖啡"};
            case 5:
                return new java.lang.String[]{"灯", "路口", "天亮之前"};
            default:
                return new java.lang.String[]{"未命名 1", "未命名 2"};
        }
    }

    private android.view.View weatherBody() {
        java.lang.String str;
        java.lang.String str2;
        java.lang.String str3;
        com.echoflow.chat.Lorebook currentWorld;
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-15069136);
        linearLayout.setPadding(dp(20), dp(28), dp(20), dp(24));
        scrollView.addView(linearLayout);
        java.util.List<com.echoflow.chat.CharacterCard> recentCards = com.echoflow.chat.PhoneStore.recentCards(this);
        int i = 0;
        if (recentCards.isEmpty() || (currentWorld = com.echoflow.chat.Lorebook.currentWorld(this, recentCards.get(0).id)) == null) {
            str = null;
            str2 = null;
        } else {
            str2 = currentWorld.id;
            str = currentWorld.name;
        }
        com.echoflow.chat.BuiltinLorebooks.Weather weather = com.echoflow.chat.BuiltinLorebooks.todayWeather(str2, com.echoflow.chat.Fortune.today());
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(str == null ? "本地天气" : "「" + str + "」今天");
        textView.setTextSize(14.0f);
        textView.setTextColor(-855638017);
        linearLayout.addView(textView);
        int i2 = -1;
        if (weather != null) {
            android.widget.TextView textView2 = new android.widget.TextView(this);
            textView2.setText(weather.cond);
            textView2.setTextSize(40.0f);
            textView2.setTextColor(-1);
            textView2.setTypeface(null, 1);
            linearLayout.addView(textView2);
            android.widget.TextView textView3 = new android.widget.TextView(this);
            textView3.setText(weather.temp);
            textView3.setTextSize(17.0f);
            textView3.setTextColor(-1521542);
            textView3.setPadding(0, dp(4), 0, 0);
            linearLayout.addView(textView3);
            android.widget.TextView textView4 = new android.widget.TextView(this);
            textView4.setText(weather.detail);
            textView4.setTextSize(13.0f);
            textView4.setTextColor(-1426063361);
            textView4.setLineSpacing(dp(4), 1.0f);
            textView4.setPadding(0, dp(8), 0, dp(2));
            linearLayout.addView(textView4);
        } else {
            android.widget.TextView textView5 = new android.widget.TextView(this);
            textView5.setText("还没有世界书，无法判断那边的天气。");
            textView5.setTextSize(13.0f);
            textView5.setTextColor(-1426063361);
            linearLayout.addView(textView5);
        }
        android.widget.TextView textView6 = new android.widget.TextView(this);
        textView6.setText("这是角色所在世界的天气，不是手机所在地的天气。\n由「世界书」决定 —— 换一本世界书，天就换了。");
        textView6.setTextSize(11.0f);
        textView6.setTextColor(2013265919);
        textView6.setLineSpacing(dp(3), 1.0f);
        textView6.setPadding(0, dp(12), 0, dp(4));
        linearLayout.addView(textView6);
        android.widget.TextView textView7 = new android.widget.TextView(this);
        textView7.setText(new java.text.SimpleDateFormat("M月d日 HH:mm", java.util.Locale.CHINA).format(new java.util.Date()));
        textView7.setTextSize(12.0f);
        textView7.setTextColor(2013265919);
        textView7.setPadding(0, dp(4), 0, dp(24));
        linearLayout.addView(textView7);
        int i3 = 0;
        while (i3 < recentCards.size()) {
            com.echoflow.chat.CharacterCard characterCard = recentCards.get(i3);
            com.echoflow.chat.Lorebook currentWorld2 = com.echoflow.chat.Lorebook.currentWorld(this, characterCard.id);
            com.echoflow.chat.BuiltinLorebooks.Weather weather2 = com.echoflow.chat.BuiltinLorebooks.todayWeather(currentWorld2 == null ? null : currentWorld2.id, com.echoflow.chat.Fortune.today());
            android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
            linearLayout2.setOrientation(i);
            linearLayout2.setGravity(16);
            android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(i2, -2);
            layoutParams.bottomMargin = dp(12);
            linearLayout2.setLayoutParams(layoutParams);
            linearLayout2.setBackground(com.echoflow.chat.EfUi.roundRectPx(452984831, i, dp(14)));
            linearLayout2.setPadding(dp(14), dp(12), dp(14), dp(12));
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp(38), dp(38)));
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, characterCard.id);
            if (loadAvatar != null) {
                imageView.setImageBitmap(loadAvatar);
            } else {
                imageView.setImageResource(com.echoflow.chat.R.drawable.app_icon);
            }
            linearLayout2.addView(imageView);
            if (weather2 != null && weather2.lines.length > 0) {
                str3 = weather2.lines[java.lang.Math.abs((characterCard.id + com.echoflow.chat.Fortune.today()).hashCode()) % weather2.lines.length];
            } else {
                str3 = "今天天气还行。";
            }
            android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
            linearLayout3.setOrientation(1);
            linearLayout3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            linearLayout3.setPadding(dp(12), 0, 0, 0);
            android.widget.TextView textView8 = new android.widget.TextView(this);
            textView8.setText(characterCard.name);
            textView8.setTextSize(12.0f);
            textView8.setTextColor(-4609793);
            linearLayout3.addView(textView8);
            android.widget.TextView textView9 = new android.widget.TextView(this);
            textView9.setText(str3);
            textView9.setTextSize(13.0f);
            textView9.setTextColor(-285212673);
            textView9.setLineSpacing(dp(3), 1.0f);
            textView9.setPadding(0, dp(3), 0, 0);
            linearLayout3.addView(textView9);
            linearLayout2.addView(linearLayout3);
            linearLayout.addView(linearLayout2);
            i3++;
            i = 0;
            i2 = -1;
        }
        return scrollView;
    }

    private android.view.View notesBody() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-1);
        linearLayout.setPadding(dp(16), dp(14), dp(16), dp(20));
        scrollView.addView(linearLayout);
        linearLayout.addView(hint("她写过、但没发出去的话。只有你能看到。"));
        boolean z = false;
        for (com.echoflow.chat.CharacterCard characterCard : com.echoflow.chat.PhoneStore.recentCards(this)) {
            for (com.echoflow.chat.DiaryEntry diaryEntry : com.echoflow.chat.DiaryStore.list(this, characterCard.id)) {
                if (diaryEntry.unsaid != null && !diaryEntry.unsaid.trim().isEmpty()) {
                    android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
                    linearLayout2.setOrientation(1);
                    android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
                    layoutParams.topMargin = dp(12);
                    linearLayout2.setLayoutParams(layoutParams);
                    linearLayout2.setBackground(com.echoflow.chat.EfUi.roundRectPx(-1818, 870893690, dp(12)));
                    linearLayout2.setPadding(dp(14), dp(12), dp(14), dp(12));
                    android.widget.TextView textView = new android.widget.TextView(this);
                    textView.setText(characterCard.name + " · " + diaryEntry.date);
                    textView.setTextSize(11.0f);
                    textView.setTextColor(-5207490);
                    linearLayout2.addView(textView);
                    android.widget.TextView textView2 = new android.widget.TextView(this);
                    textView2.setText(diaryEntry.unsaid);
                    textView2.setTextSize(14.0f);
                    textView2.setTextColor(-12964332);
                    textView2.setPadding(0, dp(6), 0, 0);
                    textView2.setLineSpacing(dp(3), 1.0f);
                    linearLayout2.addView(textView2);
                    linearLayout.addView(linearLayout2);
                    z = true;
                }
            }
        }
        if (!z) {
            linearLayout.addView(hint("暂时还没有。和她多聊几天，这里会有东西。"));
        }
        return scrollView;
    }

    private android.view.View calcBody() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundColor(-15658735);
        linearLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
        android.widget.TextView textView = new android.widget.TextView(this);
        this.calcDisplay = textView;
        textView.setText("0");
        this.calcDisplay.setTextSize(46.0f);
        this.calcDisplay.setTextColor(-1);
        this.calcDisplay.setGravity(8388693);
        this.calcDisplay.setPadding(dp(20), dp(20), dp(20), dp(24));
        this.calcDisplay.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 0, 1.6f));
        linearLayout.addView(this.calcDisplay);
        java.lang.String[][] strArr = {new java.lang.String[]{"C", "±", "%", "÷"}, new java.lang.String[]{"7", "8", "9", "×"}, new java.lang.String[]{"4", "5", "6", "−"}, new java.lang.String[]{"1", "2", "3", "+"}, new java.lang.String[]{"0", ".", "⌫", "="}};
        for (int i = 0; i < 5; i++) {
            java.lang.String[] strArr2 = strArr[i];
            android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
            linearLayout2.setOrientation(0);
            linearLayout2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
            for (java.lang.String str : strArr2) {
                linearLayout2.addView(calcKey(str));
            }
            linearLayout.addView(linearLayout2);
        }
        return linearLayout;
    }

    private android.view.View calcKey(final java.lang.String str) {
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(str);
        textView.setTextSize(22.0f);
        textView.setGravity(17);
        textView.setTextColor(-1);
        if ("÷×−+=".contains(str)) {
            textView.setBackgroundColor(-24822);
        } else if ("C±%".contains(str)) {
            textView.setBackgroundColor(-11908534);
        } else {
            textView.setBackgroundColor(-13882322);
        }
        textView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -1, 1.0f));
        textView.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneAppActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.PhoneAppActivity.this.lambda$calcKey$6(str, view);
            }
        });
        return textView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$calcKey$6(java.lang.String str, android.view.View view) {
        onCalcKey(str);
    }

    private void onCalcKey(java.lang.String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case kotlinx.coroutines.internal.LockFreeTaskQueueCore.CLOSED_SHIFT /* 61 */:
                if (str.equals("=")) {
                    c = 0;
                    break;
                }
                break;
            case 67:
                if (str.equals("C")) {
                    c = 1;
                    break;
                }
                break;
            case 9003:
                if (str.equals("⌫")) {
                    c = 2;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                java.lang.String eval = com.echoflow.chat.CalcEngine.eval(this.calcExpr.toString());
                this.calcDisplay.setText(eval);
                this.calcExpr.setLength(0);
                java.lang.StringBuilder sb = this.calcExpr;
                if ("0".equals(eval)) {
                    eval = "";
                }
                sb.append(eval);
                return;
            case 1:
                this.calcExpr.setLength(0);
                this.calcDisplay.setText("0");
                return;
            case 2:
                if (this.calcExpr.length() > 0) {
                    java.lang.StringBuilder sb2 = this.calcExpr;
                    sb2.setLength(sb2.length() - 1);
                }
                this.calcDisplay.setText(this.calcExpr.length() != 0 ? this.calcExpr.toString() : "0");
                return;
            default:
                this.calcExpr.append(str);
                this.calcDisplay.setText(this.calcExpr.toString());
                return;
        }
    }

    private android.widget.TextView hint(java.lang.String str) {
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(str);
        textView.setTextSize(13.0f);
        textView.setTextColor(-6710887);
        textView.setLineSpacing(dp(4), 1.0f);
        textView.setPadding(dp(4), dp(8), dp(4), dp(8));
        return textView;
    }

    private void showMusicDetail(java.lang.String str, java.lang.String str2) {
        com.echoflow.chat.Schedule.Now now = com.echoflow.chat.Schedule.now(com.echoflow.chat.Schedule.load(this, str2));
        com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, str2);
        java.lang.String[] songsFor = songsFor(str2);
        java.lang.StringBuilder sb = new java.lang.StringBuilder("此刻：");
        sb.append(now.what == null ? "空闲" : now.what).append("\n情绪：");
        sb.append((loadEmotion == null || loadEmotion.mood == null) ? "平静" : loadEmotion.mood).append("\n\n歌单：\n");
        for (java.lang.String str3 : songsFor) {
            sb.append("♪ ").append(str3).append("\n");
        }
        sb.append("\n（播放功能还没接 —— 这里只是她的歌单。要真的放音得接一个音频源，那是另一件事。）");
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(str + " 在听").setMessage(sb.toString()).setPositiveButton("好", (android.content.DialogInterface.OnClickListener) null).show();
    }

    private void showPhoto(java.lang.String str, java.lang.String str2, int i) {
        android.widget.ImageView imageView = new android.widget.ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        if (i == 0) {
            android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, str2);
            if (loadAvatar != null) {
                imageView.setImageBitmap(loadAvatar);
            }
        } else {
            imageView.setImageResource(i);
        }
        int dp = dp(8);
        imageView.setPadding(dp, dp, dp, dp);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(str + " 的照片").setView(imageView).setPositiveButton("关", (android.content.DialogInterface.OnClickListener) null).show();
    }

    private int dp(int i) {
        return java.lang.Math.round(i * getResources().getDisplayMetrics().density);
    }
}
