package com.echoflow.chat;

/* loaded from: classes.dex */
public class FortuneActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private com.echoflow.chat.EmotionState emo;
    private com.echoflow.chat.Fortune fortune;
    private com.echoflow.chat.RelationshipState rel;

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
        com.echoflow.chat.Fortune load = com.echoflow.chat.FortuneService.load(this, card.id, com.echoflow.chat.Fortune.today());
        this.fortune = load;
        if (load == null) {
            java.util.List<com.echoflow.chat.Persona> listPersonas = com.echoflow.chat.CardStore.listPersonas(this);
            com.echoflow.chat.Persona persona = (listPersonas == null || listPersonas.isEmpty()) ? new com.echoflow.chat.Persona() : listPersonas.get(0);
            this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
            this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
            com.echoflow.chat.ShrineState load2 = com.echoflow.chat.ShrineStore.load(this, this.card.id);
            java.lang.String baseUrl = (this.card.baseUrl == null || this.card.baseUrl.isEmpty()) ? com.echoflow.chat.ChatStore.getBaseUrl(this) : this.card.baseUrl;
            java.lang.String model = (this.card.model == null || this.card.model.isEmpty()) ? com.echoflow.chat.ChatStore.getModel(this) : this.card.model;
            com.echoflow.chat.FortuneService.currentCtx = getApplicationContext();
            this.fortune = com.echoflow.chat.FortuneService.todayOrDraw(this, this.card, persona, baseUrl, com.echoflow.chat.SecureStore.get(this), model, this.rel, load2, this.emo, null);
        }
        this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        com.echoflow.chat.Fortune load;
        super.onResume();
        com.echoflow.chat.CharacterCard characterCard = this.card;
        if (characterCard == null || (load = com.echoflow.chat.FortuneService.load(this, characterCard.id, com.echoflow.chat.Fortune.today())) == null || load.reading == null || load.reading.isEmpty()) {
            return;
        }
        if (this.fortune.reading == null || this.fortune.reading.isEmpty()) {
            this.fortune = load;
            buildUi();
        }
    }

    private void buildUi() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, com.echoflow.chat.R.color.ef_night_800));
        scrollView.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        scrollView.addView(linearLayout);
        linearLayout.addView(topBar());
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        linearLayout2.setPadding(dp, 0, dp, com.echoflow.chat.EfUi.dp(this, 32.0f));
        linearLayout.addView(linearLayout2);
        linearLayout2.addView(washiSlip());
        linearLayout2.addView(readingBlock());
        linearLayout2.addView(appliedBlock());
        linearLayout2.addView(actions());
        setContentView(scrollView);
    }

    private android.view.View topBar() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        int dp = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout.setPadding(dp, dp, dp, com.echoflow.chat.EfUi.dp(this, 10.0f));
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "‹", 24.0f, -724225, false);
        text.setGravity(17);
        int dp2 = com.echoflow.chat.EfUi.dp(this, 36.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.FortuneActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.FortuneActivity.this.lambda$topBar$0(view);
            }
        });
        linearLayout.addView(text);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout2.setLayoutParams(layoutParams);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "银月神社", 11.0f, -9475445, false));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "今日签文", 24.0f, -724225, true));
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$0(android.view.View view) {
        finish();
    }

    private android.view.View washiSlip() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_washi);
        int dp = com.echoflow.chat.EfUi.dp(this, 18.0f);
        linearLayout.setPadding(dp, com.echoflow.chat.EfUi.dp(this, 20.0f), dp, dp);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "第 " + this.fortune.slipNoCn() + " 番", 11.0f, -9740724, false), new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, this.fortune.dateLabel(), 11.0f, -9740724, false);
        text.setGravity(androidx.core.view.GravityCompat.END);
        linearLayout2.addView(text);
        linearLayout.addView(linearLayout2);
        android.view.View view = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 1.0f));
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        layoutParams.bottomMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        view.setLayoutParams(layoutParams);
        view.setBackgroundColor(775565608);
        linearLayout.addView(view);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setGravity(1);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "御\u3000籤", 11.0f, -9740724, false);
        text2.setLetterSpacing(0.34f);
        linearLayout3.addView(text2);
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, this.fortune.rank, 62.0f, rankColor(), true);
        text3.setLetterSpacing(0.06f);
        text3.setGravity(17);
        linearLayout3.addView(text3);
        android.widget.TextView text4 = com.echoflow.chat.EfUi.text(this, this.fortune.summary, 12.0f, -9740724, false);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        text4.setLayoutParams(layoutParams2);
        linearLayout3.addView(text4);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        layoutParams3.bottomMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout3.setLayoutParams(layoutParams3);
        linearLayout.addView(linearLayout3);
        for (com.echoflow.chat.Fortune.Dim dim : this.fortune.dimensions) {
            android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
            linearLayout4.setOrientation(0);
            linearLayout4.setGravity(16);
            linearLayout4.setPadding(0, com.echoflow.chat.EfUi.dp(this, 9.0f), 0, com.echoflow.chat.EfUi.dp(this, 9.0f));
            android.widget.TextView text5 = com.echoflow.chat.EfUi.text(this, dim.key, 12.0f, -12963544, true);
            text5.setWidth(com.echoflow.chat.EfUi.dp(this, 62.0f));
            linearLayout4.addView(text5);
            linearLayout4.addView(com.echoflow.chat.EfUi.segmentBar(this, dim.value, 5), new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            android.widget.TextView text6 = com.echoflow.chat.EfUi.text(this, java.lang.String.valueOf(dim.value), 11.0f, -9740724, false);
            text6.setGravity(androidx.core.view.GravityCompat.END);
            text6.setWidth(com.echoflow.chat.EfUi.dp(this, 40.0f));
            linearLayout4.addView(text6);
            linearLayout.addView(linearLayout4);
        }
        android.widget.LinearLayout linearLayout5 = new android.widget.LinearLayout(this);
        linearLayout5.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout5.setLayoutParams(layoutParams4);
        linearLayout5.addView(metaCell("幸运物", this.fortune.luckyItem, 0));
        linearLayout5.addView(metaCell("幸运色", this.fortune.luckyColorName, parseColor(this.fortune.luckyColorHex)));
        linearLayout.addView(linearLayout5);
        android.widget.LinearLayout linearLayout6 = new android.widget.LinearLayout(this);
        linearLayout6.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams5.topMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        linearLayout6.setLayoutParams(layoutParams5);
        linearLayout6.addView(metaCell("幸运方位", this.fortune.luckyDirection, 0));
        linearLayout6.addView(metaCell("幸运数字", java.lang.String.valueOf(this.fortune.luckyNumber), 0));
        linearLayout.addView(linearLayout6);
        android.widget.LinearLayout linearLayout7 = new android.widget.LinearLayout(this);
        linearLayout7.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams6 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams6.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        linearLayout7.setLayoutParams(layoutParams6);
        linearLayout7.addView(yijiCell("宜", this.fortune.good, true));
        linearLayout7.addView(yijiCell("忌", this.fortune.bad, false));
        linearLayout.addView(linearLayout7);
        android.widget.TextView text7 = com.echoflow.chat.EfUi.text(this, "银 月 神 社 · 御 籤 · 第 " + this.fortune.slipNo + " 番", 10.0f, -9740724, false);
        text7.setGravity(17);
        text7.setLetterSpacing(0.1f);
        android.widget.LinearLayout.LayoutParams layoutParams7 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams7.topMargin = com.echoflow.chat.EfUi.dp(this, 16.0f);
        text7.setLayoutParams(layoutParams7);
        linearLayout.addView(text7);
        return linearLayout;
    }

    private android.view.View metaCell(java.lang.String str, java.lang.String str2, int i) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(com.echoflow.chat.EfUi.roundRectPx(255471912, 440021288, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        int dp = com.echoflow.chat.EfUi.dp(this, 10.0f);
        linearLayout.setPadding(dp, com.echoflow.chat.EfUi.dp(this, 8.0f), dp, com.echoflow.chat.EfUi.dp(this, 8.0f));
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.rightMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, str, 10.0f, -9740724, false));
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        if (i != 0) {
            android.view.View view = new android.view.View(this);
            int dp2 = com.echoflow.chat.EfUi.dp(this, 10.0f);
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(dp2, dp2);
            layoutParams2.rightMargin = com.echoflow.chat.EfUi.dp(this, 5.0f);
            view.setLayoutParams(layoutParams2);
            view.setBackground(com.echoflow.chat.EfUi.roundRectPx(i, 771751936, com.echoflow.chat.EfUi.dp(this, 3.0f)));
            linearLayout2.addView(view);
        }
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, str2, 13.0f, -12963544, true));
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 2.0f);
        linearLayout2.setLayoutParams(layoutParams3);
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    private android.view.View yijiCell(java.lang.String str, java.util.List<java.lang.String> list, boolean z) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        int i = z ? 439259000 : 399002172;
        int i2 = z ? 1110347640 : 1036536380;
        int i3 = z ? -14718120 : -7458265;
        linearLayout.setBackground(com.echoflow.chat.EfUi.roundRectPx(i, i2, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        int dp = com.echoflow.chat.EfUi.dp(this, 11.0f);
        linearLayout.setPadding(dp, com.echoflow.chat.EfUi.dp(this, 9.0f), dp, com.echoflow.chat.EfUi.dp(this, 9.0f));
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.rightMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout.setLayoutParams(layoutParams);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, str, 10.0f, i3, true);
        text.setLetterSpacing(0.18f);
        linearLayout.addView(text);
        java.util.Iterator<java.lang.String> it = list.iterator();
        while (it.hasNext()) {
            android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, it.next(), 12.0f, i3, false);
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
            layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 3.0f);
            text2.setLayoutParams(layoutParams2);
            linearLayout.addView(text2);
        }
        return linearLayout;
    }

    private android.view.View readingBlock() {
        java.lang.String str;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{714832895, 401131642});
        gradientDrawable.setStroke(1, 1206438010);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 20.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, this.card.id);
        if (loadAvatar != null) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setImageBitmap(loadAvatar);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            int dp2 = com.echoflow.chat.EfUi.dp(this, 34.0f);
            imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
            imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(0, 704643071, com.echoflow.chat.EfUi.dp(this, 11.0f)));
            imageView.setClipToOutline(true);
            linearLayout2.addView(imageView);
        }
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        linearLayout3.setLayoutParams(layoutParams2);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, this.card.name + " 的解读", 14.0f, -1521542, true));
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "关系「" + this.rel.levelName() + "」· 情绪 " + (this.emo.mood == null ? "平静" : this.emo.mood), 11.0f, -9475445, false));
        linearLayout2.addView(linearLayout3);
        linearLayout2.addView(com.echoflow.chat.EfUi.chip(this, "羁绊 +1", 2));
        linearLayout.addView(linearLayout2);
        if (this.fortune.reading == null || this.fortune.reading.trim().isEmpty()) {
            str = "（她低头看签，还在想怎么跟你说……）";
        } else {
            str = this.fortune.reading;
        }
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, str, 14.0f, -1, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 7.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 11.0f);
        text.setLayoutParams(layoutParams3);
        linearLayout.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "今日缘签 · 你与她 " + this.fortune.bondScore + " · " + this.fortune.bondRank, 12.0f, -1521542, false);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 11.0f);
        text2.setLayoutParams(layoutParams4);
        linearLayout.addView(text2);
        return linearLayout;
    }

    private android.view.View appliedBlock() {
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 22.0f);
        card.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "已写入记忆", 4));
        linearLayout.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "羁绊 +1", 1));
        linearLayout.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "连续参拜 " + com.echoflow.chat.ShrineStore.load(this, this.card.id).streak + " 天", 2));
        card.addView(linearLayout);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "签文已存为长期记忆：「" + this.fortune.date + " 在银月神社求了一支签：「" + this.fortune.rank + "」，签文是「" + this.fortune.summary + "」。」她以后会记得这一天。", 12.0f, -5527866, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text.setLayoutParams(layoutParams2);
        card.addView(text);
        return card;
    }

    private android.view.View actions() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 22.0f);
        linearLayout.setLayoutParams(layoutParams);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "挂上绘马", 14.0f, -1, true);
        text.setGravity(17);
        text.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text.setPadding(0, com.echoflow.chat.EfUi.dp(this, 13.0f), 0, com.echoflow.chat.EfUi.dp(this, 13.0f));
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.rightMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        text.setLayoutParams(layoutParams2);
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.FortuneActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.FortuneActivity.this.lambda$actions$1(view);
            }
        });
        linearLayout.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "回到神社", 14.0f, -724225, true);
        text2.setGravity(17);
        text2.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_chip);
        text2.setPadding(0, com.echoflow.chat.EfUi.dp(this, 13.0f), 0, com.echoflow.chat.EfUi.dp(this, 13.0f));
        text2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.FortuneActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.FortuneActivity.this.lambda$actions$2(view);
            }
        });
        linearLayout.addView(text2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$actions$1(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.EmaActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$actions$2(android.view.View view) {
        finish();
    }

    private int rankColor() {
        java.lang.String str = this.fortune.rank;
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

    private static int parseColor(java.lang.String str) {
        try {
            return android.graphics.Color.parseColor(str);
        } catch (java.lang.Exception unused) {
            return -6587393;
        }
    }
}
