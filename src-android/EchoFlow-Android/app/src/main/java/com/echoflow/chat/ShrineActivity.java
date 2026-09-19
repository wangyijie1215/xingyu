package com.echoflow.chat;

/* loaded from: classes.dex */
public class ShrineActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private com.echoflow.chat.EmotionState emo;
    private com.echoflow.chat.Fortune fortune;
    private com.echoflow.chat.Persona persona;
    private com.echoflow.chat.RelationshipState rel;
    private android.widget.LinearLayout root;
    private com.echoflow.chat.ShrineState shrine;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        com.echoflow.chat.CharacterCard card = com.echoflow.chat.CardStore.getCard(this, getIntent().getStringExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID));
        this.card = card;
        if (card == null) {
            android.widget.Toast.makeText(this, "角色不存在", 0).show();
            finish();
            return;
        }
        java.util.List<com.echoflow.chat.Persona> listPersonas = com.echoflow.chat.CardStore.listPersonas(this);
        this.persona = (listPersonas == null || listPersonas.isEmpty()) ? new com.echoflow.chat.Persona() : listPersonas.get(0);
        this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        this.shrine = com.echoflow.chat.ShrineStore.load(this, this.card.id);
        java.lang.String baseUrl = (this.card.baseUrl == null || this.card.baseUrl.isEmpty()) ? com.echoflow.chat.ChatStore.getBaseUrl(this) : this.card.baseUrl;
        java.lang.String model = (this.card.model == null || this.card.model.isEmpty()) ? com.echoflow.chat.ChatStore.getModel(this) : this.card.model;
        com.echoflow.chat.FortuneService.currentCtx = getApplicationContext();
        this.fortune = com.echoflow.chat.FortuneService.todayOrDraw(this, this.card, this.persona, baseUrl, com.echoflow.chat.SecureStore.get(this), model, this.rel, this.shrine, this.emo, new java.lang.Runnable() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.ShrineActivity.this.lambda$onCreate$1();
            }
        });
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1() {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.ShrineActivity.this.lambda$onCreate$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0() {
        this.shrine = com.echoflow.chat.ShrineStore.load(this, this.card.id);
        this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        com.echoflow.chat.CharacterCard characterCard = this.card;
        if (characterCard != null) {
            this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, characterCard.id);
            this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
            this.shrine = com.echoflow.chat.ShrineStore.load(this, this.card.id);
            this.fortune = com.echoflow.chat.FortuneService.load(this, this.card.id, com.echoflow.chat.Fortune.today());
            if (this.root != null) {
                buildUi();
            }
        }
    }

    private void buildUi() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, com.echoflow.chat.R.color.ef_shrine_night));
        scrollView.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        scrollView.addView(linearLayout);
        linearLayout.addView(heroBlock());
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        this.root = linearLayout2;
        linearLayout2.setOrientation(1);
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        this.root.setPadding(dp, dp, dp, com.echoflow.chat.EfUi.dp(this, 40.0f));
        linearLayout.addView(this.root);
        this.root.addView(todayCard());
        this.root.addView(attendantBlock());
        this.root.addView(omamoriSection());
        this.root.addView(emaSection());
        this.root.addView(weekSection());
        setContentView(scrollView);
    }

    private android.view.View heroBlock() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
        frameLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 260.0f)));
        android.widget.ImageView imageView = new android.widget.ImageView(this);
        imageView.setImageResource(com.echoflow.chat.R.drawable.ef_shrine_hero);
        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        frameLayout.addView(imageView, new android.widget.FrameLayout.LayoutParams(-1, -1));
        android.view.View view = new android.view.View(this);
        view.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{920082, 1426983442, -871495150, -15857134}));
        frameLayout.addView(view, new android.widget.FrameLayout.LayoutParams(-1, -1));
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        android.widget.FrameLayout.LayoutParams layoutParams = new android.widget.FrameLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        layoutParams.rightMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout2.setLayoutParams(layoutParams);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "‹", 24.0f, -1, false);
        text.setGravity(17);
        text.setBackground(com.echoflow.chat.EfUi.roundRectPx(-2062544872, 704643071, com.echoflow.chat.EfUi.dp(this, 999.0f)));
        int dp = com.echoflow.chat.EfUi.dp(this, 36.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view2) {
                com.echoflow.chat.ShrineActivity.this.lambda$heroBlock$2(view2);
            }
        });
        linearLayout2.addView(text);
        linearLayout2.addView(new android.view.View(this), new android.widget.LinearLayout.LayoutParams(0, 1, 1.0f));
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "绘马架", 12.0f, -1521542, true);
        text2.setGravity(17);
        text2.setBackground(com.echoflow.chat.EfUi.roundRectPx(-2062544872, 704643071, com.echoflow.chat.EfUi.dp(this, 999.0f)));
        text2.setPadding(com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 8.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 8.0f));
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view2) {
                com.echoflow.chat.ShrineActivity.this.lambda$heroBlock$3(view2);
            }
        });
        linearLayout2.addView(text2);
        frameLayout.addView(linearLayout2);
        linearLayout.addView(frameLayout);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$heroBlock$2(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$heroBlock$3(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.EmaActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    private android.view.View todayCard() {
        android.widget.LinearLayout.LayoutParams layoutParams;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_shrine_card);
        int dp = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "银月神社 · 第 " + this.fortune.slipNo + " 番", 11.0f, -1521542, true);
        text.setLetterSpacing(0.12f);
        linearLayout3.addView(text);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "今日运势", 20.0f, -1, true));
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, dateLine(), 12.0f, -1275068417, false);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 6.0f);
        text2.setLayoutParams(layoutParams2);
        linearLayout3.addView(text2);
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(16);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout4.setLayoutParams(layoutParams3);
        int i = 0;
        while (i < 7) {
            android.view.View view = new android.view.View(this);
            android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 9.0f), com.echoflow.chat.EfUi.dp(this, 9.0f));
            layoutParams4.rightMargin = com.echoflow.chat.EfUi.dp(this, 5.0f);
            view.setLayoutParams(layoutParams4);
            view.setBackground(com.echoflow.chat.EfUi.roundRectPx(i < this.shrine.streak ? -1521542 : 536870911, 0, com.echoflow.chat.EfUi.dp(this, 5.0f)));
            linearLayout4.addView(view);
            i++;
        }
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, "连续参拜 " + this.shrine.streak + " 天", 11.0f, -1521542, true);
        text3.setPadding(com.echoflow.chat.EfUi.dp(this, 6.0f), 0, 0, 0);
        linearLayout4.addView(text3);
        linearLayout3.addView(linearLayout4);
        linearLayout2.addView(linearLayout3, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        linearLayout2.addView(sealView());
        linearLayout.addView(linearLayout2);
        android.view.View view2 = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 1.0f));
        layoutParams5.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        layoutParams5.bottomMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        view2.setLayoutParams(layoutParams5);
        view2.setBackgroundColor(870893690);
        linearLayout.addView(view2);
        android.widget.LinearLayout linearLayout5 = new android.widget.LinearLayout(this);
        linearLayout5.setOrientation(0);
        android.widget.LinearLayout linearLayout6 = new android.widget.LinearLayout(this);
        linearLayout6.setOrientation(1);
        linearLayout6.addView(com.echoflow.chat.EfUi.text(this, "今日签运", 12.0f, -1711276033, false));
        linearLayout6.addView(com.echoflow.chat.EfUi.text(this, this.fortune.rank, 26.0f, rankColorA(), true));
        linearLayout5.addView(linearLayout6, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.LinearLayout linearLayout7 = new android.widget.LinearLayout(this);
        linearLayout7.setOrientation(1);
        linearLayout7.setGravity(androidx.core.view.GravityCompat.END);
        linearLayout7.addView(com.echoflow.chat.EfUi.text(this, "综合", 12.0f, -1711276033, false));
        linearLayout7.addView(com.echoflow.chat.EfUi.text(this, java.lang.String.valueOf(this.fortune.score), 26.0f, -1521542, true));
        linearLayout5.addView(linearLayout7);
        linearLayout.addView(linearLayout5);
        android.widget.LinearLayout linearLayout8 = new android.widget.LinearLayout(this);
        linearLayout8.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams6 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams6.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout8.setLayoutParams(layoutParams6);
        android.widget.TextView text4 = com.echoflow.chat.EfUi.text(this, "查看今日签文", 12.0f, -1, true);
        text4.setGravity(17);
        text4.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text4.setPadding(com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 9.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 9.0f));
        text4.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view3) {
                com.echoflow.chat.ShrineActivity.this.lambda$todayCard$4(view3);
            }
        });
        linearLayout8.addView(text4);
        android.widget.TextView text5 = com.echoflow.chat.EfUi.text(this, "再摇一支", 12.0f, -1, true);
        text5.setGravity(17);
        text5.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_chip);
        text5.setPadding(com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 9.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 9.0f));
        if (((android.widget.LinearLayout.LayoutParams) text5.getLayoutParams()) == null) {
            layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
        } else {
            layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
        }
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        text5.setLayoutParams(layoutParams);
        text5.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view3) {
                com.echoflow.chat.ShrineActivity.this.lambda$todayCard$5(view3);
            }
        });
        linearLayout8.addView(text5);
        linearLayout.addView(linearLayout8);
        android.widget.LinearLayout linearLayout9 = new android.widget.LinearLayout(this);
        linearLayout9.setOrientation(1);
        linearLayout9.addView(linearLayout);
        return linearLayout9;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$todayCard$4(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.FortuneActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$todayCard$5(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.OmikujiActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    private android.view.View sealView() {
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "已\n参拜", 13.0f, -1747893, true);
        text.setGravity(17);
        text.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_seal);
        int dp = com.echoflow.chat.EfUi.dp(this, 54.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        text.setRotation(-6.0f);
        return text;
    }

    private android.view.View attendantBlock() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "陪你来参拜", "她的反应随签运变化", null));
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setBackground(com.echoflow.chat.EfUi.roundRectPx(352321535, 352321535, com.echoflow.chat.EfUi.dp(this, 20.0f)));
        int dp = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout2.setPadding(dp, dp, dp, dp);
        android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, this.card.id);
        if (loadAvatar != null) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setImageBitmap(loadAvatar);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            int dp2 = com.echoflow.chat.EfUi.dp(this, 76.0f);
            imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
            imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(0, 704643071, com.echoflow.chat.EfUi.dp(this, 24.0f)));
            imageView.setClipToOutline(true);
            linearLayout2.addView(imageView);
        }
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, attendantLine(), 14.0f, -1, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 6.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        text.setLayoutParams(layoutParams);
        linearLayout2.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "—— " + this.card.name + " · 关系「" + this.rel.levelName() + "」", 11.0f, -1521542, true);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        text2.setLayoutParams(layoutParams2);
        text2.setGravity(androidx.core.view.GravityCompat.END);
        linearLayout2.addView(text2);
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    private java.lang.String attendantLine() {
        com.echoflow.chat.Fortune fortune = this.fortune;
        if (fortune == null) {
            return "（她站在鸟居下面等你，没有催促。）";
        }
        java.lang.String str = fortune.rank;
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case 21513:
                if (str.equals("吉")) {
                    c = 0;
                    break;
                }
                break;
            case 641916:
                if (str.equals("中吉")) {
                    c = 1;
                    break;
                }
                break;
            case 729026:
                if (str.equals("大吉")) {
                    c = 2;
                    break;
                }
                break;
            case 752090:
                if (str.equals("小吉")) {
                    c = 3;
                    break;
                }
                break;
            case 840254:
                if (str.equals("末吉")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return "「……签我替你看了。别站在雨里，先过来。」";
            case 1:
                return "「不急。」她把伞往你那边偏了偏。「中吉也是吉。」";
            case 2:
                return "「……签我替你看了。」她说完就把签纸塞给你，耳尖有点红。「是好签。别浪费。」";
            case 3:
                return "「小吉。」她顿了顿，「够用了。走吧。」";
            case 4:
                return "「……有点阻滞。」她看了你一眼，「那就慢一点走。」";
            default:
                return "「……今天风大。」她把签纸收进袖口，「要走哪条路，我陪你。」";
        }
    }

    private android.view.View omamoriSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "御守", "御守箱 " + this.shrine.unlocked.size() + " / " + com.echoflow.chat.FortuneService.OMAMORI.length, null));
        android.widget.HorizontalScrollView horizontalScrollView = new android.widget.HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        horizontalScrollView.addView(linearLayout2);
        for (final com.echoflow.chat.FortuneService.Omamori omamori : com.echoflow.chat.FortuneService.OMAMORI) {
            boolean contains = this.shrine.unlocked.contains(omamori.id);
            boolean equals = omamori.id.equals(this.shrine.equipped);
            android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
            linearLayout3.setOrientation(1);
            linearLayout3.setGravity(1);
            linearLayout3.setBackgroundResource(equals ? com.echoflow.chat.R.drawable.ef_bg_omamori_on : com.echoflow.chat.R.drawable.ef_bg_omamori);
            android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 96.0f), -2);
            layoutParams.rightMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
            linearLayout3.setLayoutParams(layoutParams);
            linearLayout3.setPadding(com.echoflow.chat.EfUi.dp(this, 8.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 8.0f), com.echoflow.chat.EfUi.dp(this, 11.0f));
            linearLayout3.setAlpha(contains ? 1.0f : 0.45f);
            android.view.View view = new android.view.View(this);
            int dp = com.echoflow.chat.EfUi.dp(this, 22.0f);
            view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
            android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{-1521542, -5206724});
            gradientDrawable.setShape(1);
            view.setBackground(gradientDrawable);
            linearLayout3.addView(view);
            android.widget.TextView text = com.echoflow.chat.EfUi.text(this, omamori.name, 12.0f, -1521542, true);
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
            layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 7.0f);
            text.setLayoutParams(layoutParams2);
            linearLayout3.addView(text);
            android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, contains ? omamori.effect : omamori.hint, 9.0f, contains ? 1946157055 : -9475445, false);
            text2.setGravity(17);
            text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 2.0f), 1.0f);
            android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
            text2.setLayoutParams(layoutParams3);
            linearLayout3.addView(text2);
            if (contains) {
                linearLayout3.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda6
                    @Override // android.view.View.OnClickListener
                    public final void onClick(android.view.View view2) {
                        com.echoflow.chat.ShrineActivity.this.lambda$omamoriSection$6(omamori, view2);
                    }
                });
            }
            linearLayout2.addView(linearLayout3);
        }
        linearLayout.addView(horizontalScrollView);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$omamoriSection$6(com.echoflow.chat.FortuneService.Omamori omamori, android.view.View view) {
        if (omamori.id.equals(this.shrine.equipped)) {
            com.echoflow.chat.ShrineStore.equip(this, this.shrine, "");
        } else {
            com.echoflow.chat.ShrineStore.equip(this, this.shrine, omamori.id);
            android.widget.Toast.makeText(this, "已佩戴「" + omamori.name + "」：" + omamori.effect, 0).show();
        }
        this.shrine = com.echoflow.chat.ShrineStore.load(this, this.card.id);
        buildUi();
    }

    private android.view.View emaSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "绘马", "去写一块 ›", new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.ShrineActivity.this.lambda$emaSection$7(view);
            }
        }));
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.TextView bodyDim = com.echoflow.chat.EfUi.bodyDim(this, "写在绘马上的愿望会进入她的长期记忆。她会在之后的日子里，偶尔提起它。");
        bodyDim.setTextColor(-1521542);
        card.addView(bodyDim);
        card.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.ShrineActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.ShrineActivity.this.lambda$emaSection$8(view);
            }
        });
        linearLayout.addView(card);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$emaSection$7(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.EmaActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$emaSection$8(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.EmaActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    private android.view.View weekSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        int i = 1;
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "最近七日", "", null));
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(80);
        int i2 = -1;
        linearLayout2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 76.0f)));
        if (this.shrine.history.isEmpty()) {
            linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "从今天开始记录。", 12.0f, -9475445, false));
        } else {
            java.util.Iterator<java.lang.String> it = this.shrine.history.iterator();
            while (it.hasNext()) {
                java.lang.String[] split = it.next().split("\\|");
                java.lang.String str = split.length > 0 ? split[0] : "";
                java.lang.String str2 = split.length > i ? split[i] : "中吉";
                android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
                linearLayout3.setOrientation(i);
                linearLayout3.setGravity(81);
                linearLayout3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, i2, 1.0f));
                android.view.View view = new android.view.View(this);
                view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 18.0f), com.echoflow.chat.EfUi.dp(this, barHeight(str2))));
                android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP, new int[]{rankColorA(str2), rankColorB(str2)});
                gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 5.0f));
                view.setBackground(gradientDrawable);
                linearLayout3.addView(view);
                if (str.length() >= 10) {
                    str = str.substring(5).replace('-', '/');
                }
                android.widget.TextView text = com.echoflow.chat.EfUi.text(this, str, 9.0f, -9475445, false);
                android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
                layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 5.0f);
                text.setLayoutParams(layoutParams);
                linearLayout3.addView(text);
                linearLayout2.addView(linearLayout3);
                i = 1;
                i2 = -1;
            }
        }
        card.addView(linearLayout2);
        if (!this.shrine.history.isEmpty()) {
            android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "七日里 " + tally() + "。她说：「有起有落才正常。」", 12.0f, -1711276033, false);
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
            text2.setLayoutParams(layoutParams2);
            card.addView(text2);
        }
        linearLayout.addView(card);
        return linearLayout;
    }

    private java.lang.String tally() {
        java.util.LinkedHashMap linkedHashMap = new java.util.LinkedHashMap();
        java.util.Iterator<java.lang.String> it = this.shrine.history.iterator();
        while (it.hasNext()) {
            java.lang.String[] split = it.next().split("\\|");
            if (split.length > 1) {
                java.lang.Integer num = (java.lang.Integer) linkedHashMap.get(split[1]);
                linkedHashMap.put(split[1], java.lang.Integer.valueOf(num != null ? 1 + num.intValue() : 1));
            }
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        for (java.lang.Object strObj : linkedHashMap.keySet()) {
            java.lang.String str = (java.lang.String) strObj;
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(str).append(" ").append(linkedHashMap.get(str)).append(" 次");
        }
        return sb.toString();
    }

    private int barHeight(java.lang.String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case 21513:
                if (str.equals("吉")) {
                    c = 0;
                    break;
                }
                break;
            case 641916:
                if (str.equals("中吉")) {
                    c = 1;
                    break;
                }
                break;
            case 729026:
                if (str.equals("大吉")) {
                    c = 2;
                    break;
                }
                break;
            case 752090:
                if (str.equals("小吉")) {
                    c = 3;
                    break;
                }
                break;
            case 840254:
                if (str.equals("末吉")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return 56;
            case 1:
                return 44;
            case 2:
                return 64;
            case 3:
                return 34;
            case 4:
                return 26;
            default:
                return 22;
        }
    }

    private int rankColorA() {
        com.echoflow.chat.Fortune fortune = this.fortune;
        return rankColorA(fortune == null ? "中吉" : fortune.rank);
    }

    private int rankColorA(java.lang.String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case 21513:
                if (str.equals("吉")) {
                    c = 0;
                    break;
                }
                break;
            case 641916:
                if (str.equals("中吉")) {
                    c = 1;
                    break;
                }
                break;
            case 729026:
                if (str.equals("大吉")) {
                    c = 2;
                    break;
                }
                break;
            case 752090:
                if (str.equals("小吉")) {
                    c = 3;
                    break;
                }
                break;
            case 840254:
                if (str.equals("末吉")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return -3905493;
            case 1:
                return -9745472;
            case 2:
                return -3565022;
            case 3:
                return -13725832;
            case 4:
                return -11054470;
            default:
                return -11904390;
        }
    }

    private int rankColorB(java.lang.String str) {
        str.hashCode();
        char c = 65535;
        switch (str.hashCode()) {
            case 21513:
                if (str.equals("吉")) {
                    c = 0;
                    break;
                }
                break;
            case 641916:
                if (str.equals("中吉")) {
                    c = 1;
                    break;
                }
                break;
            case 729026:
                if (str.equals("大吉")) {
                    c = 2;
                    break;
                }
                break;
            case 752090:
                if (str.equals("小吉")) {
                    c = 3;
                    break;
                }
                break;
            case 840254:
                if (str.equals("末吉")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                return -1531293;
            case 1:
                return -5665793;
            case 2:
                return -1521542;
            case 3:
                return -8396604;
            case 4:
                return -6514755;
            default:
                return -7362871;
        }
    }

    private java.lang.String dateLine() {
        return new java.text.SimpleDateFormat("yyyy年M月d日 EEEE", java.util.Locale.CHINA).format(new java.util.Date());
    }
}
