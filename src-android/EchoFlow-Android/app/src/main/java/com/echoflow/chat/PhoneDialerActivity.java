package com.echoflow.chat;

/* loaded from: classes.dex */
public class PhoneDialerActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.echoflow.chat.PhoneDialerActivity.CallAdapter adapter;
    private final java.util.List<com.echoflow.chat.CharacterCard> cards = new java.util.ArrayList();
    private android.widget.TextView display;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        setContentView(com.echoflow.chat.R.layout.activity_phone_dialer);
        new com.echoflow.chat.PhoneStatusBar(this, findViewById(android.R.id.content)).bind();
        findViewById(com.echoflow.chat.R.id.di_back).setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneDialerActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(android.view.View view) {
                com.echoflow.chat.PhoneDialerActivity.this.lambda$onCreate$0(view);
            }
        });
        this.display = (android.widget.TextView) findViewById(com.echoflow.chat.R.id.di_display);
        androidx.recyclerview.widget.RecyclerView recyclerView = (androidx.recyclerview.widget.RecyclerView) findViewById(com.echoflow.chat.R.id.di_list);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.echoflow.chat.PhoneDialerActivity.CallAdapter callAdapter = new com.echoflow.chat.PhoneDialerActivity.CallAdapter();
        this.adapter = callAdapter;
        recyclerView.setAdapter(callAdapter);
        this.cards.addAll(com.echoflow.chat.PhoneStore.recentCards(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(android.view.View view) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        this.cards.clear();
        this.cards.addAll(com.echoflow.chat.PhoneStore.recentCards(this));
        this.adapter.notifyDataSetChanged();
        if (this.cards.isEmpty()) {
            this.display.setText("还没有联系人");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CallAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.echoflow.chat.PhoneDialerActivity.CallAdapter.VH> {
        CallAdapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public com.echoflow.chat.PhoneDialerActivity.CallAdapter.VH onCreateViewHolder(android.view.ViewGroup viewGroup, int i) {
            return new com.echoflow.chat.PhoneDialerActivity.CallAdapter.VH(android.view.LayoutInflater.from(viewGroup.getContext()).inflate(com.echoflow.chat.R.layout.item_dialer, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(com.echoflow.chat.PhoneDialerActivity.CallAdapter.VH vh, int i) {
            final com.echoflow.chat.CharacterCard characterCard = (com.echoflow.chat.CharacterCard) com.echoflow.chat.PhoneDialerActivity.this.cards.get(i);
            vh.name.setText(characterCard.name);
            android.graphics.Bitmap loadAvatar = com.echoflow.chat.CardStore.loadAvatar(com.echoflow.chat.PhoneDialerActivity.this, characterCard.id);
            if (loadAvatar != null) {
                vh.avatar.setImageBitmap(loadAvatar);
                vh.avatar.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            } else {
                vh.avatar.setImageResource(com.echoflow.chat.R.drawable.app_icon);
            }
            com.echoflow.chat.Schedule.Now now = com.echoflow.chat.Schedule.now(com.echoflow.chat.Schedule.load(com.echoflow.chat.PhoneDialerActivity.this, characterCard.id));
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            if (now.what != null && !now.what.isEmpty()) {
                sb.append("此刻：").append(now.what);
                if (now.busy) {
                    sb.append(" · 可能不方便接");
                }
            } else {
                sb.append("未知");
            }
            com.echoflow.chat.EmotionState loadEmotion = com.echoflow.chat.CharacterStateStore.loadEmotion(com.echoflow.chat.PhoneDialerActivity.this, characterCard.id);
            if (loadEmotion != null && loadEmotion.mood != null && !loadEmotion.mood.isEmpty()) {
                sb.append(" · ").append(loadEmotion.mood);
            }
            vh.status.setText(sb.toString());
            vh.name.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneDialerActivity$CallAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.PhoneDialerActivity.CallAdapter.this.lambda$onBindViewHolder$0(characterCard, view);
                }
            });
            vh.call.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneDialerActivity$CallAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.PhoneDialerActivity.CallAdapter.this.lambda$onBindViewHolder$1(characterCard, view);
                }
            });
            vh.itemView.setOnClickListener(new android.view.View.OnClickListener() { // from class: com.echoflow.chat.PhoneDialerActivity$CallAdapter$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(android.view.View view) {
                    com.echoflow.chat.PhoneDialerActivity.CallAdapter.this.lambda$onBindViewHolder$2(characterCard, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(com.echoflow.chat.CharacterCard characterCard, android.view.View view) {
            com.echoflow.chat.PhoneDialerActivity.this.display.setText(characterCard.name);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$1(com.echoflow.chat.CharacterCard characterCard, android.view.View view) {
            com.echoflow.chat.PhoneDialerActivity.this.display.setText("正在呼叫 " + characterCard.name + "…");
            com.echoflow.chat.Schedule.Now now = com.echoflow.chat.Schedule.now(com.echoflow.chat.Schedule.load(com.echoflow.chat.PhoneDialerActivity.this, characterCard.id));
            if (now.busy) {
                android.widget.Toast.makeText(com.echoflow.chat.PhoneDialerActivity.this, characterCard.name + " 现在正在「" + now.what + "」，可能会晚一点接", 0).show();
            }
            android.content.Intent intent = new android.content.Intent(com.echoflow.chat.PhoneDialerActivity.this, (java.lang.Class<?>) com.echoflow.chat.VoiceCallActivity.class);
            intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, characterCard.id);
            com.echoflow.chat.PhoneDialerActivity.this.startActivity(intent);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$2(com.echoflow.chat.CharacterCard characterCard, android.view.View view) {
            android.content.Intent intent = new android.content.Intent(com.echoflow.chat.PhoneDialerActivity.this, (java.lang.Class<?>) com.echoflow.chat.PhoneChatActivity.class);
            intent.putExtra(com.echoflow.chat.PetService.EXTRA_CARD_ID, characterCard.id);
            com.echoflow.chat.PhoneDialerActivity.this.startActivity(intent);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return com.echoflow.chat.PhoneDialerActivity.this.cards.size();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final android.widget.ImageView avatar;
            final android.widget.TextView call;
            final android.widget.TextView name;
            final android.widget.TextView status;

            VH(android.view.View view) {
                super(view);
                this.avatar = (android.widget.ImageView) view.findViewById(com.echoflow.chat.R.id.dr_avatar);
                this.name = (android.widget.TextView) view.findViewById(com.echoflow.chat.R.id.dr_name);
                this.status = (android.widget.TextView) view.findViewById(com.echoflow.chat.R.id.dr_status);
                this.call = (android.widget.TextView) view.findViewById(com.echoflow.chat.R.id.dr_call);
            }
        }
    }
}
