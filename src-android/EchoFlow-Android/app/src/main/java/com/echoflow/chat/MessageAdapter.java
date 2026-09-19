package com.echoflow.chat;

/* loaded from: classes.dex */
public class MessageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> {
    private static final int TYPE_AI = 2;
    private static final int TYPE_USER = 1;
    private com.echoflow.chat.Message generating;
    private final java.util.List<com.echoflow.chat.Message> messages;

    public MessageAdapter(java.util.List<com.echoflow.chat.Message> list) {
        this.messages = list;
    }

    public void setGenerating(com.echoflow.chat.Message message) {
        this.generating = message;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemViewType(int i) {
        return "user".equals(this.messages.get(i).role) ? 1 : 2;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup viewGroup, int i) {
        android.view.LayoutInflater from = android.view.LayoutInflater.from(viewGroup.getContext());
        if (i == 1) {
            return new com.echoflow.chat.MessageAdapter.UserHolder(from.inflate(com.echoflow.chat.R.layout.item_message_user, viewGroup, false));
        }
        return new com.echoflow.chat.MessageAdapter.AiHolder(from.inflate(com.echoflow.chat.R.layout.item_message_ai, viewGroup, false));
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int i) {
        com.echoflow.chat.Message message = this.messages.get(i);
        if (viewHolder instanceof com.echoflow.chat.MessageAdapter.UserHolder) {
            ((com.echoflow.chat.MessageAdapter.UserHolder) viewHolder).text.setText(message.content);
        } else {
            ((com.echoflow.chat.MessageAdapter.AiHolder) viewHolder).bind(message.content, message == this.generating);
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.messages.size();
    }

    /* loaded from: classes.dex */
    static class UserHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final android.widget.TextView text;

        UserHolder(android.view.View view) {
            super(view);
            this.text = (android.widget.TextView) view.findViewById(com.echoflow.chat.R.id.user_text);
        }
    }

    /* loaded from: classes.dex */
    static class AiHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        final android.widget.LinearLayout container;
        final android.content.Context ctx;

        AiHolder(android.view.View view) {
            super(view);
            this.container = (android.widget.LinearLayout) view.findViewById(com.echoflow.chat.R.id.ai_container);
            this.ctx = view.getContext();
        }

        void bind(java.lang.String str, boolean z) {
            this.container.removeAllViews();
            int color = androidx.core.content.ContextCompat.getColor(this.ctx, com.echoflow.chat.R.color.text_primary);
            int color2 = androidx.core.content.ContextCompat.getColor(this.ctx, com.echoflow.chat.R.color.text_secondary);
            int color3 = androidx.core.content.ContextCompat.getColor(this.ctx, com.echoflow.chat.R.color.divider);
            if (str == null || str.trim().isEmpty()) {
                if (z) {
                    android.widget.TextView textView = new android.widget.TextView(this.ctx);
                    textView.setText("● ● ●");
                    textView.setTextSize(10.0f);
                    textView.setTextColor(color2);
                    this.container.addView(textView);
                    this.container.setVisibility(0);
                    return;
                }
                this.container.setVisibility(8);
                return;
            }
            this.container.setVisibility(0);
            java.util.List<com.echoflow.chat.MarkdownParser.Segment> parse = com.echoflow.chat.MarkdownParser.parse(str);
            if (parse.isEmpty()) {
                android.widget.TextView textView2 = new android.widget.TextView(this.ctx);
                textView2.setText(str);
                textView2.setTextSize(16.0f);
                textView2.setTextColor(color);
                this.container.addView(textView2);
                return;
            }
            for (com.echoflow.chat.MarkdownParser.Segment segment : parse) {
                if ("code".equals(segment.type)) {
                    if (segment.lang != null && !segment.lang.isEmpty()) {
                        android.widget.TextView textView3 = new android.widget.TextView(this.ctx);
                        textView3.setText(segment.lang);
                        textView3.setTextSize(12.0f);
                        textView3.setTextColor(color2);
                        this.container.addView(textView3);
                    }
                    android.widget.TextView textView4 = new android.widget.TextView(this.ctx);
                    textView4.setText(segment.content);
                    textView4.setTextSize(13.0f);
                    textView4.setTypeface(android.graphics.Typeface.MONOSPACE);
                    textView4.setTextColor(color);
                    int dp = dp(8);
                    textView4.setPadding(dp, dp, dp, dp);
                    textView4.setBackgroundColor(color3);
                    this.container.addView(textView4);
                } else if ("heading".equals(segment.type)) {
                    android.widget.TextView textView5 = new android.widget.TextView(this.ctx);
                    textView5.setText(segment.content);
                    textView5.setTextSize(18.0f);
                    textView5.setTypeface(null, 1);
                    textView5.setTextColor(color);
                    this.container.addView(textView5);
                } else if ("bullet".equals(segment.type)) {
                    android.widget.TextView textView6 = new android.widget.TextView(this.ctx);
                    textView6.setText("· " + segment.content);
                    textView6.setTextSize(16.0f);
                    textView6.setTextColor(color);
                    textView6.setLineSpacing(dp(4), 1.0f);
                    this.container.addView(textView6);
                } else if ("quote".equals(segment.type)) {
                    android.widget.TextView textView7 = new android.widget.TextView(this.ctx);
                    textView7.setText("「" + segment.content + "」");
                    textView7.setTextSize(15.0f);
                    textView7.setTextColor(color2);
                    textView7.setLineSpacing(dp(4), 1.0f);
                    this.container.addView(textView7);
                } else {
                    android.widget.TextView textView8 = new android.widget.TextView(this.ctx);
                    textView8.setText(segment.content);
                    textView8.setTextSize(16.0f);
                    textView8.setTextColor(color);
                    textView8.setLineSpacing(dp(4), 1.0f);
                    this.container.addView(textView8);
                }
            }
        }

        private int dp(int i) {
            return java.lang.Math.round(i * this.ctx.getResources().getDisplayMetrics().density);
        }
    }
}
