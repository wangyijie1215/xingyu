package com.echoflow.chat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 生图页 —— 让「生图"这件事真的能点。
 *
 * 用 Pollinations：免费、无需 API Key、同一 seed 出同一张图。
 * 三种方向：立绘 / 世界场景 / 她此刻的自拍。
 *
 * 缓存策略见 ImageGen：同 key 只真生成一次，之后读磁盘。
 */
public class ImageGenActivity extends AppCompatActivity {

    private CharacterCard card;
    private ImageView preview;
    private TextView status;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean working = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        card = CardStore.getCard(this, getIntent().getStringExtra("card_id"));
        android.util.Log.i("ImageGen", "card=" + (card == null ? "null" : card.name)
                + " kind=" + ImageProvider.load(this).kind);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E0A18);

        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, root, false);
        root.addView(sb);
        new PhoneStatusBar(this, sb).bind();
        root.addView(topBar());

        // 预览区：**高度自适应**，不要吃掉剩余空间。
        //
        // 原来这里用 weight=1 撑满剩余空间，结果图片下方的状态提示
        // 被挤出可视区 —— 生成失败时用户看不到任何错误，
        // 表现就是"点了没反应"。改成 wrap_content 之后，
        // 图片、状态文字、按钮会依次往下排，都能看到。
        LinearLayout previewBox = new LinearLayout(this);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setGravity(Gravity.CENTER_HORIZONTAL);
        previewBox.setPadding(dp(20), dp(16), dp(20), dp(8));
        previewBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        preview = new ImageView(this);
        // 预览框的尺寸要**跟着图片走**，不能写死。
        //
        // 踩过的两个坑：
        //   1. CENTER_CROP + 固定尺寸 → 512x768 的图被从中间裁一块，脸看不到
        //   2. 改成 FIT_CENTER 但高度仍写死 → 图被"信箱化"塞进形状不对的框里，
        //      看起来还是只有一半
        // 正解：让 ImageView 按图片宽高比自己撑开
        // （adjustViewBounds + 高度 wrap_content + 宽度 match_parent）。
        preview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        preview.setMinimumHeight(dp(200));
        preview.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x22FFFFFF, dp(16)));
        preview.setClipToOutline(true);
        previewBox.addView(preview);

        status = new TextView(this);
        status.setText("选一个方向开始生成");
        status.setTextSize(12);
        status.setTextColor(0xFF8B84A8);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, 0);
        previewBox.addView(status);
        root.addView(previewBox);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(dp(18), 0, dp(18), dp(20));
        root.addView(actions);

        setContentView(root);

        // ---- 自定义提示词 ----
        TextView promptLabel = new TextView(this);
        promptLabel.setText("提示词（留空则用下方预设）");
        promptLabel.setTextSize(12);
        promptLabel.setTextColor(0xFF8B84A8);
        promptLabel.setPadding(dp(2), 0, 0, dp(6));
        actions.addView(promptLabel);

        promptInput = new EditText(this);
        promptInput.setHint("例如：银发少女，雨夜街头，撑着透明的伞");
        promptInput.setTextSize(14);
        promptInput.setTextColor(0xFFEDE8FF);
        promptInput.setHintTextColor(0xFF5A5478);
        promptInput.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x24FFFFFF, dp(12)));
        promptInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        promptInput.setMinLines(2);
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        actions.addView(promptInput);

        actions.addView(button("用这个提示词生成", this::genCustomPrompt));
        actions.addView(button("角色立绘（用角色信息）", this::genPortrait));
        actions.addView(button("「世界」场景图", this::genScene));
        actions.addView(button("「她此刻」的自拍", this::genSelfie));

        // ---- 渠道 ----
        TextView chLabel = new TextView(this);
        chLabel.setText("生图渠道");
        chLabel.setTextSize(12);
        chLabel.setTextColor(0xFF8B84A8);
        chLabel.setPadding(dp(2), dp(10), 0, dp(6));
        actions.addView(chLabel);

        channelInfo = new TextView(this);
        channelInfo.setTextSize(12);
        channelInfo.setLineSpacing(dp(3), 1f);
        channelInfo.setPadding(dp(14), dp(12), dp(14), dp(12));
        channelInfo.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x24FFFFFF, dp(12)));
        actions.addView(channelInfo);

        actions.addView(button("更换渠道 / 检测 ComfyUI", this::openChannelPicker));
        actions.addView(button("清空生成缓存（" + ImageProvider.cacheSizeKB(this) + " KB）", () -> {
            int n = ImageProvider.clearCache(this);
            Toast.makeText(this, "已清除 " + n + " 张缓存图", Toast.LENGTH_SHORT).show();
            preview.setImageDrawable(null);
            status.setText("缓存已清空");
        }));

        updateChannelInfo();

        if (card != null) {
            showCached("portrait_" + card.id);
        }
    }

    private void updateChannelInfo() {
        if (channelInfo == null) {
            return;
        }
        ImageProvider.Config cfg = ImageProvider.load(this);
        String name;
        switch (cfg.kind) {
            case ImageProvider.KIND_COMFY:
                name = "本地 ComfyUI\n" + ProviderStore.cleanHost(cfg.comfyHost)
                        + "\n模型：" + cfg.comfyCkpt + " · " + cfg.steps + " 步";
                break;
            case ImageProvider.KIND_OPENAI:
                name = "Grok / OpenAI 兼容\n" + cfg.openaiUrl
                        + "\n模型：" + cfg.openaiModel
                        + (cfg.openaiUseSharedKey ? "\n用设置页那个 Key" : "\n用单独填的 Key");
                break;
            case ImageProvider.KIND_CUSTOM:
                name = "自定义端点\n" + (cfg.customUrl.isEmpty() ? "（还没填地址）" : cfg.customUrl);
                break;
            default:
                name = "Pollinations（免费公共服）\n无需配置，但只有一个模型，画质一般";
                break;
        }
        if (cfg.styleSuffix != null && !cfg.styleSuffix.isEmpty()) {
            name = name + "\n画风后缀：" + cfg.styleSuffix;
        }
        channelInfo.setText(name);
    }

    /** 自定义提示词输入框 */
    private EditText promptInput;
    /** 渠道信息展示 */
    private TextView channelInfo;

    /** 用用户自己写的提示词生成 */
    private void genCustomPrompt() {
        String p = promptInput == null ? "" : promptInput.getText().toString().trim();
        if (p.isEmpty()) {
            Toast.makeText(this, "先写一句提示词吧", Toast.LENGTH_SHORT).show();
            return;
        }
        // key 用提示词的哈希，所以同一个提示词永远出同一张图
        run(p, "custom_" + Math.abs(p.hashCode()), 640, 896);
    }

    /** 渠道选择 + ComfyUI 探测 */
    private void openChannelPicker() {
        final ImageProvider.Config cfg = ImageProvider.load(this);
        String[] items = {
                (ImageProvider.KIND_CLOUD.equals(cfg.kind) ? "● " : "○ ") + "Pollinations（免费，画质一般）",
                (ImageProvider.KIND_COMFY.equals(cfg.kind) ? "● " : "○ ") + "本地 ComfyUI（画质最好）",
                (ImageProvider.KIND_OPENAI.equals(cfg.kind) ? "● " : "○ ") + "Grok / OpenAI 兼容生图",
                (ImageProvider.KIND_CUSTOM.equals(cfg.kind) ? "● " : "○ ") + "自定义 HTTP 端点",
                "── 设置画风后缀",
                "── 检测 ComfyUI"
        };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("生图渠道")
                .setItems(items, (d, w) -> {
                    switch (w) {
                        case 0:
                            cfg.kind = ImageProvider.KIND_CLOUD;
                            ImageProvider.save(this, cfg);
                            updateChannelInfo();
                            Toast.makeText(this, "已切到 Pollinations", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            cfg.kind = ImageProvider.KIND_COMFY;
                            ImageProvider.save(this, cfg);
                            updateChannelInfo();
                            editComfyHost(cfg);
                            break;
                        case 2:
                            cfg.kind = ImageProvider.KIND_OPENAI;
                            ImageProvider.save(this, cfg);
                            updateChannelInfo();
                            editOpenAiImage(cfg);
                            break;
                        case 3:
                            cfg.kind = ImageProvider.KIND_CUSTOM;
                            ImageProvider.save(this, cfg);
                            updateChannelInfo();
                            editCustomUrl(cfg);
                            break;
                        case 4:
                            editStyle(cfg);
                            break;
                        case 5:
                            detectComfy(cfg);
                            break;
                    }
                })
                .show();
    }

    private void editComfyHost(ImageProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.comfyHost);
        in.setHint("192.168.1.100:8188");
        in.setTextColor(0xFF1A1A1A);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("ComfyUI 地址")
                .setMessage("模拟器填 10.0.2.2:8188\n真机填电脑的局域网 IP，例如 192.168.1.100:8188")
                .setView(in)
                .setPositiveButton("保存并检测", (d, w) -> {
                    cfg.comfyHost = in.getText().toString().trim();
                    ImageProvider.save(this, cfg);
                    updateChannelInfo();
                    detectComfy(cfg);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void editCustomUrl(ImageProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.customUrl);
        in.setHint("https://your-api/gen?prompt={prompt}&w={width}&h={height}");
        in.setTextColor(0xFF1A1A1A);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("自定义端点")
                .setMessage("任何「给提示词返回图片」的 HTTP 服务都可以。\n"
                        + "可用占位符：{prompt} {width} {height}")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.customUrl = in.getText().toString().trim();
                    ImageProvider.save(this, cfg);
                    updateChannelInfo();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 配 Grok / OpenAI 兼容生图 */
    private void editOpenAiImage(final ImageProvider.Config cfg) {
        final android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));

        box.addView(hintText("接口地址（会拼上 /v1/images/generations）"));
        final EditText urlIn = input(cfg.openaiUrl, "https://aibridgea.com");
        box.addView(urlIn);

        box.addView(hintText("模型名（不确定就点下面的「拉取模型列表」）"));
        final EditText modelIn = input(cfg.openaiModel, "grok-imagine-image-2.0");
        box.addView(modelIn);

        box.addView(hintText("尺寸（有的中转只认 1024x1024）"));
        final EditText sizeIn = input(cfg.openaiSize, "1024x1024");
        box.addView(sizeIn);

        box.addView(hintText("API Key 用设置页里那个（对话和生图共用一个）"));
        final android.widget.CheckBox shared =
                new android.widget.CheckBox(this);
        shared.setText("用设置页的 Key");
        shared.setTextColor(0xFFC3BEDB);
        shared.setChecked(cfg.openaiUseSharedKey);
        box.addView(shared);

        final EditText keyIn = input(cfg.openaiKey, "sk-...（不共用时填这里）");
        keyIn.setEnabled(!cfg.openaiUseSharedKey);
        shared.setOnCheckedChangeListener((b, checked) -> keyIn.setEnabled(!checked));

        // 拉取模型列表 —— 模型名手填容易错，而且中转站的模型说变就变
        final TextView listBtn = new TextView(this);
        listBtn.setText("⟳ 拉取模型列表");
        listBtn.setTextSize(13);
        listBtn.setTextColor(0xFF9B7BFF);
        listBtn.setPadding(0, dp(6), 0, dp(6));
        listBtn.setGravity(Gravity.CENTER);
        listBtn.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(10)));
        box.addView(listBtn);

        final TextView listStatus = hunt("");
        box.addView(listStatus);

        listBtn.setOnClickListener(v -> {
            listStatus.setText("正在拉取…");
            listStatus.setTextColor(0xFFE8C87A);
            // 先用当前填的值试，避免用户改完地址还没保存
            final ImageProvider.Config probe = new ImageProvider.Config();
            probe.openaiUrl = urlIn.getText().toString().trim();
            probe.openaiUseSharedKey = shared.isChecked();
            probe.openaiKey = keyIn.getText().toString().trim();
            new Thread(() -> {
                final ImageProvider.ModelList ml = ImageProvider.listOpenAiModels(this, probe);
                runOnUiThread(() -> {
                    if (!ml.ok) {
                        listStatus.setText("✗ " + ml.message);
                        listStatus.setTextColor(0xFFFF9BB0);
                        return;
                    }
                    listStatus.setText("✓ " + ml.message);
                    listStatus.setTextColor(0xFF7FE0C4);
                    // 优先列画图类；没有就列全部
                    final java.util.List<String> show = ml.imageModels.isEmpty()
                            ? ml.allModels : ml.imageModels;
                    if (show.isEmpty()) {
                        return;
                    }
                    String title = ml.imageModels.isEmpty()
                            ? "全部模型（没识别出画图类）" : "可能是画图模型";
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(title)
                            .setItems(show.toArray(new String[0]), (dd, ww) ->
                                    modelIn.setText(show.get(ww)))
                            .show();
                });
            }).start();
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Grok / OpenAI 兼容生图")
                .setView(box)
                .setPositiveButton("保存并试一张", (d, w) -> {
                    cfg.openaiUrl = urlIn.getText().toString().trim();
                    cfg.openaiModel = modelIn.getText().toString().trim();
                    cfg.openaiSize = sizeIn.getText().toString().trim();
                    cfg.openaiUseSharedKey = shared.isChecked();
                    cfg.openaiKey = keyIn.getText().toString().trim();
                    ImageProvider.save(this, cfg);
                    updateChannelInfo();
                    genCustomPrompt();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private EditText input(String value, String hint) {
        EditText e = new EditText(this);
        e.setText(value == null ? "" : value);
        e.setHint(hint);
        e.setTextColor(0xFFEDE8FF);
        e.setHintTextColor(0xFF5A5478);
        e.setTextSize(14);
        return e;
    }

    private TextView hunt(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(12);
        tv.setTextColor(0xFF8B84A8);
        tv.setLineSpacing(dp(3), 1f);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView hintText(String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextSize(11);
        tv.setTextColor(0xFF8B84A8);
        tv.setPadding(0, dp(10), 0, dp(2));
        return tv;
    }

    private void editStyle(ImageProvider.Config cfg) {
        final EditText in = new EditText(this);
        in.setText(cfg.styleSuffix);
        in.setHint("masterpiece, best quality, anime style");
        in.setTextColor(0xFF1A1A1A);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("画风后缀")
                .setMessage("这段会被追加到每个提示词后面，用来统一画风。\n留空则用默认。")
                .setView(in)
                .setPositiveButton("保存", (d, w) -> {
                    cfg.styleSuffix = in.getText().toString().trim();
                    ImageProvider.save(this, cfg);
                    updateChannelInfo();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void detectComfy(ImageProvider.Config cfg) {
        status.setText("正在检测 ComfyUI…");
        status.setTextColor(0xFFE8C87A);
        new Thread(() -> {
            ImageProvider.Probe p = ImageProvider.probeComfy(cfg.comfyHost);
            ui.post(() -> {
                if (p.ok) {
                    cfg.comfyHost = p.host;
                    if (!p.checkpoints.isEmpty()) {
                        cfg.comfyCkpt = p.checkpoints.get(0);
                    }
                    ImageProvider.save(this, cfg);
                    updateChannelInfo();
                    status.setText("✓ " + p.message + "\n用 " + cfg.comfyCkpt);
                    status.setTextColor(0xFF7FE0C4);
                } else {
                    status.setText("✗ " + p.message);
                    status.setTextColor(0xFFFF9BB0);
                }
            });
        }).start();
    }
    private View topBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundResource(R.drawable.wx_topbar);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        bar.setPadding(dp(12), 0, dp(12), 0);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(26);
        back.setTextColor(0xFF181818);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView t = new TextView(this);
        t.setText("生图");
        t.setTextSize(17);
        t.setTextColor(0xFF181818);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(t);

        TextView info = new TextView(this);
        info.setText("说明");
        info.setTextSize(13);
        info.setTextColor(0xFF07C160);
        info.setGravity(Gravity.CENTER);
        info.setLayoutParams(new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT));
        info.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("关于生图")
                                        .setMessage("use Pollinations; no API key needed")
                        .setPositiveButton("OK", null)
                        .show());
        bar.addView(info);
        return bar;
    }

    // ==================================================================
    // 生成
    // ==================================================================

    private void genPortrait() {
        if (card == null) {
            Toast.makeText(this, "没有角色信息，无法生成立绘", Toast.LENGTH_SHORT).show();
            status.setText("没有角色信息。请从「手机 → 生图」进入，或先创建角色。");
            status.setTextColor(0xFFFF9BB0);
            return;
        }
        // 关键：只喂**外观**相关的词，不要把性格描写塞进去。
        // 性格是"她是什么样的人"，对画面没有指导意义，
        // 反而会淹没构图（实测：塞进去之后只出下半身，脸都被裁掉了）。
        String look = appearOf(card);
        String prompt = STYLE_PRE
                + "1girl, solo, full body, standing, "
                + look
                + ", looking at viewer, gentle expression"
                + STYLE_POST;
        run(prompt, "portrait_" + card.id, 512, 768);
    }

    /**
     * 从角色卡里抽出**外观**描述。
     *
     * 角色卡的 description 通常写成"银白色长发，琥珀色的眼睛，……"，
     * 这部分正好可以直接当提示词用。做法是取前几句，
     * 并在遇到明显是讲性格/背景的词时截断。
     */
    private String appearOf(CharacterCard card) {
        StringBuilder sb = new StringBuilder();

        // 1) 标签里常有 #少女骑士 #银发 这类，很有用
        if (card.tags != null) {
            for (int i = 0; i < Math.min(3, card.tags.size()); i++) {
                String t = card.tags.get(i).trim();
                if (!t.isEmpty()) {
                    sb.append(t).append(", ");
                }
            }
        }

        // 2) description 的前 60 字，通常是外貌
        if (card.description != null && !card.description.isEmpty()) {
            String d = card.description.replaceAll("[\\r\\n\\t]", " ").trim();
            if (d.length() > 60) {
                d = d.substring(0, 60);
            }
            sb.append(d);
        }

        String s = sb.toString().trim();
        // 兜底：什么都没有就给一个通用描述
        if (s.isEmpty()) {
            s = "beautiful girl, detailed face";
        }
        return s;
    }

    private void genScene() {
        String cid = card == null ? null : card.id;
        Lorebook world = Lorebook.currentWorld(this, cid);
        String wname = world == null ? "modern city" : safe(world.name);
        String desc = world == null ? "" : safe(world.description);
        String prompt = STYLE_PRE + "scenery of " + wname + ", " + desc
                + ", no people, background art" + STYLE_POST;
        run(prompt, "scene_" + (world == null ? "none" : world.id), 768, 512);
    }

    private void genSelfie() {
        if (card == null) {
            Toast.makeText(this, "没有角色信息", Toast.LENGTH_SHORT).show();
            status.setText("没有角色信息。请从「手机 → 生图」进入，或先创建角色。");
            status.setTextColor(0xFFFF9BB0);
            return;
        }
        Schedule.Now now = Schedule.now(Schedule.load(this, card.id));
        EmotionState emo = CharacterStateStore.loadEmotion(this, card.id);
        Lorebook world = Lorebook.currentWorld(this, card.id);
        // 同样只喂外观 + 当下情境，不喂性格
        String prompt = STYLE_PRE
                + "1girl, solo, close-up, selfie, "
                + appearOf(card) + ", "
                + (emo == null || emo.mood == null ? "" : emo.mood + " mood, ")
                + ((now == null || now.what == null) ? "" : now.what + ", ")
                + (world == null ? "" : "in " + world.name.replaceAll("[\\r\\n\\t]", "") + ", ")
                + "casual clothes"
                + STYLE_POST;
        // 自拍用「此刻」做 key 的一部分，所以状态变了图也会换
        String key = "selfie_" + card.id + "_" + Fortune.today()
                + "_" + (now == null ? "" : now.what);
        run(prompt, key, 512, 768);
    }

    private void run(String prompt, String key, int w, int h) {
        if (working) {
            Toast.makeText(this, "正在生成，稍等一下", Toast.LENGTH_SHORT).show();
            return;
        }
        working = true;

        final ImageProvider.Config cfg = ImageProvider.load(this);
        String hint;
        switch (cfg.kind) {
            case ImageProvider.KIND_COMFY:
                hint = "正在用本地 ComfyUI 生成…\n（显卡不同，通常 20~60 秒）";
                break;
            case ImageProvider.KIND_CUSTOM:
                hint = "正在通过自定义端点生成…";
                break;
            default:
                hint = "正在用 Pollinations 生成…\n（免费公共服务，首次可能要 10~40 秒）";
                break;
        }
        status.setText(hint);
        status.setTextColor(0xFFE8C87A);

        // 缓存优先：同 key 只真生成一次
        final java.io.File cache = ImageProvider.cacheFile(this, key);
        if (cache.exists()) {
            Bitmap cached = android.graphics.BitmapFactory.decodeFile(cache.getAbsolutePath());
            if (cached != null) {
                working = false;
                preview.setImageBitmap(cached);
                status.setText("这是上次生成的（已缓存）");
                status.setTextColor(0xFF8B84A8);
                return;
            }
            cache.delete();
        }

        // 拼画风后缀
        final String fullPrompt = (cfg.styleSuffix != null && !cfg.styleSuffix.isEmpty())
                ? prompt + ", " + cfg.styleSuffix
                : prompt;
        final int seed = Math.abs(key.hashCode()) % 1000000;

        new Thread(() -> {
            try {
                android.util.Log.i("ImageGen", "start gen kind=" + cfg.kind
                        + " host=" + cfg.comfyHost + " prompt=" + fullPrompt);
                Bitmap bmp = ImageProvider.generate(this, cfg, fullPrompt, STYLE_NEG, seed, w, h);
                android.util.Log.i("ImageGen", "gen ok " + bmp.getWidth() + "x" + bmp.getHeight());
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cache)) {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                }
                ui.post(() -> {
                    working = false;
                    preview.setImageBitmap(bmp);
                    status.setText("完成 · 已缓存，下次秒开");
                    status.setTextColor(0xFF7FE0C4);
                });
            } catch (Exception e) {
                String m = e.getMessage() == null ? "生成失败" : e.getMessage();
                android.util.Log.e("ImageGen", "gen failed: " + m, e);
                ui.post(() -> {
                    working = false;
                    status.setText(m);
                    status.setTextColor(0xFFFF9BB0);
                });
            }
        }).start();
    }

    private void showCached(String key) {
        java.io.File f = ImageProvider.cacheFile(this, key);
        if (!f.exists()) {
            return;
        }
        Bitmap b = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
        if (b != null) {
            preview.setImageBitmap(b);
            status.setText("这是上次生成的（已缓存）");
            status.setTextColor(0xFF8B84A8);
        }
    }

    /** 把中文角色名/描述转成提示词里安全的形式（去掉会导致 URL 过长的内容） */
    /**
     * 统一画风。
     * 第一次实测出来是写实风（真人照片质感），和角色的二次元设定不搭 ——
     * 所以把风格词写死在前后缀里，不让它跑偏。
     */
    /**
     * 画风前缀。
     *
     * `score_9, score_8_up, score_7_up` 是 **Pony Diffusion V6 的质量标记**，
     * 不是通用修饰词。Pony 系模型靠这三个标记选"好图档位"，
     * 只写 masterpiece/best quality 它基本无感 ——
     * 实测不写会出构图崩坏、头被裁掉的图，写上是明显不同的一档。
     *
     * 其他模型（Pollinations 的 sana）会忽略不认识的词，无害。
     */
    private static final String STYLE_PRE =
            "score_9, score_8_up, score_7_up, "
            + "anime style, 2D illustration, cel shading, key visual, ";

    private static final String STYLE_POST =
            ", masterpiece, best quality, highly detailed, vibrant colors, "
            + "japanese animation art, trending on pixiv";

    /** 负面提示词：把"裁掉头""出画"这类构图错误明确排除 */
    private static final String STYLE_NEG =
            "score_4, score_5, score_6, worst quality, low quality, "
            + "blurry, jpeg artifacts, watermark, signature, text, "
            + "bad anatomy, bad hands, extra fingers, deformed, "
            + "cropped head, head out of frame, out of frame";

    private String safe(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("[\\r\\n\\t]", " ").trim();
        if (t.length() > 80) {
            t = t.substring(0, 80);
        }
        return t;
    }

    private View button(String label, Runnable action) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTextColor(0xFFFFFFFF);
        b.setGravity(Gravity.CENTER);
        b.setBackground(EfUi.roundRectPx(0x339B7BFF, 0x669B7BFF, dp(12)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
