package com.echoflow.chat;

/* loaded from: classes.dex */
public class GeneralAssistantActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.PhoneMessageAdapter adapter;
    private android.widget.EditText input;
    private androidx.recyclerview.widget.RecyclerView list;
    private android.widget.TextView send;
    private android.widget.TextView subtitle;
    private final java.util.List<com.echoflow.chat.Message> messages = new java.util.ArrayList();
    private boolean sending = false;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.echoflow.chat.R.layout.activity_phone_chat);
        ((android.widget.TextView) findViewById(com.echoflow.chat.R.id.wx_title)).setText(com.echoflow.chat.GeneralAssistant.NAME);
        android.widget.TextView textView = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.wx_subtitle);
        this.subtitle = textView;
        textView.setTextColor(-7633752);
        updateSubtitle();
        findViewById(com.echoflow.chat.R.id.wx_back).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$onCreate$0(view);
            }
        });
        findViewById(com.echoflow.chat.R.id.wx_more).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$onCreate$1(view);
            }
        });
        new com.echoflow.chat.PhoneStatusBar(this, findViewById(android.R.id.content)).bind();
        androidx.recyclerview.widget.RecyclerView recyclerView = (androidx.recyclerview.widget.RecyclerView) findViewById(com.echoflow.chat.R.id.wx_list);
        this.list = recyclerView;
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.echoflow.chat.PhoneMessageAdapter phoneMessageAdapter = new com.echoflow.chat.PhoneMessageAdapter(this.messages, null);
        this.adapter = phoneMessageAdapter;
        this.list.setAdapter(phoneMessageAdapter);
        android.widget.EditText editText = (android.widget.EditText) findViewById(com.echoflow.chat.R.id.wx_input);
        this.input = editText;
        editText.setHint("问点什么…");
        this.input.setInputType(147457);
        android.widget.TextView textView2 = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.wx_send);
        this.send = textView2;
        textView2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$onCreate$2(view);
            }
        });
        findViewById(com.echoflow.chat.R.id.wx_voice).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$onCreate$3(view);
            }
        });
        this.messages.addAll(load());
        if (this.messages.isEmpty()) {
            this.messages.add(new com.echoflow.chat.Message("assistant", com.echoflow.chat.GeneralAssistant.greeting()));
        }
        this.adapter.notifyDataSetChanged();
        scrollToBottom(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.view.View view) {
        showMenu();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        sendMessage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.view.View view) {
        android.widget.Toast.makeText(this, "语音输入待接入", 0).show();
    }

    private void updateSubtitle() {
        com.echoflow.chat.Skill byId = com.echoflow.chat.Skill.byId(com.echoflow.chat.GeneralAssistant.activeSkillId());
        if (byId == null) {
            this.subtitle.setText("通用助手 · 无技能");
        } else {
            this.subtitle.setText(byId.icon + " " + byId.name + " 已开启");
        }
    }

    private void pickSkill() {
        final java.util.List<com.echoflow.chat.Skill> all = com.echoflow.chat.Skill.all();
        java.lang.String[] strArr = new java.lang.String[all.size() + 1];
        int i = 0;
        strArr[0] = "✖ 不用技能（普通对话）";
        while (i < all.size()) {
            com.echoflow.chat.Skill skill = all.get(i);
            i++;
            strArr[i] = skill.icon + " " + skill.name + " — " + skill.desc;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("选一个技能").setItems(strArr, new android.content.DialogInterface.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda3
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i2) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$pickSkill$4(all, dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$pickSkill$4(java.util.List list, android.content.DialogInterface dialogInterface, int i) {
        if (i == 0) {
            com.echoflow.chat.GeneralAssistant.setActiveSkill(null);
            android.widget.Toast.makeText(this, "已关闭技能", 0).show();
        } else {
            com.echoflow.chat.Skill skill = (com.echoflow.chat.Skill) list.get(i - 1);
            com.echoflow.chat.GeneralAssistant.setActiveSkill(skill.id);
            android.widget.Toast.makeText(this, skill.name + " 已开启", 0).show();
        }
        updateSubtitle();
    }

    private void showMenu() {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("助手").setItems(new java.lang.String[]{"选择技能", "我的文档", "清空对话", "关于 YIJIE"}, new android.content.DialogInterface.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda4
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$showMenu$5(dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showMenu$5(android.content.DialogInterface dialogInterface, int i) {
        if (i == 0) {
            pickSkill();
            return;
        }
        if (i == 1) {
            showDocs();
            return;
        }
        if (i == 2) {
            this.messages.clear();
            this.messages.add(new com.echoflow.chat.Message("assistant", com.echoflow.chat.GeneralAssistant.greeting()));
            this.adapter.notifyDataSetChanged();
            save();
            android.widget.Toast.makeText(this, "已清空", 0).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("关于 YIJIE").setMessage("YIJIE 是一个**有性格但不扮演**的助手。\n\n· 说话直接，有判断，不和稀泥\n· 不注入角色的记忆、关系、情绪、日程、世界书\n· 不卖萌、不谈感情话题\n\n技能：写文档 / 写代码 / 总结 / 翻译 / 做计划 / 分析\n写文档时产物会自动存到「我的文档」里。").setPositiveButton("懂了", (android.content.DialogInterface.OnClickListener) null).show();
    }

    private java.io.File file() {
        java.io.File file = new java.io.File(getFilesDir(), "general");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new java.io.File(file, "chat.json");
    }

    private java.util.List<com.echoflow.chat.Message> load() {
        int i;
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.io.File file = file();
        if (!file.exists()) {
            return arrayList;
        }
        try {
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            java.io.FileReader fileReader = new java.io.FileReader(file);
            try {
                char[] cArr = new char[4096];
                while (true) {
                    int read = fileReader.read(cArr);
                    if (read <= 0) {
                        break;
                    }
                    sb.append(cArr, 0, read);
                }
                fileReader.close();
                org.json.JSONArray jSONArray = new org.json.JSONArray(sb.toString());
                for (i = 0; i < jSONArray.length(); i++) {
                    org.json.JSONObject optJSONObject = jSONArray.optJSONObject(i);
                    if (optJSONObject != null) {
                        arrayList.add(new com.echoflow.chat.Message(optJSONObject.optString("role", "user"), optJSONObject.optString("content", "")));
                    }
                }
            } finally {
            }
        } catch (java.lang.Exception unused) {
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void save() {
        org.json.JSONArray jSONArray = new org.json.JSONArray();
        for (com.echoflow.chat.Message message : this.messages) {
            try {
                org.json.JSONObject jSONObject = new org.json.JSONObject();
                jSONObject.put("role", message.role);
                jSONObject.put("content", message.content);
                jSONArray.put(jSONObject);
            } catch (java.lang.Exception unused) {
            }
        }
        try {
            java.io.FileWriter fileWriter = new java.io.FileWriter(file());
            try {
                fileWriter.write(jSONArray.toString());
                fileWriter.close();
            } finally {
            }
        } catch (java.lang.Exception unused2) {
        }
    }

    private void sendMessage() {
        if (this.sending) {
            return;
        }
        java.lang.String trim = this.input.getText().toString().trim();
        if (trim.isEmpty()) {
            return;
        }
        java.lang.String str = com.echoflow.chat.SecureStore.get(this);
        boolean isOllama = com.echoflow.chat.ProviderStore.isOllama(this);
        if (!isOllama && (str == null || str.isEmpty())) {
            android.widget.Toast.makeText(this, "请先到设置页配置模型（本地 Ollama 不需要密钥）", 0).show();
            startActivity(new android.content.Intent(this, (java.lang.Class<?>) com.echoflow.chat.SettingsActivity.class));
            return;
        }
        com.echoflow.chat.Message message = new com.echoflow.chat.Message("user", trim);
        com.echoflow.chat.Message message2 = new com.echoflow.chat.Message("assistant", "");
        this.messages.add(message);
        this.messages.add(message2);
        this.input.setText("");
        this.adapter.notifyItemRangeInserted(this.messages.size() - 2, 2);
        scrollToBottom(true);
        this.sending = true;
        this.send.setEnabled(false);
        this.adapter.setGenerating(message2);
        this.adapter.notifyItemChanged(this.messages.size() - 1);
        java.util.ArrayList arrayList = new java.util.ArrayList();
        for (com.echoflow.chat.Message message3 : this.messages) {
            if (message3 != message2) {
                arrayList.add(message3);
            }
        }
        com.echoflow.chat.ApiClient.streamChat(com.echoflow.chat.ProviderStore.effectiveBaseUrl(this), str == null ? "" : str, com.echoflow.chat.ProviderStore.effectiveModel(this), com.echoflow.chat.GeneralAssistant.build(arrayList), isOllama, new com.echoflow.chat.GeneralAssistantActivity.AnonymousClass1(message2));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.echoflow.chat.GeneralAssistantActivity$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements com.echoflow.chat.ApiClient.Callback {
        final /* synthetic */ com.echoflow.chat.Message val$aiMsg;

        AnonymousClass1(com.echoflow.chat.Message message) {
            this.val$aiMsg = message;
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onChunk(final java.lang.String str) {
            android.os.Handler handler = com.echoflow.chat.GeneralAssistantActivity.this.ui;
            final com.echoflow.chat.Message message = this.val$aiMsg;
            handler.post(new java.lang.Runnable() { // from class: com.echoflow.chat.GeneralAssistantActivity$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    com.echoflow.chat.GeneralAssistantActivity.AnonymousClass1.this.lambda$onChunk$0(message, str);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onChunk$0(com.echoflow.chat.Message message, java.lang.String str) {
            message.content = str;
            com.echoflow.chat.GeneralAssistantActivity.this.adapter.notifyItemChanged(com.echoflow.chat.GeneralAssistantActivity.this.messages.indexOf(message));
            com.echoflow.chat.GeneralAssistantActivity.this.scrollToBottom(true);
        }

        @Override // com.echoflow.chat.ApiClient.Callback
        public void onDone(final boolean z, final java.lang.String str) {
            android.os.Handler handler = com.echoflow.chat.GeneralAssistantActivity.this.ui;
            final com.echoflow.chat.Message message = this.val$aiMsg;
            handler.post(new java.lang.Runnable() { // from class: com.echoflow.chat.GeneralAssistantActivity$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    com.echoflow.chat.GeneralAssistantActivity.AnonymousClass1.this.lambda$onDone$1(z, str, message);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onDone$1(boolean z, java.lang.String str, com.echoflow.chat.Message message) {
            java.lang.String saveDocument;
            int indexOf;
            com.echoflow.chat.GeneralAssistantActivity.this.sending = false;
            com.echoflow.chat.GeneralAssistantActivity.this.send.setEnabled(true);
            com.echoflow.chat.GeneralAssistantActivity.this.adapter.setGenerating(null);
            if (z && str != null && !str.isEmpty()) {
                android.widget.Toast.makeText(com.echoflow.chat.GeneralAssistantActivity.this, str, 1).show();
            }
            if (z && ((message.content == null || message.content.trim().isEmpty()) && (indexOf = com.echoflow.chat.GeneralAssistantActivity.this.messages.indexOf(message)) >= 0)) {
                com.echoflow.chat.GeneralAssistantActivity.this.messages.remove(indexOf);
                com.echoflow.chat.GeneralAssistantActivity.this.adapter.notifyItemRemoved(indexOf);
            }
            if (!z && "doc".equals(com.echoflow.chat.GeneralAssistant.activeSkillId()) && com.echoflow.chat.GeneralAssistant.looksLikeDocument(message.content) && (saveDocument = com.echoflow.chat.Skill.saveDocument(com.echoflow.chat.GeneralAssistantActivity.this, message.content)) != null) {
                message.content += "\n\n---\n📄 已保存到「我的文档」：" + saveDocument;
                com.echoflow.chat.GeneralAssistantActivity.this.adapter.notifyItemChanged(com.echoflow.chat.GeneralAssistantActivity.this.messages.indexOf(message));
                android.widget.Toast.makeText(com.echoflow.chat.GeneralAssistantActivity.this, "文档已保存", 0).show();
            }
            com.echoflow.chat.GeneralAssistantActivity.this.save();
        }
    }

    private void showDocs() {
        java.util.List<java.io.File> documents = com.echoflow.chat.Skill.documents(this);
        if (documents.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("我的文档").setMessage("还没有文档。\n\n在菜单里选「写文档」技能，然后让它写点什么，生成的 Markdown 会自动存到这里。").setPositiveButton("好", (android.content.DialogInterface.OnClickListener) null).show();
            return;
        }
        final java.lang.String[] strArr = new java.lang.String[documents.size()];
        for (int i = 0; i < documents.size(); i++) {
            strArr[i] = documents.get(i).getName();
        }
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("我的文档（" + documents.size() + "）").setItems(strArr, new android.content.DialogInterface.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i2) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$showDocs$6(strArr, dialogInterface, i2);
            }
        }).setNeutralButton("全删", new android.content.DialogInterface.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda1
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(android.content.DialogInterface dialogInterface, int i2) {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$showDocs$7(strArr, dialogInterface, i2);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDocs$6(java.lang.String[] strArr, android.content.DialogInterface dialogInterface, int i) {
        openDoc(strArr[i]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showDocs$7(java.lang.String[] strArr, android.content.DialogInterface dialogInterface, int i) {
        for (java.lang.String str : strArr) {
            com.echoflow.chat.Skill.deleteDoc(this, str);
        }
        android.widget.Toast.makeText(this, "已清空", 0).show();
    }

    private void openDoc(final java.lang.String str) {
        java.io.File docFile = com.echoflow.chat.Skill.docFile(this, str);
        if (!docFile.exists()) {
            android.widget.Toast.makeText(this, "文件不见了", 0).show();
            return;
        }
        try {
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            java.io.FileReader fileReader = new java.io.FileReader(docFile);
            try {
                char[] cArr = new char[4096];
                while (true) {
                    int read = fileReader.read(cArr);
                    if (read > 0) {
                        sb.append(cArr, 0, read);
                    } else {
                        fileReader.close();
                        final java.lang.String sb2 = sb.toString();
                        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                        android.widget.TextView textView = new android.widget.TextView(this);
                        textView.setText(sb2);
                        textView.setTextSize(13.0f);
                        textView.setTextIsSelectable(true);
                        textView.setPadding(dp(16), dp(16), dp(16), dp(16));
                        textView.setLineSpacing(dp(3), 1.0f);
                        scrollView.addView(textView);
                        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(str).setView(scrollView).setPositiveButton("关", (android.content.DialogInterface.OnClickListener) null).setNeutralButton("导出", new android.content.DialogInterface.OnClickListener() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda2
                            @Override // android.content.DialogInterface.OnClickListener
                            public final void onClick(android.content.DialogInterface dialogInterface, int i) {
                                com.echoflow.chat.GeneralAssistantActivity.this.lambda$openDoc$8(str, sb2, dialogInterface, i);
                            }
                        }).show();
                        return;
                    }
                }
            } finally {
            }
        } catch (java.lang.Exception unused) {
            android.widget.Toast.makeText(this, "读取失败", 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openDoc$8(java.lang.String str, java.lang.String str2, android.content.DialogInterface dialogInterface, int i) {
        exportDoc(str, str2);
    }

    private void exportDoc(java.lang.String str, java.lang.String str2) {
        try {
            java.io.File file = new java.io.File(getExternalFilesDir(null), "docs");
            if (!file.exists()) {
                file.mkdirs();
            }
            java.io.File file2 = new java.io.File(file, str);
            java.io.FileWriter fileWriter = new java.io.FileWriter(file2);
            try {
                fileWriter.write(str2);
                fileWriter.close();
                new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("已导出").setMessage("文件位置：\n" + file2.getAbsolutePath() + "\n\n这个路径在 Android/data/com.echoflow.chat/files/docs 下，用文件管理器能找到。").setPositiveButton("好", (android.content.DialogInterface.OnClickListener) null).show();
            } finally {
            }
        } catch (java.lang.Exception e) {
            android.widget.Toast.makeText(this, "导出失败：" + e.getMessage(), 1).show();
        }
    }

    private int dp(int i) {
        return java.lang.Math.round(i * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scrollToBottom(final boolean z) {
        if (this.messages.isEmpty()) {
            return;
        }
        this.list.post(new java.lang.Runnable() { // from class: com.echoflow.chat.GeneralAssistantActivity$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.GeneralAssistantActivity.this.lambda$scrollToBottom$9(z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$scrollToBottom$9(boolean z) {
        int itemCount = this.adapter.getItemCount() - 1;
        if (itemCount >= 0) {
            if (z) {
                this.list.smoothScrollToPosition(itemCount);
            } else {
                this.list.scrollToPosition(itemCount);
            }
        }
    }
}
