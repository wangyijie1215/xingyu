package com.echoflow.chat;

/* loaded from: classes.dex */
public class CapsuleActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private com.echoflow.chat.EmotionState emo;
    private java.lang.String openId;
    private com.echoflow.chat.RelationshipState rel;
    private android.widget.LinearLayout root;
    private java.util.List<com.echoflow.chat.TimeCapsule> all = new java.util.ArrayList();
    private boolean creating = false;
    private int delivered = 0;

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
        load();
        int deliverDue = com.echoflow.chat.CapsuleService.deliverDue(this, this.card);
        this.delivered = deliverDue;
        if (deliverDue > 0) {
            load();
        }
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        if (this.card != null) {
            load();
        }
    }

    private void load() {
        this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        this.all = com.echoflow.chat.CapsuleStore.list(this, this.card.id);
    }

    private java.lang.String baseUrl() {
        return (this.card.baseUrl == null || this.card.baseUrl.isEmpty()) ? com.echoflow.chat.ChatStore.getBaseUrl(this) : this.card.baseUrl;
    }

    private java.lang.String model() {
        return (this.card.model == null || this.card.model.isEmpty()) ? com.echoflow.chat.ChatStore.getModel(this) : this.card.model;
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
        this.root = linearLayout2;
        linearLayout2.setOrientation(1);
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        this.root.setPadding(dp, 0, dp, com.echoflow.chat.EfUi.dp(this, 32.0f));
        linearLayout.addView(this.root);
        int i = this.delivered;
        if (i > 0) {
            this.root.addView(deliveredBanner(i));
        }
        this.root.addView(heroJar());
        this.root.addView(herSection());
        this.root.addView(mySection());
        this.root.addView(createSection());
        this.root.addView(sealNotice());
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
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CapsuleActivity.this.lambda$topBar$0(view);
            }
        });
        linearLayout.addView(text);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout2.setLayoutParams(layoutParams);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "写给未来的话", 11.0f, -9475445, false));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "时间胶囊", 24.0f, -724225, true));
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$0(android.view.View view) {
        finish();
    }

    private android.view.View deliveredBanner(int i) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setColor(535349370);
        gradientDrawable.setStroke(1, 1810417786);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 16.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 13.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, "✦", 16.0f, -1521542, false), new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 28.0f), -2));
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, i + " 个时间胶囊到期了。已放进信笺，也写进了她的记忆。", 12.0f, -1521542, false), new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        return linearLayout;
    }

    private android.view.View heroJar() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(1);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{639639098, 437719847, 336464402});
        gradientDrawable.setStroke(1, 1307101306);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 20.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 18.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(1);
        android.view.View view = new android.view.View(this);
        view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 30.0f), com.echoflow.chat.EfUi.dp(this, 10.0f)));
        android.graphics.drawable.GradientDrawable gradientDrawable2 = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-3562930, -7705558});
        gradientDrawable2.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 4.0f));
        view.setBackground(gradientDrawable2);
        linearLayout2.addView(view);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setGravity(17);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 62.0f), com.echoflow.chat.EfUi.dp(this, 74.0f));
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 3.0f);
        linearLayout3.setLayoutParams(layoutParams);
        android.graphics.drawable.GradientDrawable gradientDrawable3 = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{1206438010, 350799994});
        gradientDrawable3.setStroke(com.echoflow.chat.EfUi.dp(this, 1.0f), -2132227974);
        gradientDrawable3.setCornerRadii(new float[]{com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 16.0f)});
        linearLayout3.setBackground(gradientDrawable3);
        android.view.View view2 = new android.view.View(this);
        int dp2 = com.echoflow.chat.EfUi.dp(this, 12.0f);
        view2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
        android.graphics.drawable.GradientDrawable gradientDrawable4 = new android.graphics.drawable.GradientDrawable();
        gradientDrawable4.setShape(1);
        gradientDrawable4.setColor(-3900);
        view2.setBackground(gradientDrawable4);
        linearLayout3.addView(view2);
        linearLayout2.addView(linearLayout3);
        android.animation.ObjectAnimator ofFloat = android.animation.ObjectAnimator.ofFloat(view2, "alpha", 0.45f, 1.0f);
        ofFloat.setDuration(1400L);
        ofFloat.setRepeatCount(-1);
        ofFloat.setRepeatMode(2);
        ofFloat.start();
        android.animation.ObjectAnimator ofFloat2 = android.animation.ObjectAnimator.ofFloat(view2, "scaleX", 0.85f, 1.25f);
        ofFloat2.setDuration(1400L);
        ofFloat2.setRepeatCount(-1);
        ofFloat2.setRepeatMode(2);
        ofFloat2.start();
        android.animation.ObjectAnimator ofFloat3 = android.animation.ObjectAnimator.ofFloat(view2, "scaleY", 0.85f, 1.25f);
        ofFloat3.setDuration(1400L);
        ofFloat3.setRepeatCount(-1);
        ofFloat3.setRepeatMode(2);
        ofFloat3.start();
        linearLayout.addView(linearLayout2);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "把话，留到那一天再说", 15.0f, -1521542, true);
        text.setGravity(17);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        text.setLayoutParams(layoutParams2);
        linearLayout.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "写下一段话并指定开启日期。\n到那一天，她会把它作为一条消息发给你。", 11.0f, -5527866, false);
        text2.setGravity(17);
        text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 5.0f);
        text2.setLayoutParams(layoutParams3);
        linearLayout.addView(text2);
        return linearLayout;
    }

    private android.view.View herSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "她留给你的", "她也会写", null));
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (com.echoflow.chat.TimeCapsule timeCapsule : this.all) {
            if (timeCapsule.fromHer()) {
                arrayList.add(timeCapsule);
            }
        }
        if (arrayList.isEmpty()) {
            android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
            card.addView(com.echoflow.chat.EfUi.text(this, "她还没有写。试试让她也留一个。", 12.0f, -9475445, false));
            linearLayout.addView(card);
        } else {
            java.util.Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                linearLayout.addView(capsuleRow((com.echoflow.chat.TimeCapsule) it.next(), true));
            }
        }
        return linearLayout;
    }

    private android.view.View mySection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (com.echoflow.chat.TimeCapsule timeCapsule : this.all) {
            if (!timeCapsule.fromHer()) {
                arrayList.add(timeCapsule);
            }
        }
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "你留给她的", "共 " + arrayList.size() + " 个", null));
        if (arrayList.isEmpty()) {
            android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
            card.addView(com.echoflow.chat.EfUi.text(this, "还没有。写一个吧 —— 有些话，现在说太早了。", 12.0f, -9475445, false));
            linearLayout.addView(card);
        } else {
            java.util.Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                linearLayout.addView(capsuleRow((com.echoflow.chat.TimeCapsule) it.next(), false));
            }
        }
        return linearLayout;
    }

    private android.view.View capsuleRow(final com.echoflow.chat.TimeCapsule timeCapsule, boolean z) {
        java.lang.String str;
        java.lang.String str2;
        final boolean isSealed = timeCapsule.isSealed();
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{401131642, 261848063});
        gradientDrawable.setStroke(1, isSealed ? 954779770 : 1810417786);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 16.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 13.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        linearLayout.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        linearLayout2.setGravity(17);
        linearLayout2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 58.0f), -2));
        int daysLeft = timeCapsule.daysLeft();
        if (daysLeft > 0) {
            str = java.lang.String.valueOf(daysLeft);
        } else {
            str = daysLeft == 0 ? "0" : "·";
        }
        java.lang.String str3 = "已到期";
        if (daysLeft > 0) {
            str2 = "天";
        } else {
            str2 = daysLeft == 0 ? "今天" : "已到期";
        }
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, str, 22.0f, -1747893, true);
        text.setGravity(17);
        linearLayout2.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, str2, 10.0f, -9475445, true);
        text2.setGravity(17);
        linearLayout2.addView(text2);
        linearLayout.addView(linearLayout2);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 11.0f);
        linearLayout3.setLayoutParams(layoutParams2);
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setGravity(16);
        linearLayout4.addView(com.echoflow.chat.EfUi.text(this, timeCapsule.title, 13.0f, -724225, true));
        if (timeCapsule.opened) {
            str3 = "已开启";
        } else if (isSealed) {
            str3 = "未到开启日";
        }
        android.widget.TextView chip = com.echoflow.chat.EfUi.chip(this, str3, timeCapsule.opened ? 4 : 2);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams3.leftMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        chip.setLayoutParams(layoutParams3);
        linearLayout4.addView(chip);
        linearLayout3.addView(linearLayout4);
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, subtitleOf(timeCapsule, isSealed), 11.0f, -5527866, false);
        text3.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 4.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 3.0f);
        text3.setLayoutParams(layoutParams4);
        linearLayout3.addView(text3);
        linearLayout.addView(linearLayout3);
        linearLayout.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CapsuleActivity.this.lambda$capsuleRow$1(isSealed, timeCapsule, view);
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$capsuleRow$1(boolean z, com.echoflow.chat.TimeCapsule timeCapsule, android.view.View view) {
        if (z) {
            android.widget.Toast.makeText(this, "还不能打开。到 " + timeCapsule.openAt + " 那天，它会自己来找你。", 0).show();
        } else {
            this.openId = timeCapsule.id;
            showDetail(timeCapsule);
        }
    }

    private java.lang.String subtitleOf(com.echoflow.chat.TimeCapsule timeCapsule, boolean z) {
        if (timeCapsule.opened) {
            return "已于 " + timeCapsule.openAt + " 开启 · " + preview(timeCapsule.content, 34);
        }
        if (z) {
            return "开启日 " + timeCapsule.openAt + " · 内容封存中";
        }
        return "已到期 · 点开查看";
    }

    private static java.lang.String preview(java.lang.String str, int i) {
        if (str == null) {
            return "";
        }
        java.lang.String trim = str.replace("\n", " ").trim();
        return trim.length() > i ? trim.substring(0, i) + "…" : trim;
    }

    private void showDetail(com.echoflow.chat.TimeCapsule timeCapsule) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackground(com.echoflow.chat.EfUi.roundRectPx(352321535, 352321535, com.echoflow.chat.EfUi.dp(this, 18.0f)));
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, timeCapsule.title, 15.0f, -1521542, true));
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, timeCapsule.content, 14.0f, -724225, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 8.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text.setLayoutParams(layoutParams);
        linearLayout.addView(text);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(timeCapsule.fromHer() ? this.card.name + " 的胶囊" : "你的胶囊").setView(linearLayout).setPositiveButton("收好", (android.content.DialogInterface.OnClickListener) null).show();
    }

    private android.view.View createSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "新建一个胶囊", "", null));
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        final android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("给它起个名字（例如：她生日那天要说的话）");
        editText.setTextColor(-724225);
        editText.setHintTextColor(-1720751477);
        editText.setTextSize(14.0f);
        editText.setBackground(com.echoflow.chat.EfUi.roundRectPx(-14408648, 352321535, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        editText.setPadding(com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f));
        card.addView(editText);
        final android.widget.EditText editText2 = new android.widget.EditText(this);
        editText2.setHint("想留到那天再说的话……");
        editText2.setTextColor(-724225);
        editText2.setHintTextColor(-1720751477);
        editText2.setTextSize(14.0f);
        editText2.setBackground(com.echoflow.chat.EfUi.roundRectPx(-14408648, 352321535, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        editText2.setPadding(com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f));
        editText2.setMinLines(3);
        editText2.setGravity(48);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        editText2.setLayoutParams(layoutParams);
        card.addView(editText2);
        final java.lang.String[] strArr = {defaultOpenDate()};
        final android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "开启日\u3000" + strArr[0], 13.0f, -1521542, true);
        text.setGravity(17);
        text.setBackground(com.echoflow.chat.EfUi.roundRectPx(350799994, 1038665850, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        text.setPadding(0, com.echoflow.chat.EfUi.dp(this, 11.0f), 0, com.echoflow.chat.EfUi.dp(this, 11.0f));
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        text.setLayoutParams(layoutParams2);
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CapsuleActivity.this.lambda$createSection$3(strArr, text, view);
            }
        });
        card.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, this.creating ? "封存中……" : "封存", 14.0f, -1, true);
        text2.setGravity(17);
        text2.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text2.setPadding(0, com.echoflow.chat.EfUi.dp(this, 13.0f), 0, com.echoflow.chat.EfUi.dp(this, 13.0f));
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text2.setLayoutParams(layoutParams3);
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CapsuleActivity.this.lambda$createSection$4(editText, editText2, strArr, view);
            }
        });
        card.addView(text2);
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, "让她也写一个", 13.0f, -1521542, true);
        text3.setGravity(17);
        text3.setBackground(com.echoflow.chat.EfUi.roundRectPx(350799994, 1038665850, com.echoflow.chat.EfUi.dp(this, 999.0f)));
        text3.setPadding(0, com.echoflow.chat.EfUi.dp(this, 11.0f), 0, com.echoflow.chat.EfUi.dp(this, 11.0f));
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        text3.setLayoutParams(layoutParams4);
        text3.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CapsuleActivity.this.lambda$createSection$7(view);
            }
        });
        card.addView(text3);
        linearLayout.addView(card);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createSection$3(final java.lang.String[] strArr, final android.widget.TextView textView, android.view.View view) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(this, new android.app.DatePickerDialog.OnDateSetListener() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda3
            @Override // android.app.DatePickerDialog.OnDateSetListener
            public final void onDateSet(android.widget.DatePicker datePicker, int i, int i2, int i3) {
                com.echoflow.chat.CapsuleActivity.lambda$createSection$2(strArr, textView, datePicker, i, i2, i3);
            }
        }, calendar.get(1), calendar.get(2), calendar.get(5)).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$createSection$2(java.lang.String[] strArr, android.widget.TextView textView, android.widget.DatePicker datePicker, int i, int i2, int i3) {
        strArr[0] = java.lang.String.format(java.util.Locale.CHINA, "%04d-%02d-%02d", java.lang.Integer.valueOf(i), java.lang.Integer.valueOf(i2 + 1), java.lang.Integer.valueOf(i3));
        textView.setText("开启日\u3000" + strArr[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createSection$4(android.widget.EditText editText, android.widget.EditText editText2, java.lang.String[] strArr, android.view.View view) {
        java.lang.String trim = editText.getText() == null ? "" : editText.getText().toString().trim();
        java.lang.String trim2 = editText2.getText() == null ? "" : editText2.getText().toString().trim();
        if (trim2.isEmpty()) {
            android.widget.Toast.makeText(this, "先写点想留到那天的话吧", 0).show();
            return;
        }
        if (strArr[0].compareTo(com.echoflow.chat.Fortune.today()) <= 0) {
            android.widget.Toast.makeText(this, "开启日要在今天之后", 0).show();
            return;
        }
        com.echoflow.chat.CharacterCard characterCard = this.card;
        if (trim.isEmpty()) {
            trim = "写给 " + strArr[0] + " 的话";
        }
        com.echoflow.chat.CapsuleService.createForHer(this, characterCard, trim, trim2, strArr[0]);
        editText.setText("");
        editText2.setText("");
        android.widget.Toast.makeText(this, "已封存 · 到期那天她会替你说出来", 0).show();
        load();
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createSection$7(android.view.View view) {
        if (this.creating) {
            return;
        }
        java.lang.String str = com.echoflow.chat.SecureStore.get(this);
        if (str == null || str.isEmpty()) {
            android.widget.Toast.makeText(this, "请先到设置页填写 API 密钥", 0).show();
            return;
        }
        this.creating = true;
        android.widget.Toast.makeText(this, "她正在写……", 0).show();
        com.echoflow.chat.CapsuleService.herCapsuleAsync(this, this.card, baseUrl(), str, model(), this.rel, this.emo, com.echoflow.chat.EventStore.list(this, this.card.id), com.echoflow.chat.FortuneService.shiftDate(com.echoflow.chat.Fortune.today(), 99), new java.lang.Runnable() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.CapsuleActivity.this.lambda$createSection$6();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createSection$6() {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.CapsuleActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.CapsuleActivity.this.lambda$createSection$5();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createSection$5() {
        this.creating = false;
        load();
        buildUi();
        android.widget.Toast.makeText(this, "她把胶囊封好了", 0).show();
    }

    private static java.lang.String defaultOpenDate() {
        return com.echoflow.chat.FortuneService.shiftDate(com.echoflow.chat.Fortune.today(), 30);
    }

    private android.view.View sealNotice() {
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, true);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 22.0f);
        card.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "🔒", 15.0f, -1521542, false);
        text.setGravity(17);
        int dp = com.echoflow.chat.EfUi.dp(this, 34.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        text.setBackground(com.echoflow.chat.EfUi.roundRectPx(703121530, 0, com.echoflow.chat.EfUi.dp(this, 11.0f)));
        linearLayout.addView(text);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout2.setLayoutParams(layoutParams2);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "封存期间不可提前打开", 14.0f, -724225, true));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "内容只保存在本地，连她也不会在到期前引用它", 11.0f, -9475445, false));
        linearLayout.addView(linearLayout2);
        card.addView(linearLayout);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "到期当天，胶囊会以「特别消息」的形式送达：一封信 + 一次记忆写入 + 一条事件。", 12.0f, -5527866, false);
        text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        text2.setLayoutParams(layoutParams3);
        card.addView(text2);
        return card;
    }
}
