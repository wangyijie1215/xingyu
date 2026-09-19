package com.echoflow.chat;

/* loaded from: classes.dex */
public class CardChatActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.MessageAdapter adapter;
    private java.lang.String apiKey;
    private java.lang.String baseUrl;
    private com.google.android.material.button.MaterialButton btnRegen;
    private com.google.android.material.button.MaterialButton btnSend;
    private com.echoflow.chat.CharacterCard card;
    private android.widget.EditText input;
    private androidx.recyclerview.widget.RecyclerView list;
    private java.lang.String modelName;
    private com.echoflow.chat.Persona persona;
    private final java.util.List<com.echoflow.chat.Message> messages = new java.util.ArrayList();
    private boolean sending = false;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.echoflow.chat.R.layout.activity_card_chat);
        com.echoflow.chat.CharacterCard card = com.echoflow.chat.CardStore.getCard(this, getIntent().getStringExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID));
        this.card = card;
        if (card == null) {
            android.widget.Toast.makeText(this, "角色不存在", 0).show();
            finish();
            return;
        }
        java.util.List<com.echoflow.chat.Persona> listPersonas = com.echoflow.chat.CardStore.listPersonas(this);
        this.persona = (listPersonas == null || listPersonas.isEmpty()) ? new com.echoflow.chat.Persona() : listPersonas.get(0);
        this.modelName = (this.card.model == null || this.card.model.isEmpty()) ? com.echoflow.chat.ProviderStore.effectiveModel(this) : this.card.model;
        this.baseUrl = (this.card.baseUrl == null || this.card.baseUrl.isEmpty()) ? com.echoflow.chat.ProviderStore.effectiveBaseUrl(this) : this.card.baseUrl;
        this.apiKey = com.echoflow.chat.SecureStore.get(this);
        android.widget.ImageView imageView = (android.widget.ImageView) findViewById(com.echoflow.chat.R.id.chat_avatar);
        android.widget.TextView textView = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.chat_card_name);
        textView.setText(this.card.name);
        updateStatusBar(com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id), com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id));
        android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(this, this.card.id);
        if (loadAvatar != null) {
            imageView.setImageBitmap(loadAvatar);
        }
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.chat_shrine);
        if (textView2 != null) {
            com.echoflow.chat.Fortune load = com.echoflow.chat.FortuneService.load(this, this.card.id, com.echoflow.chat.Fortune.today());
            textView2.setText(load == null ? "⛩ 参拜" : "⛩ " + load.rank);
            textView2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CardChatActivity$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.CardChatActivity.this.lambda$onCreate$0(view);
                }
            });
        }
        androidx.recyclerview.widget.RecyclerView recyclerView = (androidx.recyclerview.widget.RecyclerView) findViewById(com.echoflow.chat.R.id.chat_list);
        this.list = recyclerView;
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.echoflow.chat.MessageAdapter messageAdapter = new com.echoflow.chat.MessageAdapter(this.messages);
        this.adapter = messageAdapter;
        this.list.setAdapter(messageAdapter);
        android.widget.EditText editText = (android.widget.EditText) findViewById(com.echoflow.chat.R.id.card_input);
        this.input = editText;
        editText.setInputType(147457);
        this.input.setOnEditorActionListener(new android.widget.TextView.OnEditorActionListener() { // from class: com.echoflow.chat.CardChatActivity$$ExternalSyntheticLambda1
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(android.widget.TextView textView3, int i, android.view.KeyEvent keyEvent) {
                boolean lambda$onCreate$1;
                lambda$onCreate$1 = com.echoflow.chat.CardChatActivity.this.lambda$onCreate$1(textView3, i, keyEvent);
                return lambda$onCreate$1;
            }
        });
        com.google.android.material.button.MaterialButton materialButton = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_card_send);
        this.btnSend = materialButton;
        materialButton.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CardChatActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CardChatActivity.this.lambda$onCreate$2(view);
            }
        });
        com.google.android.material.button.MaterialButton materialButton2 = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_regen);
        this.btnRegen = materialButton2;
        materialButton2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.CardChatActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.CardChatActivity.this.lambda$onCreate$3(view);
            }
        });
        loadHistory();
        if (this.messages.isEmpty()) {
            java.lang.String applyVars = com.echoflow.chat.PromptBuilder.applyVars(this.card.firstMes, this.card, this.persona);
            if ((applyVars == null || applyVars.isEmpty()) && this.card.alternateGreetings != null && !this.card.alternateGreetings.isEmpty()) {
                applyVars = com.echoflow.chat.PromptBuilder.applyVars(this.card.alternateGreetings.get(0), this.card, this.persona);
            }
            if (applyVars != null && !applyVars.isEmpty()) {
                this.messages.add(new com.echoflow.chat.Message("assistant", applyVars));
            }
            saveHistory();
        }
        this.adapter.notifyDataSetChanged();
        scrollToBottom(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.ShrineActivity.class);
        intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, this.card.id);
        startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreate$1(android.widget.TextView textView, int i, android.view.KeyEvent keyEvent) {
        sendMessage();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        sendMessage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.view.View view) {
        regenerate();
    }

    private void sendMessage() {
        if (this.sending) {
            return;
        }
        java.lang.String trim = this.input.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        java.lang.String str = this.apiKey;
        if (str == null || str.isEmpty()) {
            if (!com.echoflow.chat.ProviderStore.isOllama(this)) {
                android.widget.Toast.makeText(this, "请先到设置页填写 API 密钥（本地 Ollama 不需要）", 0).show();
                startActivity(new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.SettingsActivity.class));
                return;
            }
            this.apiKey = "";
        }
        com.echoflow.chat.Message message = new com.echoflow.chat.Message("user", trim);
        com.echoflow.chat.Message message2 = new com.echoflow.chat.Message("assistant", "");
        this.messages.add(message);
        this.messages.add(message2);
        this.input.setText("");
        this.adapter.notifyItemRangeInserted(this.messages.size() - 2, 2);
        scrollToBottom(true);
        startStream(message2);
    }

    private void regenerate() {
        if (this.sending) {
            return;
        }
        int size = this.messages.size() - 1;
        while (true) {
            if (size < 0) {
                size = -1;
                break;
            } else if ("assistant".equals(this.messages.get(size).role)) {
                break;
            } else {
                size--;
            }
        }
        if (size < 0) {
            return;
        }
        com.echoflow.chat.Message message = this.messages.get(size);
        message.content = "";
        this.adapter.notifyItemChanged(size);
        startStream(message);
    }

    private void startStream(com.echoflow.chat.Message message) {
        this.sending = true;
        this.btnSend.setEnabled(false);
        this.btnRegen.setEnabled(false);
        this.adapter.setGenerating(message);
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (com.echoflow.chat.Message message2 : this.messages) {
            if (message2 != message) {
                arrayList.add(message2);
            }
        }
        java.util.List<com.echoflow.chat.Memory> list = com.echoflow.chat.MemoryStore.topForPrompt(this, this.card.id, 15);
        com.echoflow.chat.RelationshipState loadRelationship = com.echoflow.chat.CharacterStateStore.loadRelationship(this, this.card.id);
        com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(this, this.card.id);
        com.echoflow.chat.Fortune load = com.echoflow.chat.FortuneService.load(this, this.card.id, com.echoflow.chat.Fortune.today());
        com.echoflow.chat.PromptBuilder.LifeContext buildLifeContext = buildLifeContext();
        updateStatusBar(loadRelationship, loadEmotion);
        com.echoflow.chat.ApiClient.streamChat(this.baseUrl, this.apiKey, this.modelName, com.echoflow.chat.PromptBuilder.buildMessages(this.card, this.persona, arrayList, list, loadRelationship, loadEmotion, load, buildLifeContext), com.echoflow.chat.ProviderStore.isOllama(this), new com.echoflow.chat.CardChatActivity.AnonymousClass1(message));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.echoflow.chat.CardChatActivity$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements com.echoflow.chat.ApiClient.Callback {
        final /* synthetic */ com.echoflow.chat.Message val$aiMsg;

        AnonymousClass1(com.echoflow.chat.Message message) {
            this.val$aiMsg = message;
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onChunk(final java.lang.String str) {
            com.echoflow.chat.CardChatActivity cardChatActivity = com.echoflow.chat.CardChatActivity.this;
            final com.echoflow.chat.Message message = this.val$aiMsg;
            cardChatActivity.runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.CardChatActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    com.echoflow.chat.CardChatActivity.AnonymousClass1.this.lambda$onChunk$0(message, str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChunk$0(com.echoflow.chat.Message message, java.lang.String str) {
            message.content = str;
            com.echoflow.chat.CardChatActivity.this.adapter.notifyItemChanged(com.echoflow.chat.CardChatActivity.this.messages.indexOf(message));
            com.echoflow.chat.CardChatActivity.this.scrollToBottom(true);
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onDone(final boolean z, final java.lang.String str) {
            com.echoflow.chat.CardChatActivity cardChatActivity = com.echoflow.chat.CardChatActivity.this;
            final com.echoflow.chat.Message message = this.val$aiMsg;
            cardChatActivity.runOnUiThread(new java.lang.Runnable() { // from class: com.echoflow.chat.CardChatActivity$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    com.echoflow.chat.CardChatActivity.AnonymousClass1.this.lambda$onDone$1(z, str, message);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDone$1(boolean z, java.lang.String str, com.echoflow.chat.Message message) {
            int indexOf;
            com.echoflow.chat.CardChatActivity.this.sending = false;
            com.echoflow.chat.CardChatActivity.this.adapter.setGenerating(null);
            com.echoflow.chat.CardChatActivity.this.btnSend.setEnabled(true);
            com.echoflow.chat.CardChatActivity.this.btnRegen.setEnabled(true);
            if (z && str != null && !str.isEmpty()) {
                android.widget.Toast.makeText(com.echoflow.chat.CardChatActivity.this, str, 1).show();
            }
            if (z && ((message.content == null || message.content.trim().isEmpty()) && (indexOf = com.echoflow.chat.CardChatActivity.this.messages.indexOf(message)) >= 0)) {
                com.echoflow.chat.CardChatActivity.this.messages.remove(indexOf);
                com.echoflow.chat.CardChatActivity.this.adapter.notifyItemRemoved(indexOf);
            }
            com.echoflow.chat.CardChatActivity.this.saveHistory();
            if (z) {
                return;
            }
            java.util.ArrayList arrayList = new java.util.ArrayList();
            for (int max = java.lang.Math.max(0, com.echoflow.chat.CardChatActivity.this.messages.size() - 6); max < com.echoflow.chat.CardChatActivity.this.messages.size(); max++) {
                com.echoflow.chat.Message message2 = (com.echoflow.chat.Message) com.echoflow.chat.CardChatActivity.this.messages.get(max);
                if (!"system".equals(message2.role) && message2.content != null && !message2.content.isEmpty()) {
                    arrayList.add(message2);
                }
            }
            com.echoflow.chat.CardChatActivity cardChatActivity = com.echoflow.chat.CardChatActivity.this;
            com.echoflow.chat.StateAnalyzer.analyze(cardChatActivity, cardChatActivity.card, com.echoflow.chat.CardChatActivity.this.persona, com.echoflow.chat.CardChatActivity.this.baseUrl, com.echoflow.chat.CardChatActivity.this.apiKey, com.echoflow.chat.CardChatActivity.this.modelName, arrayList);
        }
    }

    private java.io.File chatFile() {
        java.io.File file = new java.io.File(getFilesDir(), "chats");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new java.io.File(file, this.card.id + ".json");
    }

    private void loadHistory() {
        int i;
        java.io.File chatFile = chatFile();
        if (chatFile.exists()) {
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            try {
                java.io.FileReader fileReader = new java.io.FileReader(chatFile);
                try {
                    char[] cArr = new char[4096];
                    while (true) {
                        int read = fileReader.read(cArr);
                        if (read <= 0) {
                            break;
                        } else {
                            sb.append(cArr, 0, read);
                        }
                    }
                    fileReader.close();
                    try {
                        org.json.JSONArray jSONArray = new org.json.JSONArray(sb.toString());
                        for (i = 0; i < jSONArray.length(); i++) {
                            org.json.JSONObject jSONObject = jSONArray.getJSONObject(i);
                            this.messages.add(new com.echoflow.chat.Message(jSONObject.optString("role", "user"), jSONObject.optString("content", "")));
                        }
                    } catch (java.lang.Exception unused) {
                    }
                } finally {
                }
            } catch (java.lang.Exception unused2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveHistory() {
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        try {
            for (com.echoflow.chat.Message message : this.messages) {
                org.json.JSONObject jSONObject = new org.json.JSONObject();
                jSONObject.put("role", message.role);
                jSONObject.put("content", message.content);
                jSONArray.put(jSONObject);
            }
        } catch (java.lang.Exception unused) {
        }
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(chatFile());
            try {
                fileWriter.write(jSONArray.toString());
                fileWriter.close();
            } finally {
            }
        } catch (java.lang.Exception unused2) {
        }
    }

    private com.echoflow.chat.PromptBuilder.LifeContext buildLifeContext() {
        com.echoflow.chat.PromptBuilder.LifeContext lifeContext = new com.echoflow.chat.PromptBuilder.LifeContext();
        java.util.List<com.echoflow.chat.DiaryEntry> list = com.echoflow.chat.DiaryStore.list(this, this.card.id);
        java.lang.String str = com.echoflow.chat.Fortune.today();
        java.util.Iterator<com.echoflow.chat.DiaryEntry> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            com.echoflow.chat.DiaryEntry next = it.next();
            if (!next.date.equals(str) && next.unsaid != null && !next.unsaid.trim().isEmpty()) {
                lifeContext.lastDiaryUnsaid = next.unsaid.trim();
                break;
            }
        }
        java.util.Iterator<com.echoflow.chat.Letter> it2 = com.echoflow.chat.LetterStore.list(this, this.card.id).iterator();
        while (true) {
            if (!it2.hasNext()) {
                break;
            }
            com.echoflow.chat.Letter next2 = it2.next();
            if (next2.fromHer()) {
                lifeContext.lastLetterHint = next2.dateLabel + "，你在信里写过：「" + com.echoflow.chat.LetterService.excerptOf(next2.body, 40) + "」";
                break;
            }
        }
        lifeContext.sealedCapsuleTeasers = com.echoflow.chat.CapsuleStore.sealedTeasers(this, this.card.id);
        return lifeContext;
    }

    private void updateStatusBar(com.echoflow.chat.RelationshipState relationshipState, com.echoflow.chat.EmotionState emotionState) {
        android.widget.TextView textView = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.chat_model);
        if (textView == null) {
            return;
        }
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        sb.append(this.modelName);
        if (relationshipState != null) {
            sb.append(" · 关系：").append(relationshipState.levelName());
        }
        if (emotionState != null && emotionState.mood != null && !emotionState.mood.isEmpty()) {
            sb.append(" · ").append(emotionState.mood);
        }
        textView.setText(sb.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollToBottom(boolean z) {
        this.list.post(new java.lang.Runnable() { // from class: com.echoflow.chat.CardChatActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.CardChatActivity.this.lambda$scrollToBottom$4();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scrollToBottom$4() {
        int itemCount = this.adapter.getItemCount() - 1;
        if (itemCount >= 0) {
            this.list.smoothScrollToPosition(itemCount);
        }
    }
}
