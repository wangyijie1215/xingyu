package com.echoflow.chat;

/* loaded from: classes.dex */
public class EmaActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private android.widget.LinearLayout listBox;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        com.echoflow.chat.CharacterCard card = com.echoflow.chat.CardStore.getCard(this, getIntent().getStringExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID));
        this.card = card;
        if (card == null) {
            android.widget.Toast.makeText(this, "角色不存在", 0).show();
            finish();
        } else {
            buildUi();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        if (this.card == null || this.listBox == null) {
            return;
        }
        renderList();
    }

    private void buildUi() {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, com.echoflow.chat.R.color.ef_night_800));
        scrollView.setVerticalScrollBarEnabled(false);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        scrollView.addView(linearLayout);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        int dp = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout2.setPadding(dp, dp, dp, com.echoflow.chat.EfUi.dp(this, 10.0f));
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "‹", 24.0f, -724225, false);
        text.setGravity(17);
        int dp2 = com.echoflow.chat.EfUi.dp(this, 36.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp2, dp2));
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.EmaActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.EmaActivity.this.lambda$buildUi$0(view);
            }
        });
        linearLayout2.addView(text);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout3.setLayoutParams(layoutParams);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "银月神社", 11.0f, -9475445, false));
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, "绘马架", 24.0f, -724225, true));
        linearLayout2.addView(linearLayout3);
        linearLayout.addView(linearLayout2);
        android.widget.LinearLayout linearLayout4 = new android.widget.LinearLayout(this);
        linearLayout4.setOrientation(1);
        int dp3 = com.echoflow.chat.EfUi.dp(this, 16.0f);
        linearLayout4.setPadding(dp3, 0, dp3, com.echoflow.chat.EfUi.dp(this, 32.0f));
        linearLayout.addView(linearLayout4);
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, "写在绘马上的愿望会进入她的长期记忆。她会在之后的日子里，偶尔提起它。", 13.0f, -1521542, false);
        text2.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        card.addView(text2);
        linearLayout4.addView(card);
        linearLayout4.addView(com.echoflow.chat.EfUi.sectionHead(this, "写一块绘马", "", null));
        android.widget.LinearLayout card2 = com.echoflow.chat.EfUi.card(this, false);
        final android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("希望……");
        editText.setTextColor(-12963544);
        editText.setHintTextColor(-1724239576);
        editText.setTextSize(14.0f);
        editText.setBackground(com.echoflow.chat.EfUi.roundRectPx(-1584216, 859451688, com.echoflow.chat.EfUi.dp(this, 10.0f)));
        editText.setPadding(com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f));
        editText.setMinLines(2);
        editText.setGravity(48);
        editText.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        card2.addView(editText);
        android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, "挂上去", 13.0f, -1, true);
        text3.setGravity(17);
        text3.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text3.setPadding(0, com.echoflow.chat.EfUi.dp(this, 11.0f), 0, com.echoflow.chat.EfUi.dp(this, 11.0f));
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text3.setLayoutParams(layoutParams2);
        text3.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.EmaActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.EmaActivity.this.lambda$buildUi$1(editText, view);
            }
        });
        card2.addView(text3);
        linearLayout4.addView(card2);
        linearLayout4.addView(com.echoflow.chat.EfUi.sectionHead(this, "绘马墙", "她也会写", new android.view.View.OnClickListener() { // from class: com.echoflow.chat.EmaActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.EmaActivity.this.lambda$buildUi$2(view);
            }
        }));
        android.widget.ImageView imageView = new android.widget.ImageView(this);
        imageView.setImageResource(com.echoflow.chat.R.drawable.ef_ema_rack);
        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 150.0f));
        layoutParams3.bottomMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        imageView.setLayoutParams(layoutParams3);
        imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(0, 352321535, com.echoflow.chat.EfUi.dp(this, 16.0f)));
        imageView.setClipToOutline(true);
        linearLayout4.addView(imageView);
        android.widget.LinearLayout linearLayout5 = new android.widget.LinearLayout(this);
        this.listBox = linearLayout5;
        linearLayout5.setOrientation(1);
        linearLayout4.addView(this.listBox);
        renderList();
        setContentView(scrollView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$1(android.widget.EditText editText, android.view.View view) {
        java.lang.String trim = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (trim.isEmpty()) {
            android.widget.Toast.makeText(this, "先写点愿望吧", 0).show();
            return;
        }
        com.echoflow.chat.EmaStore.writeUserEma(this, this.card, trim);
        editText.setText("");
        android.widget.Toast.makeText(this, "绘马已挂上 · 她记下了", 0).show();
        renderList();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$2(android.view.View view) {
        requestHerEma();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void renderList() {
        this.listBox.removeAllViews();
        java.util.List<com.echoflow.chat.EmaStore.Ema> list = com.echoflow.chat.EmaStore.list(this, this.card.id);
        if (list.isEmpty()) {
            android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "绘马架还是空的。写一块吧。", 13.0f, -9475445, false);
            text.setGravity(17);
            text.setPadding(0, com.echoflow.chat.EfUi.dp(this, 24.0f), 0, com.echoflow.chat.EfUi.dp(this, 24.0f));
            this.listBox.addView(text);
            return;
        }
        java.util.Iterator<com.echoflow.chat.EmaStore.Ema> it = list.iterator();
        while (it.hasNext()) {
            this.listBox.addView(emaView(it.next()));
        }
    }

    private android.view.View emaView(final com.echoflow.chat.EmaStore.Ema ema) {
        java.lang.String str;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_ema);
        int dp = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout.setPadding(dp, com.echoflow.chat.EfUi.dp(this, 14.0f), dp, com.echoflow.chat.EfUi.dp(this, 14.0f));
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        linearLayout.setLayoutParams(layoutParams);
        if ("user".equals(ema.author)) {
            android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{-1584216, -2703732, -3889034});
            gradientDrawable.setCornerRadii(new float[]{com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 10.0f), com.echoflow.chat.EfUi.dp(this, 14.0f), com.echoflow.chat.EfUi.dp(this, 14.0f), com.echoflow.chat.EfUi.dp(this, 14.0f), com.echoflow.chat.EfUi.dp(this, 14.0f)});
            gradientDrawable.setStroke(com.echoflow.chat.EfUi.dp(this, 2.0f), -1747893);
            linearLayout.setBackground(gradientDrawable);
        }
        linearLayout.addView(com.echoflow.chat.EfUi.text(this, "user".equals(ema.author) ? "你的绘马" : this.card.name + "的绘马", 10.0f, -7706066, true));
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, ema.text, 13.0f, -12240344, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 7.0f);
        text.setLayoutParams(layoutParams2);
        linearLayout.addView(text);
        if ("user".equals(ema.author)) {
            str = ema.mentioned ? "已被她看见 · 她提起了" : "已被她看见 · 她记下了";
        } else {
            str = "她写的";
        }
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, str, 10.0f, -8755904, false);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 7.0f);
        text2.setLayoutParams(layoutParams3);
        linearLayout.addView(text2);
        linearLayout.setOnLongClickListener(new android.view.View.OnLongClickListener() { // from class: com.echoflow.chat.EmaActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnLongClickListener
            public final boolean onLongClick(android.view.View view) {
                boolean lambda$emaView$3;
                lambda$emaView$3 = com.echoflow.chat.EmaActivity.this.lambda$emaView$3(ema, view);
                return lambda$emaView$3;
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$emaView$3(com.echoflow.chat.EmaStore.Ema ema, android.view.View view) {
        com.echoflow.chat.EmaStore.delete(this, this.card.id, ema.id);
        renderList();
        return true;
    }

    private void requestHerEma() {
        java.lang.String str = com.echoflow.chat.SecureStore.get(this);
        if (str == null || str.isEmpty()) {
            android.widget.Toast.makeText(this, "请先到设置页填写 API 密钥", 0).show();
            return;
        }
        android.widget.Toast.makeText(this, "她正在写……", 0).show();
        java.lang.String baseUrl = (this.card.baseUrl == null || this.card.baseUrl.isEmpty()) ? com.echoflow.chat.ChatStore.getBaseUrl(this) : this.card.baseUrl;
        java.lang.String model = (this.card.model == null || this.card.model.isEmpty()) ? com.echoflow.chat.ChatStore.getModel(this) : this.card.model;
        com.echoflow.chat.RelationshipState loadRelationship = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        java.lang.StringBuilder sb = new java.lang.StringBuilder("你现在是 ");
        sb.append(this.card.name).append("。");
        if (this.card.personality != null && !this.card.personality.isEmpty()) {
            sb.append("性格：").append(this.card.personality);
        }
        sb.append("\n【你们的关系】").append(loadRelationship.levelName()).append("，亲密度 ").append(loadRelationship.points).append("/100");
        if (loadEmotion.mood != null) {
            sb.append("\n【你当前的情绪】").append(loadEmotion.mood);
        }
        java.util.List<com.echoflow.chat.StoryEvent> list = com.echoflow.chat.EventStore.list(this, this.card.id);
        if (!list.isEmpty()) {
            sb.append("\n【最近发生的事】").append(list.get(0).title);
        }
        sb.append("\n\n【任务】你在神社的绘马架上写了一块绘马，替对方许了个愿。\n要求：一句话，不超过 18 个字，符合你的性格（话少、不太会直说）。\n直接输出绘马上的文字，不要引号，不要解释。");
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add(new com.echoflow.chat.Message("system", sb.toString()));
        arrayList.add(new com.echoflow.chat.Message("user", "请输出绘马上的文字。"));
        com.echoflow.chat.ApiClient.streamChat(baseUrl, str, model, arrayList, new com.echoflow.chat.EmaActivity.AnonymousClass1());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.echoflow.chat.EmaActivity$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements com.echoflow.chat.ApiClient.Callback {
        private java.lang.String buf = "";

        AnonymousClass1() {
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onChunk(java.lang.String str) {
            this.buf = str;
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onDone(boolean z, java.lang.String str) {
            java.lang.String str2 = this.buf;
            if (z || str2 == null || str2.trim().isEmpty()) {
                str2 = "希望旅人明天不要淋雨。";
            }
            java.lang.String replaceAll = str2.trim().replaceAll("^[\"「『]|[\"」』]$", "");
            com.echoflow.chat.EmaActivity emaActivity = com.echoflow.chat.EmaActivity.this;
            com.echoflow.chat.EmaStore.writeHerEma(emaActivity, emaActivity.card, replaceAll);
            com.echoflow.chat.EmaActivity.this.runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.EmaActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    com.echoflow.chat.EmaActivity.AnonymousClass1.this.lambda$onDone$0();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDone$0() {
            com.echoflow.chat.EmaActivity.this.renderList();
            android.widget.Toast.makeText(com.echoflow.chat.EmaActivity.this, "她写完挂上去了", 0).show();
        }
    }
}
