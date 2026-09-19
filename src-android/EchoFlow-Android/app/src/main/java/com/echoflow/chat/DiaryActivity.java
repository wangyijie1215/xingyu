package com.echoflow.chat;

/* loaded from: classes.dex */
public class DiaryActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private com.echoflow.chat.EmotionState emo;
    private com.echoflow.chat.Persona persona;
    private com.echoflow.chat.RelationshipState rel;
    private android.widget.LinearLayout root;
    private com.echoflow.chat.ShrineState shrine;
    private com.echoflow.chat.DiaryEntry today;
    private java.util.List<com.echoflow.chat.DiaryEntry> all = new java.util.ArrayList();
    private boolean generating = false;

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
        loadState();
        this.today = com.echoflow.chat.DiaryService.todayOrGenerate(this, this.card, this.persona, baseUrl(), com.echoflow.chat.SecureStore.get(this), model(), this.rel, this.emo, com.echoflow.chat.MemoryStore.topForPrompt(this, this.card.id, 5), com.echoflow.chat.EventStore.list(this, this.card.id), this.shrine, new java.lang.Runnable() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.DiaryActivity.this.lambda$onCreate$1();
            }
        });
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1() {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.DiaryActivity.this.lambda$onCreate$0();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0() {
        loadState();
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        if (this.card != null) {
            loadState();
            com.echoflow.chat.DiaryEntry diaryEntry = com.echoflow.chat.DiaryStore.today(this, this.card.id);
            if (diaryEntry != null) {
                this.today = diaryEntry;
            }
            buildUi();
        }
    }

    private void loadState() {
        this.rel = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        this.emo = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        this.shrine = com.echoflow.chat.ShrineStore.load(this, this.card.id);
        java.util.List<com.echoflow.chat.DiaryEntry> list = com.echoflow.chat.DiaryStore.list(this, this.card.id);
        this.all = list;
        if (list.isEmpty() || !this.all.get(0).date.equals(com.echoflow.chat.Fortune.today())) {
            return;
        }
        this.today = this.all.get(0);
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
        this.root.addView(filterRow());
        this.root.addView(todayCard());
        this.root.addView(moodSection());
        this.root.addView(historySection());
        this.root.addView(generateRow());
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
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.DiaryActivity.this.lambda$topBar$2(view);
            }
        });
        linearLayout.addView(text);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout2.setLayoutParams(layoutParams);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "自动生成 · 每日一篇", 11.0f, -9475445, false));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "她的日记", 24.0f, -724225, true));
        linearLayout.addView(linearLayout2);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "⬇", 16.0f, -724225, false);
        text2.setGravity(17);
        text2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.DiaryActivity.this.lambda$topBar$3(view);
            }
        });
        linearLayout.addView(text2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$2(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$3(android.view.View view) {
        android.widget.Toast.makeText(this, "已生成 " + this.all.size() + " 篇日记，可导出为文本", 0).show();
    }

    private android.view.View filterRow() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "按日期", 1));
        linearLayout.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "按心情", 0));
        linearLayout.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "只看里程碑", 0));
        return linearLayout;
    }

    private android.view.View todayCard() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{401131642, 201326591});
        gradientDrawable.setStroke(1, 352321535);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 20.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        if (this.today == null) {
            linearLayout.addView(com.echoflow.chat.EfUi.text(this, "今天还没有日记。", 14.0f, -5527866, false));
            return linearLayout;
        }
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
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        linearLayout3.setLayoutParams(layoutParams);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, this.today.dateLabel() + " · 夜", 13.0f, -724225, true));
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "情绪 " + this.today.emotion + "\u3000|\u3000羁绊 " + this.today.bond, 11.0f, -9475445, false));
        linearLayout2.addView(linearLayout3);
        linearLayout2.addView(com.echoflow.chat.EfUi.chip(this, "今天", 2));
        linearLayout.addView(linearLayout2);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, this.today.line, 14.0f, -724225, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 8.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        text.setLayoutParams(layoutParams2);
        linearLayout.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, this.today.body, 14.0f, -1275068417, false);
        text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 9.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text2.setLayoutParams(layoutParams3);
        linearLayout.addView(text2);
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
        linearLayout4.setOrientation(0);
        linearLayout4.setBackground(com.echoflow.chat.EfUi.roundRectPx(350799994, 1038665850, com.echoflow.chat.EfUi.dp(this, 12.0f)));
        int dp3 = com.echoflow.chat.EfUi.dp(this, 11.0f);
        linearLayout4.setPadding(dp3, dp3, dp3, dp3);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        linearLayout4.setLayoutParams(layoutParams4);
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, "她没说出口的：「" + this.today.unsaid + "」", 12.0f, -1521542, false);
        text3.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 6.0f), 1.0f);
        linearLayout4.addView(text3);
        linearLayout.addView(linearLayout4);
        if (this.today.tomorrow != null && !this.today.tomorrow.isEmpty()) {
            android.widget.TextView text4 = com.echoflow.chat.EfUi.text(this, "明天：「" + this.today.tomorrow + "」", 11.0f, -9475445, false);
            android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams5.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
            text4.setLayoutParams(layoutParams5);
            linearLayout.addView(text4);
        }
        android.view.View view = new android.view.View(this);
        android.widget.LinearLayout.LayoutParams layoutParams6 = new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 1.0f));
        layoutParams6.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        layoutParams6.bottomMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        view.setLayoutParams(layoutParams6);
        view.setBackgroundColor(352321535);
        linearLayout.addView(view);
        android.widget.TextView text5 = com.echoflow.chat.EfUi.text(this, "—— " + this.card.name + " · 关系「" + this.rel.levelName() + "」", 11.0f, -9475445, false);
        text5.setGravity(androidx.core.view.GravityCompat.END);
        linearLayout.addView(text5);
        return linearLayout;
    }

    private android.view.View moodSection() {
        float f;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        int i = 1;
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "最近十四天的心情", "由日记的情绪快照汇总", null));
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, true);
        java.util.List<com.echoflow.chat.DiaryStore.MoodPoint> moodCurve = com.echoflow.chat.DiaryStore.moodCurve(this.all, 14);
        if (moodCurve.isEmpty()) {
            card.addView(com.echoflow.chat.EfUi.text(this, "还没有足够的心情记录。多来几天就有了。", 12.0f, -9475445, false));
            linearLayout.addView(card);
            return linearLayout;
        }
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(80);
        int i2 = -1;
        linearLayout2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 64.0f)));
        java.util.Iterator<com.echoflow.chat.DiaryStore.MoodPoint> it = moodCurve.iterator();
        while (true) {
            f = 8.0f;
            if (!it.hasNext()) {
                break;
            }
            com.echoflow.chat.DiaryStore.MoodPoint next = it.next();
            android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
            linearLayout3.setOrientation(i);
            linearLayout3.setGravity(81);
            linearLayout3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, i2, 1.0f));
            android.view.View view = new android.view.View(this);
            view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(com.echoflow.chat.EfUi.dp(this, 14.0f), java.lang.Math.max(8, (int) (com.echoflow.chat.EfUi.dp(this, 56.0f) * (next.value / 100)))));
            android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP, new int[]{com.echoflow.chat.DiaryStore.moodColor(next.mood), blend(com.echoflow.chat.DiaryStore.moodColor(next.mood))});
            gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 4.0f));
            view.setBackground(gradientDrawable);
            linearLayout3.addView(view);
            android.widget.TextView text = com.echoflow.chat.EfUi.text(this, next.date, 8.0f, -9475445, false);
            android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-2, -2);
            layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
            text.setLayoutParams(layoutParams);
            linearLayout3.addView(text);
            linearLayout2.addView(linearLayout3);
            i = 1;
            i2 = -1;
        }
        card.addView(linearLayout2);
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
        linearLayout4.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout4.setLayoutParams(layoutParams2);
        for (java.lang.String str : com.echoflow.chat.DiaryStore.moodsIn(moodCurve)) {
            android.view.View view2 = new android.view.View(this);
            int dp = com.echoflow.chat.EfUi.dp(this, f);
            android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(dp, dp);
            layoutParams3.rightMargin = com.echoflow.chat.EfUi.dp(this, 5.0f);
            view2.setLayoutParams(layoutParams3);
            view2.setBackground(com.echoflow.chat.EfUi.roundRectPx(com.echoflow.chat.DiaryStore.moodColor(str), 0, com.echoflow.chat.EfUi.dp(this, 4.0f)));
            linearLayout4.addView(view2);
            android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, str, 10.0f, -5527866, false);
            text2.setPadding(0, 0, com.echoflow.chat.EfUi.dp(this, 12.0f), 0);
            linearLayout4.addView(text2);
            f = 8.0f;
        }
        card.addView(linearLayout4);
        java.lang.String observation = observation(moodCurve);
        if (!observation.isEmpty()) {
            android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, observation, 12.0f, -5527866, false);
            text3.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
            android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
            text3.setLayoutParams(layoutParams4);
            card.addView(text3);
        }
        linearLayout.addView(card);
        return linearLayout;
    }

    private java.lang.String observation(java.util.List<com.echoflow.chat.DiaryStore.MoodPoint> list) {
        if (list.size() < 4) {
            return "";
        }
        java.util.LinkedHashMap linkedHashMap = new java.util.LinkedHashMap();
        for (com.echoflow.chat.DiaryStore.MoodPoint moodPoint : list) {
            java.lang.Integer num = (java.lang.Integer) linkedHashMap.get(moodPoint.mood);
            java.lang.String str = moodPoint.mood;
            int i = 1;
            if (num != null) {
                i = 1 + num.intValue();
            }
            linkedHashMap.put(str, java.lang.Integer.valueOf(i));
        }
        java.lang.String str2 = null;
        int i2 = 0;
        for (java.lang.Object str3Obj : linkedHashMap.keySet()) {
            java.lang.String str3 = (java.lang.String) str3Obj;
            if (((java.lang.Integer) linkedHashMap.get(str3)).intValue() > i2) {
                i2 = ((java.lang.Integer) linkedHashMap.get(str3)).intValue();
                str2 = str3;
            }
        }
        return str2 == null ? "" : "这 " + list.size() + " 天里，「" + str2 + "」出现了 " + i2 + " 次。";
    }

    private static int blend(int i) {
        return java.lang.Math.min(255, (i & 255) + 48) | (java.lang.Math.min(255, ((i >> 16) & 255) + 48) << 16) | androidx.core.view.ViewCompat.MEASURED_STATE_MASK | (java.lang.Math.min(255, ((i >> 8) & 255) + 48) << 8);
    }

    private android.view.View historySection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "往期", "共 " + this.all.size() + " 篇", null));
        int i = 0;
        if (this.all.size() <= 1) {
            android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
            card.addView(com.echoflow.chat.EfUi.text(this, "明天再来看，就有一篇新的了。", 12.0f, -9475445, false));
            linearLayout.addView(card);
            return linearLayout;
        }
        for (com.echoflow.chat.DiaryEntry diaryEntry : this.all) {
            if (!diaryEntry.date.equals(com.echoflow.chat.Fortune.today())) {
                linearLayout.addView(pastCard(diaryEntry));
                i++;
                if (i >= 6) {
                    break;
                }
            }
        }
        return linearLayout;
    }

    private android.view.View pastCard(com.echoflow.chat.DiaryEntry diaryEntry) {
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        card.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.view.View view = new android.view.View(this);
        int dp = com.echoflow.chat.EfUi.dp(this, 8.0f);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(dp, dp);
        layoutParams2.rightMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        view.setLayoutParams(layoutParams2);
        view.setBackground(com.echoflow.chat.EfUi.roundRectPx(com.echoflow.chat.DiaryStore.moodColor(diaryEntry.emotion), 0, com.echoflow.chat.EfUi.dp(this, 4.0f)));
        linearLayout.addView(view);
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, diaryEntry.dateLabel(), 13.0f, -724225, true), new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        if (diaryEntry.milestone) {
            linearLayout.addView(com.echoflow.chat.EfUi.chip(this, "里程碑", 2));
        }
        card.addView(linearLayout);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, diaryEntry.line, 13.0f, -5527866, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 7.0f);
        text.setLayoutParams(layoutParams3);
        card.addView(text);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "她没说出口的：「" + diaryEntry.unsaid + "」", 11.0f, -1521542, false);
        text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 4.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
        text2.setLayoutParams(layoutParams4);
        card.addView(text2);
        return card;
    }

    private android.view.View generateRow() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(new android.widget.LinearLayout(this).getOrientation());
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 22.0f);
        linearLayout.setLayoutParams(layoutParams);
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, true);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "✎", 16.0f, -6587393, false);
        text.setGravity(17);
        int dp = com.echoflow.chat.EfUi.dp(this, 34.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        text.setBackground(com.echoflow.chat.EfUi.roundRectPx(714832895, 0, com.echoflow.chat.EfUi.dp(this, 11.0f)));
        linearLayout2.addView(text);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout3.setLayoutParams(layoutParams2);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "重新写今天的日记", 14.0f, -724225, true));
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "以她的口吻重写一遍，可以替换现在的版本", 11.0f, -9475445, false));
        linearLayout2.addView(linearLayout3);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, this.generating ? "生成中…" : "生成", 12.0f, -1, true);
        text2.setGravity(17);
        text2.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text2.setPadding(com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 8.0f), com.echoflow.chat.EfUi.dp(this, 16.0f), com.echoflow.chat.EfUi.dp(this, 8.0f));
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.DiaryActivity.this.lambda$generateRow$4(view);
            }
        });
        linearLayout2.addView(text2);
        card.addView(linearLayout2);
        linearLayout.addView(card);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$generateRow$4(android.view.View view) {
        regenerate();
    }

    private void regenerate() {
        if (this.generating) {
            return;
        }
        java.lang.String str = com.echoflow.chat.SecureStore.get(this);
        if (str == null || str.isEmpty()) {
            android.widget.Toast.makeText(this, "请先到设置页填写 API 密钥", 0).show();
            return;
        }
        this.generating = true;
        android.widget.Toast.makeText(this, "她正在写今天的日记……", 0).show();
        com.echoflow.chat.DiaryStore.delete(this, this.card.id, com.echoflow.chat.Fortune.today());
        this.today = com.echoflow.chat.DiaryService.todayOrGenerate(this, this.card, this.persona, baseUrl(), str, model(), this.rel, this.emo, com.echoflow.chat.MemoryStore.topForPrompt(this, this.card.id, 5), com.echoflow.chat.EventStore.list(this, this.card.id), this.shrine, new java.lang.Runnable() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.DiaryActivity.this.lambda$regenerate$6();
            }
        });
        buildUi();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$regenerate$6() {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.DiaryActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.DiaryActivity.this.lambda$regenerate$5();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$regenerate$5() {
        this.generating = false;
        loadState();
        buildUi();
        android.widget.Toast.makeText(this, "写好了", 0).show();
    }
}
