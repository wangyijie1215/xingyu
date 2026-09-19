package com.echoflow.chat;

/* loaded from: classes.dex */
public class LettersActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private com.echoflow.chat.EmotionState emo;
    private java.lang.String openLetterId;
    private com.echoflow.chat.Persona persona;
    private com.echoflow.chat.RelationshipState rel;
    private java.util.List<com.echoflow.chat.Letter> all = new java.util.ArrayList();
    private boolean writing = false;
    private boolean composing = false;

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
            java.util.List<com.echoflow.chat.Persona> listPersonas = com.echoflow.chat.CardStore.listPersonas(this);
            this.persona = (listPersonas == null || listPersonas.isEmpty()) ? new com.echoflow.chat.Persona() : listPersonas.get(0);
            load();
            buildUi();
        }
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
        java.util.List<com.echoflow.chat.Letter> list = com.echoflow.chat.LetterStore.list(this, this.card.id);
        this.all = list;
        if (this.openLetterId == null) {
            for (com.echoflow.chat.Letter letter : list) {
                if (letter.fromHer() && !letter.read) {
                    this.openLetterId = letter.id;
                    return;
                }
            }
        }
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
        linearLayout2.setOrientation(1);
        int dp = com.echoflow.chat.EfUi.dp(this, 16.0f);
        linearLayout2.setPadding(dp, 0, dp, com.echoflow.chat.EfUi.dp(this, 32.0f));
        linearLayout.addView(linearLayout2);
        linearLayout2.addView(com.echoflow.chat.EfUi.sectionHead(this, "往来", "共 " + this.all.size() + " 封", null));
        if (this.all.isEmpty()) {
            android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
            card.addView(com.echoflow.chat.EfUi.text(this, "还没有信。写一封给她，或者等她先动笔。", 13.0f, -9475445, false));
            linearLayout2.addView(card);
        } else {
            java.util.Iterator<com.echoflow.chat.Letter> it = this.all.iterator();
            while (it.hasNext()) {
                linearLayout2.addView(letterRow(it.next()));
            }
        }
        com.echoflow.chat.Letter letter = this.openLetterId == null ? null : com.echoflow.chat.LetterStore.get(this, this.card.id, this.openLetterId);
        if (letter != null) {
            linearLayout2.addView(com.echoflow.chat.EfUi.sectionHead(this, "拆开", letter.fromHer() ? letter.dateLabel : "你寄出的", null));
            linearLayout2.addView(letterPaper(letter));
            if (letter.fromHer()) {
                linearLayout2.addView(basisCard(letter));
            }
        }
        linearLayout2.addView(writeSection());
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
        text.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.LettersActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.LettersActivity.this.lambda$topBar$0(view);
            }
        });
        linearLayout.addView(text);
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams.leftMargin = com.echoflow.chat.EfUi.dp(this, 4.0f);
        linearLayout2.setLayoutParams(layoutParams);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "对话之外的另一种说话方式", 11.0f, -9475445, false));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "信笺", 24.0f, -724225, true));
        linearLayout.addView(linearLayout2);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$topBar$0(android.view.View view) {
        finish();
    }

    private android.view.View letterRow(final com.echoflow.chat.Letter letter) {
        boolean z = letter.fromHer() && !letter.read;
        boolean equals = letter.id.equals(this.openLetterId);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setColor(z ? 313539118 : 251658239);
        gradientDrawable.setStroke(1, z ? 1638939182 : equals ? 1805351935 : 352321535);
        gradientDrawable.setCornerRadius(com.echoflow.chat.EfUi.dp(this, 16.0f));
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 13.0f);
        linearLayout.setPadding(dp, dp, dp, dp);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = com.echoflow.chat.EfUi.dp(this, 9.0f);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.addView(waxSeal(letter, z));
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        linearLayout2.setLayoutParams(layoutParams2);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(16);
        linearLayout3.addView(com.echoflow.chat.EfUi.text(this, letter.fromHer() ? this.card.name + " 的来信" : "你寄出的信", 13.0f, -724225, true));
        if (z) {
            android.widget.TextView chip = com.echoflow.chat.EfUi.chip(this, "未拆", 3);
            android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
            layoutParams3.leftMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
            chip.setLayoutParams(layoutParams3);
            linearLayout3.addView(chip);
        } else {
            android.widget.TextView chip2 = com.echoflow.chat.EfUi.chip(this, letter.fromHer() ? "已读" : "已送达", 0);
            android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-2, -2);
            layoutParams4.leftMargin = com.echoflow.chat.EfUi.dp(this, 8.0f);
            chip2.setLayoutParams(layoutParams4);
            linearLayout3.addView(chip2);
        }
        linearLayout2.addView(linearLayout3);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, letter.preview(), 12.0f, -5527866, false);
        text.setMaxLines(1);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        android.widget.LinearLayout.LayoutParams layoutParams5 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams5.topMargin = com.echoflow.chat.EfUi.dp(this, 3.0f);
        text.setLayoutParams(layoutParams5);
        linearLayout2.addView(text);
        linearLayout.addView(linearLayout2);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, shortDate(letter.createdAt), 10.0f, -9475445, false);
        text2.setGravity(androidx.core.view.GravityCompat.END);
        linearLayout.addView(text2);
        linearLayout.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.LettersActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.LettersActivity.this.lambda$letterRow$1(letter, view);
            }
        });
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$letterRow$1(com.echoflow.chat.Letter letter, android.view.View view) {
        this.openLetterId = letter.id;
        if (letter.fromHer() && !letter.read) {
            com.echoflow.chat.LetterStore.markRead(this, this.card.id, letter.id);
        }
        load();
        buildUi();
    }

    private android.view.View waxSeal(com.echoflow.chat.Letter letter, boolean z) {
        int[] iArr;
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, letter.fromHer() ? firstChar(this.card.name) : "你", letter.fromHer() ? 15.0f : 12.0f, -419430401, true);
        text.setGravity(17);
        int dp = com.echoflow.chat.EfUi.dp(this, 46.0f);
        text.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
        android.graphics.drawable.GradientDrawable.Orientation orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR;
        if (letter.fromHer()) {
            iArr = new int[]{-3125185, -5227986, -8509920};
        } else {
            iArr = new int[]{-7760, -1521542, -5206724};
        }
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(orientation, iArr);
        gradientDrawable.setShape(1);
        text.setBackground(gradientDrawable);
        if (z) {
            android.animation.ObjectAnimator ofFloat = android.animation.ObjectAnimator.ofFloat(text, "alpha", 1.0f, 0.72f);
            ofFloat.setDuration(1700L);
            ofFloat.setRepeatCount(-1);
            ofFloat.setRepeatMode(2);
            ofFloat.start();
        }
        return text;
    }

    private static java.lang.String firstChar(java.lang.String str) {
        if (str == null || str.isEmpty()) {
            return "信";
        }
        return str.substring(0, 1);
    }

    private static java.lang.String shortDate(long j) {
        if (j <= 0) {
            return "";
        }
        return new java.text.SimpleDateFormat("M/d", java.util.Locale.CHINA).format(new java.util.Date(j));
    }

    private android.view.View letterPaper(com.echoflow.chat.Letter letter) {
        int[] iArr;
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        android.graphics.drawable.GradientDrawable.Orientation orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR;
        if (letter.fromHer()) {
            iArr = new int[]{-658950, -1646862, -2568468};
        } else {
            iArr = new int[]{-521, -790814, -1647418};
        }
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(orientation, iArr);
        gradientDrawable.setCornerRadii(new float[]{com.echoflow.chat.EfUi.dp(this, 4.0f), com.echoflow.chat.EfUi.dp(this, 4.0f), com.echoflow.chat.EfUi.dp(this, 4.0f), com.echoflow.chat.EfUi.dp(this, 4.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f)});
        linearLayout.setBackground(gradientDrawable);
        int dp = com.echoflow.chat.EfUi.dp(this, 18.0f);
        linearLayout.setPadding(dp, com.echoflow.chat.EfUi.dp(this, 20.0f), dp, dp);
        if (!letter.greet.isEmpty()) {
            linearLayout.addView(com.echoflow.chat.EfUi.text(this, letter.greet, 12.0f, -9542008, false));
        }
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, letter.body, 14.0f, -12962992, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 14.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        text.setLayoutParams(layoutParams);
        linearLayout.addView(text);
        if (!letter.sign.isEmpty()) {
            android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, letter.sign, 13.0f, -12962992, true);
            text2.setGravity(androidx.core.view.GravityCompat.END);
            android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 16.0f);
            text2.setLayoutParams(layoutParams2);
            linearLayout.addView(text2);
        }
        if (!letter.dateLabel.isEmpty()) {
            android.widget.TextView text3 = com.echoflow.chat.EfUi.text(this, letter.dateLabel, 10.0f, -9542008, false);
            text3.setGravity(androidx.core.view.GravityCompat.END);
            text3.setLetterSpacing(0.1f);
            android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, -2);
            layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 3.0f);
            text3.setLayoutParams(layoutParams3);
            linearLayout.addView(text3);
        }
        if (!letter.ps.isEmpty()) {
            android.view.View view = new android.view.View(this);
            android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, com.echoflow.chat.EfUi.dp(this, 1.0f));
            layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
            layoutParams4.bottomMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
            view.setLayoutParams(layoutParams4);
            view.setBackgroundColor(859452240);
            linearLayout.addView(view);
            android.widget.TextView text4 = com.echoflow.chat.EfUi.text(this, letter.ps, 12.0f, -9542008, false);
            text4.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
            linearLayout.addView(text4);
        }
        return linearLayout;
    }

    private android.view.View basisCard(com.echoflow.chat.Letter letter) {
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, true);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 14.0f);
        card.setLayoutParams(layoutParams);
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, this.card.id);
        if (loadAvatar != null) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setImageBitmap(loadAvatar);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            int dp = com.echoflow.chat.EfUi.dp(this, 34.0f);
            imageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp, dp));
            imageView.setBackground(com.echoflow.chat.EfUi.roundRectPx(0, 704643071, com.echoflow.chat.EfUi.dp(this, 11.0f)));
            imageView.setClipToOutline(true);
            linearLayout.addView(imageView);
        }
        android.widget.LinearLayout linearLayout2 = new android.widget.LinearLayout(this);
        linearLayout2.setOrientation(1);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
        layoutParams2.leftMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        linearLayout2.setLayoutParams(layoutParams2);
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "她的信是怎么写出来的", 13.0f, -724225, true));
        linearLayout2.addView(com.echoflow.chat.EfUi.text(this, "依据：角色卡 · 关系 · 情绪 · 记忆 · 事件", 11.0f, -9475445, false));
        linearLayout.addView(linearLayout2);
        card.addView(linearLayout);
        android.widget.LinearLayout linearLayout3 = new android.widget.LinearLayout(this);
        linearLayout3.setOrientation(0);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        layoutParams3.topMargin = com.echoflow.chat.EfUi.dp(this, 11.0f);
        linearLayout3.setLayoutParams(layoutParams3);
        linearLayout3.addView(com.echoflow.chat.EfUi.chip(this, "已写入记忆", 4));
        linearLayout3.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout3.addView(com.echoflow.chat.EfUi.chip(this, "羁绊 +2", 1));
        linearLayout3.addView(com.echoflow.chat.EfUi.spacer(this, 6));
        linearLayout3.addView(com.echoflow.chat.EfUi.chip(this, "可存回忆卡", 0));
        card.addView(linearLayout3);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "生成时：关系「" + this.rel.levelName() + "」· 情绪 " + (letter.basisEmotion.isEmpty() ? "平静" : letter.basisEmotion) + "· 亲密度 " + letter.basisBond + (letter.basisMemoryIds.isEmpty() ? "" : " · 引用了 " + letter.basisMemoryIds.size() + " 条记忆"), 11.0f, -5527866, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 4.0f), 1.0f);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = com.echoflow.chat.EfUi.dp(this, 10.0f);
        text.setLayoutParams(layoutParams4);
        card.addView(text);
        return card;
    }

    private android.view.View writeSection() {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.addView(com.echoflow.chat.EfUi.sectionHead(this, "写一封信给她", "", null));
        android.widget.LinearLayout card = com.echoflow.chat.EfUi.card(this, false);
        android.widget.TextView text = com.echoflow.chat.EfUi.text(this, "信与聊天不同：聊天是说，信是写。信可以很长，可以停顿，可以说一半就停。", 12.0f, -5527866, false);
        text.setLineSpacing(com.echoflow.chat.EfUi.dp(this, 5.0f), 1.0f);
        card.addView(text);
        final android.widget.EditText editText = new android.widget.EditText(this);
        editText.setHint("想说的话……");
        editText.setTextColor(-724225);
        editText.setHintTextColor(-1720751477);
        editText.setTextSize(14.0f);
        editText.setBackground(com.echoflow.chat.EfUi.roundRectPx(-14408648, 352321535, com.echoflow.chat.EfUi.dp(this, 12.0f)));
        editText.setPadding(com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f), com.echoflow.chat.EfUi.dp(this, 12.0f));
        editText.setMinLines(4);
        editText.setGravity(48);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        editText.setLayoutParams(layoutParams);
        card.addView(editText);
        android.widget.TextView text2 = com.echoflow.chat.EfUi.text(this, this.composing ? "她正在回信……" : "寄出去，等她回信", 14.0f, -1, true);
        text2.setGravity(17);
        text2.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_btn_shrine);
        text2.setPadding(0, com.echoflow.chat.EfUi.dp(this, 13.0f), 0, com.echoflow.chat.EfUi.dp(this, 13.0f));
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = com.echoflow.chat.EfUi.dp(this, 12.0f);
        text2.setLayoutParams(layoutParams2);
        text2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.LettersActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.LettersActivity.this.lambda$writeSection$2(editText, view);
            }
        });
        card.addView(text2);
        linearLayout.addView(card);
        return linearLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$writeSection$2(android.widget.EditText editText, android.view.View view) {
        java.lang.String trim = editText.getText() == null ? "" : editText.getText().toString().trim();
        if (trim.isEmpty()) {
            android.widget.Toast.makeText(this, "先写点想说的话吧", 0).show();
        } else {
            sendLetter(trim, editText);
        }
    }

    private void sendLetter(java.lang.String str, android.widget.EditText editText) {
        if (this.composing) {
            return;
        }
        com.echoflow.chat.Letter sendFromUser = com.echoflow.chat.LetterService.sendFromUser(this, this.card, str);
        editText.setText("");
        android.widget.Toast.makeText(this, "信已寄出", 0).show();
        load();
        this.openLetterId = sendFromUser.id;
        buildUi();
        java.lang.String str2 = com.echoflow.chat.SecureStore.get(this);
        if (str2 == null || str2.isEmpty()) {
            android.widget.Toast.makeText(this, "配好 API 密钥后她才能回信", 1).show();
            return;
        }
        this.composing = true;
        android.widget.Toast.makeText(this, "她正在写回信……", 0).show();
        com.echoflow.chat.LetterService.replyFromHer(this, this.card, this.persona, baseUrl(), str2, model(), this.rel, this.emo, com.echoflow.chat.MemoryStore.topForPrompt(this, this.card.id, 5), com.echoflow.chat.EventStore.list(this, this.card.id), str, new java.lang.Runnable() { // from class: com.echoflow.chat.LettersActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.LettersActivity.this.lambda$sendLetter$4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendLetter$4() {
        runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.LettersActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.LettersActivity.this.lambda$sendLetter$3();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendLetter$3() {
        this.composing = false;
        load();
        com.echoflow.chat.Letter letter = this.all.isEmpty() ? null : this.all.get(0);
        if (letter != null) {
            this.openLetterId = letter.id;
        }
        buildUi();
        android.widget.Toast.makeText(this, "她的回信到了", 0).show();
    }
}
