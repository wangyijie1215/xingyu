package com.echoflow.chat;

/* loaded from: classes.dex */
public class CharacterProfileActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.CharacterCard card;
    private java.lang.String cardId;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.echoflow.chat.R.layout.activity_character_profile);
        java.lang.String stringExtra = getIntent().getStringExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID);
        this.cardId = stringExtra;
        com.echoflow.chat.CharacterCard card = com.echoflow.chat.CardStore.getCard(this, stringExtra);
        this.card = card;
        if (card == null) {
            android.widget.Toast.makeText(this, "角色不存在", 0).show();
            finish();
            return;
        }
        android.widget.ImageView imageView = (android.widget.ImageView) findViewById(com.echoflow.chat.R.id.profile_avatar);
        android.widget.TextView textView = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.profile_name);
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.profile_tags);
        android.widget.TextView textView3 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.profile_desc);
        textView.setText(this.card.name);
        textView2.setText(this.card.tags.isEmpty() ? "" : "#" + java.lang.String.join("  #", this.card.tags));
        java.lang.String str = this.card.description;
        if (str != null && str.length() > 120) {
            str = str.substring(0, 120) + "…";
        }
        textView3.setText(str != null ? str : "");
        android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, this.card.id);
        if (loadAvatar != null) {
            imageView.setImageBitmap(loadAvatar);
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        }
        android.widget.ImageView imageView2 = (android.widget.ImageView) findViewById(com.echoflow.chat.R.id.profile_hero);
        if (imageView2 != null) {
            imageView2.setImageBitmap(loadAvatar != null ? loadAvatar : null);
            if (loadAvatar == null) {
                imageView2.setImageResource(com.echoflow.chat.R.drawable.ef_shrine_hero);
            }
            imageView2.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        }
        com.echoflow.chat.RelationshipState loadRelationship = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        java.util.List<com.echoflow.chat.Memory> list = com.echoflow.chat.MemoryStore.list(this, this.card.id);
        java.util.List<com.echoflow.chat.StoryEvent> list2 = com.echoflow.chat.EventStore.list(this, this.card.id);
        android.widget.TextView textView4 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.rel_level);
        android.widget.ProgressBar progressBar = (android.widget.ProgressBar) findViewById(com.echoflow.chat.R.id.rel_progress);
        android.widget.TextView textView5 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.rel_points);
        textView4.setText(loadRelationship.levelName());
        progressBar.setProgress(loadRelationship.points);
        textView5.setText("亲密度 " + loadRelationship.points + " / 100");
        android.widget.TextView textView6 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_chat);
        android.widget.TextView textView7 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_memory);
        android.widget.TextView textView8 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_event);
        android.widget.TextView textView9 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_mood);
        textView6.setText(java.lang.String.valueOf(loadRelationship.chatCount));
        textView7.setText(java.lang.String.valueOf(list.size()));
        textView8.setText(java.lang.String.valueOf(list2.size()));
        textView9.setText(loadEmotion.mood == null ? "平静" : loadEmotion.mood);
        android.widget.LinearLayout linearLayout = (android.widget.LinearLayout) findViewById(com.echoflow.chat.R.id.memory_list);
        linearLayout.removeAllViews();
        if (list.isEmpty()) {
            linearLayout.addView(makeHint("还没有记忆，多和 TA 聊聊天吧"));
        } else {
            int min = java.lang.Math.min(3, list.size());
            for (int i = 0; i < min; i++) {
                linearLayout.addView(makeMemoryItem(list.get(i)));
            }
        }
        android.widget.LinearLayout linearLayout2 = (android.widget.LinearLayout) findViewById(com.echoflow.chat.R.id.event_list);
        linearLayout2.removeAllViews();
        if (list2.isEmpty()) {
            linearLayout2.addView(makeHint("还没有事件记录"));
        } else {
            int min2 = java.lang.Math.min(5, list2.size());
            for (int i2 = 0; i2 < min2; i2++) {
                linearLayout2.addView(makeEventItem(list2.get(i2)));
            }
        }
        refreshShrineEntry();
        refreshStoryEntries();
        ((com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_chat)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CharacterProfileActivity.this.lambda$onCreate$0(view);
            }
        });
        ((com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_edit)).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CharacterProfileActivity.this.lambda$onCreate$1(view);
            }
        });
        com.google.android.material.button.MaterialButton materialButton = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_phone);
        if (materialButton != null) {
            materialButton.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda7
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.CharacterProfileActivity.this.lambda$onCreate$2(view);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.CardChatActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.CardEditActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.PhoneChatActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    private void refreshShrineEntry() {
        android.view.View findViewById = findViewById(com.echoflow.chat.R.id.btn_shrine);
        if (findViewById == null || this.card == null) {
            return;
        }
        findViewById.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CharacterProfileActivity.this.lambda$refreshShrineEntry$3(view);
            }
        });
        com.echoflow.chat.Fortune load = com.echoflow.chat.FortuneService.load(this, this.card.id, com.echoflow.chat.Fortune.today());
        com.echoflow.chat.ShrineState load2 = com.echoflow.chat.ShrineStore.load(this, this.card.id);
        android.widget.TextView textView = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.shrine_title);
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.shrine_sub);
        android.widget.TextView textView3 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.shrine_rank);
        if (load == null) {
            textView.setText("银月神社 · 今日运势");
            textView2.setText("今天还没去参拜");
            textView3.setText("未\n参拜");
            textView3.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_seal_blank);
            textView3.setTextColor(-9475445);
            textView3.setRotation(0.0f);
            return;
        }
        textView.setText("银月神社 · 今日「" + load.rank + "」");
        textView2.setText(java.lang.String.format(java.util.Locale.CHINA, "%s · 连续参拜 %d 天 · 缘签 %s", load.summary, java.lang.Integer.valueOf(load2.streak), load.bondRank));
        textView3.setText("已\n参拜");
        textView3.setBackgroundResource(com.echoflow.chat.R.drawable.ef_bg_seal);
        textView3.setTextColor(-1747893);
        textView3.setRotation(-6.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshShrineEntry$3(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.ShrineActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX WARN: Removed duplicated region for block: B:11:0x0065  */
    /* JADX WARN: Removed duplicated region for block: B:14:0x0075  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void refreshStoryEntries() {
        /*
            r5 = this;
            com.echoflow.chat.CharacterCard r0 = r5.card
            if (r0 != 0) goto L5
            return
        L5:
            int r0 = com.echoflow.chat.R.id.btn_diary
            int r1 = com.echoflow.chat.R.id.diary_title
            com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda2 r2 = new com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda2
            r2.<init>()
            java.lang.String r3 = "她的日记"
            r5.bindEntry(r0, r1, r3, r2)
            com.echoflow.chat.CharacterCard r0 = r5.card
            java.lang.String r0 = r0.id
            int r0 = com.echoflow.chat.LetterStore.unreadCount(r5, r0)
            com.echoflow.chat.CharacterCard r1 = r5.card
            java.lang.String r1 = r1.id
            java.util.List r1 = com.echoflow.chat.LetterStore.list(r5, r1)
            int r1 = r1.size()
            int r2 = com.echoflow.chat.R.id.btn_letters
            int r3 = com.echoflow.chat.R.id.letters_title
            java.lang.String r4 = "信笺 "
            if (r0 <= 0) goto L3d
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>(r4)
            java.lang.StringBuilder r0 = r1.append(r0)
        L38:
            java.lang.String r0 = r0.toString()
            goto L4b
        L3d:
            if (r1 <= 0) goto L49
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>(r4)
            java.lang.StringBuilder r0 = r0.append(r1)
            goto L38
        L49:
            java.lang.String r0 = "信笺"
        L4b:
            com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda3 r1 = new com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda3
            r1.<init>()
            r5.bindEntry(r2, r3, r0, r1)
            com.echoflow.chat.CharacterCard r0 = r5.card
            java.lang.String r0 = r0.id
            java.util.List r0 = com.echoflow.chat.CapsuleStore.list(r5, r0)
            int r0 = r0.size()
            int r1 = com.echoflow.chat.R.id.btn_capsule
            int r2 = com.echoflow.chat.R.id.capsule_title
            if (r0 <= 0) goto L75
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r4 = "胶囊 "
            r3.<init>(r4)
            java.lang.StringBuilder r0 = r3.append(r0)
            java.lang.String r0 = r0.toString()
            goto L77
        L75:
            java.lang.String r0 = "时间胶囊"
        L77:
            com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda4 r3 = new com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda4
            r3.<init>()
            r5.bindEntry(r1, r2, r0, r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.echoflow.chat.CharacterProfileActivity.refreshStoryEntries():void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshStoryEntries$4() {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.DiaryActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshStoryEntries$5() {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.LettersActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refreshStoryEntries$6() {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.CapsuleActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    private void bindEntry(int i, int i2, java.lang.String str, final java.lang.Runnable runnable) {
        android.view.View findViewById = findViewById(i);
        android.widget.TextView textView = (android.widget.TextView) findViewById(i2);
        if (textView != null) {
            textView.setText(str);
        }
        if (findViewById != null) {
            findViewById.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CharacterProfileActivity$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    runnable.run();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        com.echoflow.chat.CharacterCard characterCard = this.card;
        if (characterCard != null) {
            com.echoflow.chat.RelationshipState loadRelationship = com.echoflow.chat.CharacterStateStore.loadRelationship(this, characterCard.id);
            com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.rel_level)).setText(loadRelationship.levelName());
            ((android.widget.ProgressBar) findViewById(com.echoflow.chat.R.id.rel_progress)).setProgress(loadRelationship.points);
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.rel_points)).setText("亲密度 " + loadRelationship.points + " / 100");
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_chat)).setText(java.lang.String.valueOf(loadRelationship.chatCount));
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_mood)).setText(loadEmotion.mood == null ? "平静" : loadEmotion.mood);
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_memory)).setText(java.lang.String.valueOf(com.echoflow.chat.MemoryStore.list(this, this.card.id).size()));
            ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.stat_event)).setText(java.lang.String.valueOf(com.echoflow.chat.EventStore.list(this, this.card.id).size()));
            refreshShrineEntry();
            refreshStoryEntries();
        }
    }

    private android.view.View makeHint(java.lang.String str) {
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(str);
        textView.setTextColor(-7435608);
        textView.setTextSize(13.0f);
        textView.setPadding(16, 12, 16, 12);
        textView.setGravity(17);
        return textView;
    }

    private android.view.View makeMemoryItem(com.echoflow.chat.Memory memory) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.bg_card_dark);
        linearLayout.setPadding(14, 10, 14, 10);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = 6;
        linearLayout.setLayoutParams(layoutParams);
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText(memory.content);
        textView.setTextColor(-1185802);
        textView.setTextSize(14.0f);
        textView.setLineSpacing(0.0f, 1.2f);
        linearLayout.addView(textView);
        android.widget.TextView textView2 = new android.widget.TextView(this);
        java.lang.String str = "";
        for (int i = 0; i < memory.importance; i++) {
            str = str + "★";
        }
        textView2.setText(str + "  " + (memory.category != null ? memory.category : ""));
        textView2.setTextColor(-11677471);
        textView2.setTextSize(11.0f);
        textView2.setPadding(0, 4, 0, 0);
        linearLayout.addView(textView2);
        return linearLayout;
    }

    private android.view.View makeEventItem(com.echoflow.chat.StoryEvent storyEvent) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setBackgroundResource(com.echoflow.chat.R.drawable.bg_card_dark);
        linearLayout.setPadding(14, 10, 14, 10);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.bottomMargin = 6;
        linearLayout.setLayoutParams(layoutParams);
        java.lang.String format = storyEvent.date > 0 ? new java.text.SimpleDateFormat("MM.dd", java.util.Locale.getDefault()).format(new java.util.Date(storyEvent.date)) : "";
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText((format.isEmpty() ? "" : format + "  ") + storyEvent.title);
        textView.setTextColor(-1185802);
        textView.setTextSize(14.0f);
        textView.setTypeface(null, 1);
        linearLayout.addView(textView);
        if (storyEvent.description != null && !storyEvent.description.isEmpty()) {
            android.widget.TextView textView2 = new android.widget.TextView(this);
            textView2.setText(storyEvent.description);
            textView2.setTextColor(-7435608);
            textView2.setTextSize(12.0f);
            textView2.setPadding(0, 2, 0, 0);
            linearLayout.addView(textView2);
        }
        return linearLayout;
    }
}
