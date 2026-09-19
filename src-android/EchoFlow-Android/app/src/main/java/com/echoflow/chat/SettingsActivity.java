package com.echoflow.chat;

/* loaded from: classes.dex */
public class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private android.view.View boxCloud;
    private android.view.View boxLocal;
    private android.widget.LinearLayout boxModels;
    private com.google.android.material.button.MaterialButton btnDetect;
    private com.google.android.material.button.MaterialButton btnKindCloud;
    private com.google.android.material.button.MaterialButton btnKindLocal;
    private com.google.android.material.textfield.TextInputEditText inputApiKey;
    private com.google.android.material.textfield.TextInputEditText inputBaseUrl;
    private com.google.android.material.textfield.TextInputEditText inputModel;
    private com.google.android.material.textfield.TextInputEditText inputOllamaHost;
    private com.google.android.material.textfield.TextInputEditText inputPersona;
    private java.util.List<java.lang.String> probedModels;
    private android.widget.TextView tvKindHint;
    private android.widget.TextView tvProbe;
    private java.lang.String currentKind = com.echoflow.chat.ProviderStore.KIND_CLOUD;
    private java.lang.String pickedModel = "";
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.echoflow.chat.R.layout.activity_settings);
        this.inputOllamaHost = (com.google.android.material.textfield.TextInputEditText) findViewById(com.echoflow.chat.R.id.input_ollama_host);
        this.inputBaseUrl = (com.google.android.material.textfield.TextInputEditText) findViewById(com.echoflow.chat.R.id.input_base_url);
        this.inputApiKey = (com.google.android.material.textfield.TextInputEditText) findViewById(com.echoflow.chat.R.id.input_api_key);
        this.inputModel = (com.google.android.material.textfield.TextInputEditText) findViewById(com.echoflow.chat.R.id.input_model);
        this.inputPersona = (com.google.android.material.textfield.TextInputEditText) findViewById(com.echoflow.chat.R.id.input_persona);
        this.btnKindLocal = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_kind_local);
        this.btnKindCloud = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_kind_cloud);
        this.boxLocal = findViewById(com.echoflow.chat.R.id.box_local);
        this.boxCloud = findViewById(com.echoflow.chat.R.id.box_cloud);
        this.tvKindHint = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.tv_kind_hint);
        this.tvProbe = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.tv_probe);
        this.boxModels = (android.widget.LinearLayout) findViewById(com.echoflow.chat.R.id.box_models);
        this.btnDetect = (com.google.android.material.button.MaterialButton) findViewById(com.echoflow.chat.R.id.btn_detect);
        this.currentKind = com.echoflow.chat.ProviderStore.kind(this);
        this.inputOllamaHost.setText(com.echoflow.chat.ProviderStore.cleanHost(com.echoflow.chat.ProviderStore.baseUrl(this)).isEmpty() ? com.echoflow.chat.ProviderStore.OLLAMA_HOST_EMULATOR : com.echoflow.chat.ProviderStore.cleanHost(com.echoflow.chat.ProviderStore.baseUrl(this)));
        // 回填必须读**同一个** store，否则进页面永远是空的、一保存又把刚填的覆盖掉。
        // 原来这里读 ChatStore（老配置）、save() 写 ProviderStore（新配置），
        // 两边不通 —— 这是"模型不存在"的直接原因。
        this.inputBaseUrl.setText(com.echoflow.chat.ProviderStore.baseUrl(this));
        this.inputModel.setText(com.echoflow.chat.ProviderStore.model(this));
        this.inputPersona.setText(com.echoflow.chat.ChatStore.getPersona(this));
        java.lang.String str = com.echoflow.chat.SecureStore.get(this);
        com.google.android.material.textfield.TextInputEditText textInputEditText = this.inputApiKey;
        if (str == null) {
            str = "";
        }
        textInputEditText.setText(str);
        this.btnKindLocal.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.SettingsActivity.this.lambda$onCreate$0(view);
            }
        });
        this.btnKindCloud.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.SettingsActivity.this.lambda$onCreate$1(view);
            }
        });
        this.btnDetect.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.SettingsActivity.this.lambda$onCreate$2(view);
            }
        });
        findViewById(com.echoflow.chat.R.id.btn_save).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.SettingsActivity.this.lambda$onCreate$3(view);
            }
        });
        findViewById(com.echoflow.chat.R.id.btn_back).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.SettingsActivity.this.lambda$onCreate$4(view);
            }
        });
        applyKind();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        setKind(com.echoflow.chat.ProviderStore.KIND_OLLAMA);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(android.view.View view) {
        setKind(com.echoflow.chat.ProviderStore.KIND_CLOUD);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2(android.view.View view) {
        doDetect();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$3(android.view.View view) {
        save();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$4(android.view.View view) {
        finish();
    }

    private void setKind(java.lang.String str) {
        this.currentKind = str;
        applyKind();
    }

    private void applyKind() {
        java.lang.String str;
        boolean equals = com.echoflow.chat.ProviderStore.KIND_OLLAMA.equals(this.currentKind);
        this.boxLocal.setVisibility(equals ? 0 : 8);
        this.boxCloud.setVisibility(equals ? 8 : 0);
        styleKindButton(this.btnKindLocal, equals);
        styleKindButton(this.btnKindCloud, !equals);
        android.widget.TextView textView = this.tvKindHint;
        if (equals) {
            str = "本地模型跑在你自己电脑上，不需要 API Key，也不消耗额度。";
        } else {
            str = "云端 OpenAI 兼容服务（Grok / OpenAI / 自建中转）。需要填 API 密钥。";
        }
        textView.setText(str);
    }

    private void styleKindButton(com.google.android.material.button.MaterialButton materialButton, boolean z) {
        if (z) {
            materialButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(com.echoflow.chat.R.color.ef_primary)));
            materialButton.setTextColor(-1);
        } else {
            materialButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0));
            materialButton.setTextColor(getColor(com.echoflow.chat.R.color.ef_text_2));
        }
    }

    private void doDetect() {
        java.lang.String text = text(this.inputOllamaHost);
        if (text.isEmpty()) {
            text = com.echoflow.chat.ProviderStore.OLLAMA_HOST_EMULATOR;
        }
        // 传给线程的必须是 final（Java 闭包要求），所以单独拷一份
        final java.lang.String hostForThread = text;
        this.btnDetect.setEnabled(false);
        this.btnDetect.setText("检测中…");
        this.tvProbe.setText("正在连接 " + com.echoflow.chat.ProviderStore.cleanHost(text) + " …");
        this.boxModels.removeAllViews();
        new java.lang.Thread(new java.lang.Runnable() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.SettingsActivity.this.lambda$doDetect$6(hostForThread);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$doDetect$6(java.lang.String str) {
        final com.echoflow.chat.ProviderStore.Probe probe = com.echoflow.chat.ProviderStore.probe(str);
        this.ui.post(new java.lang.Runnable() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                com.echoflow.chat.SettingsActivity.this.lambda$doDetect$5(probe);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$doDetect$5(com.echoflow.chat.ProviderStore.Probe probe) {
        this.btnDetect.setEnabled(true);
        this.btnDetect.setText("检测并拉取模型列表");
        if (probe.ok) {
            this.probedModels = probe.models;
            this.tvProbe.setText("✓ " + probe.message + "\n地址：" + probe.host);
            this.tvProbe.setTextColor(getColor(com.echoflow.chat.R.color.ef_memory));
            this.inputOllamaHost.setText(probe.host);
            renderModels(probe.models);
            return;
        }
        this.tvProbe.setText("✗ " + probe.message);
        this.tvProbe.setTextColor(getColor(com.echoflow.chat.R.color.ef_emotion));
    }

    private void renderModels(final java.util.List<java.lang.String> list) {
        this.boxModels.removeAllViews();
        if (list == null || list.isEmpty()) {
            return;
        }
        android.widget.TextView textView = new android.widget.TextView(this);
        textView.setText("选一个模型（当前：" + (this.pickedModel.isEmpty() ? "未选" : this.pickedModel) + "）");
        textView.setTextColor(getColor(com.echoflow.chat.R.color.ef_text_2));
        textView.setTextSize(13.0f);
        textView.setPadding(0, 0, 0, dp(8));
        this.boxModels.addView(textView);
        android.widget.LinearLayout linearLayout = null;
        for (int i = 0; i < list.size(); i++) {
            if (i % 1 == 0) {
                linearLayout = new android.widget.LinearLayout(this);
                linearLayout.setOrientation(0);
                android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
                layoutParams.bottomMargin = dp(6);
                linearLayout.setLayoutParams(layoutParams);
                this.boxModels.addView(linearLayout);
            }
            final java.lang.String str = list.get(i);
            android.widget.TextView textView2 = new android.widget.TextView(this);
            textView2.setText(str);
            textView2.setTextSize(13.0f);
            textView2.setGravity(16);
            textView2.setPadding(dp(12), dp(10), dp(12), dp(10));
            textView2.setBackground(com.echoflow.chat.EfUi.roundRectPx(352321535, 872415231, dp(10)));
            textView2.setTextColor(getColor(com.echoflow.chat.R.color.ef_text_1));
            textView2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
            textView2.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.SettingsActivity$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.SettingsActivity.this.lambda$renderModels$7(str, list, view);
                }
            });
            linearLayout.addView(textView2);
        }
        if (this.pickedModel.isEmpty()) {
            this.pickedModel = list.get(0);
            renderModels(list);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$renderModels$7(java.lang.String str, java.util.List list, android.view.View view) {
        this.pickedModel = str;
        renderModels(list);
        android.widget.Toast.makeText(this, "已选择 " + str, 0).show();
    }

    private void save() {
        if (com.echoflow.chat.ProviderStore.KIND_OLLAMA.equals(this.currentKind)) {
            java.lang.String cleanHost = com.echoflow.chat.ProviderStore.cleanHost(text(this.inputOllamaHost));
            if (cleanHost.isEmpty()) {
                cleanHost = com.echoflow.chat.ProviderStore.OLLAMA_HOST_EMULATOR;
            }
            java.lang.String str = this.pickedModel;
            if (str.isEmpty()) {
                android.widget.Toast.makeText(this, "未检测模型，已用默认 qwen2.5:7b。建议点一次「检测并拉取模型列表」", 1).show();
                str = "qwen2.5:7b";
            }
            com.echoflow.chat.ProviderStore.save(this, com.echoflow.chat.ProviderStore.KIND_OLLAMA, cleanHost, str);
        } else {
            java.lang.String text = text(this.inputApiKey);
            if (text.isEmpty()) {
                android.widget.Toast.makeText(this, "请输入 API 密钥", 0).show();
                return;
            }
            try {
                com.echoflow.chat.SecureStore.save(this, text);
                java.lang.String text2 = text(this.inputBaseUrl);
                java.lang.String text3 = text(this.inputModel);
                com.echoflow.chat.ChatStore.saveConfig(this, text2, text3);
                com.echoflow.chat.ProviderStore.save(this, com.echoflow.chat.ProviderStore.KIND_CLOUD, text2, text3);
            } catch (java.lang.Exception e) {
                android.widget.Toast.makeText(this, "密钥保存失败: " + e.getMessage(), 1).show();
                return;
            }
        }
        com.echoflow.chat.ChatStore.savePersona(this, text(this.inputPersona));
        android.widget.Toast.makeText(this, com.echoflow.chat.R.string.toast_saved, 0).show();
        finish();
    }

    private java.lang.String text(android.widget.EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int dp(int i) {
        return java.lang.Math.round(i * getResources().getDisplayMetrics().density);
    }
}
