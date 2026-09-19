package com.echoflow.chat;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

/**
 * 手机里的「文档」。
 *
 * 与 YIJIE 菜单里的「我的文档」的分工：
 *   · YIJIE 那个是从**助手**出发的（我让它写的东西）
 *   · 这个是从**文档**出发的（所有文档，可读、可改、可新建）
 *
 * 后者才算"文档功能" —— 一个能管理、能编辑的地方，而不只是一个产物列表。
 */
public class DocActivity extends AppCompatActivity {

    private LinearLayout listBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E0A18);

        View sb = getLayoutInflater().inflate(R.layout.view_phone_statusbar, root, false);
        root.addView(sb);
        new PhoneStatusBar(this, sb).bind();
        root.addView(topBar());

        ScrollView sv = new ScrollView(this);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(dp(16), dp(10), dp(16), dp(30));
        sv.addView(listBox);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
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
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40),
                ViewGroup.LayoutParams.MATCH_PARENT));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView t = new TextView(this);
        t.setText("文档");
        t.setTextSize(17);
        t.setTextColor(0xFF181818);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(t);

        TextView newBtn = new TextView(this);
        newBtn.setText("＋");
        newBtn.setTextSize(20);
        newBtn.setTextColor(0xFF07C160);
        newBtn.setGravity(Gravity.CENTER);
        newBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(44),
                ViewGroup.LayoutParams.MATCH_PARENT));
        newBtn.setOnClickListener(v -> newDoc());
        bar.addView(newBtn);
        return bar;
    }

    // ==================================================================

    private void reload() {
        listBox.removeAllViews();

        List<File> docs = Skill.documents(this);

        TextView intro = new TextView(this);
        intro.setText("这里存着 YIJIE 帮你写的文档，也可以自己新建。\n点开能看，能改，能导出。");
        intro.setTextSize(12);
        intro.setTextColor(0xFF8B84A8);
        intro.setLineSpacing(dp(4), 1f);
        intro.setPadding(dp(4), dp(6), dp(4), dp(16));
        listBox.addView(intro);

        if (docs.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有文档。\n\n点右上角加号新建一个，\n"
                    + "或者去 YIJIE 助手里选写文档技能让它写。");
            empty.setTextSize(13);
            empty.setTextColor(0xFF8B84A8);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(dp(5), 1f);
            empty.setPadding(dp(20), dp(60), dp(20), dp(20));
            listBox.addView(empty);
            return;
        }

        for (File f : docs) {
            listBox.addView(docRow(f));
        }
    }

    private View docRow(final File f) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);
        row.setBackground(EfUi.roundRectPx(0x14FFFFFF, 0x1FFFFFFF, dp(14)));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView icon = new TextView(this);
        icon.setText("📄");
        icon.setTextSize(22);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        icon.setGravity(Gravity.CENTER);
        row.addView(icon);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.setPadding(dp(10), 0, 0, 0);

        TextView name = new TextView(this);
        name.setText(displayName(f));
        name.setTextSize(15);
        name.setTextColor(0xFFF4F2FF);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        col.addView(name);

        TextView meta = new TextView(this);
        meta.setText(sizeLabel(f) + " · " + timeLabel(f));
        meta.setTextSize(11);
        meta.setTextColor(0xFF8B84A8);
        meta.setPadding(0, dp(3), 0, 0);
        col.addView(meta);
        row.addView(col);

        row.setOnClickListener(v -> openDoc(f));
        row.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(displayName(f))
                    .setItems(new String[]{"打开", "重命名", "删除"}, (d, w) -> {
                        if (w == 0) {
                            openDoc(f);
                        } else if (w == 1) {
                            renameDoc(f);
                        } else {
                            f.delete();
                            Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                            reload();
                        }
                    })
                    .show();
            return true;
        });
        return row;
    }

    private String displayName(File f) {
        return f.getName().replaceAll("_\\d{8}_\\d{6}\\.md$", "");
    }

    private String sizeLabel(File f) {
        long kb = f.length() / 1024;
        return kb < 1 ? f.length() + " B" : kb + " KB";
    }

    private String timeLabel(File f) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                "M月d日 HH:mm", java.util.Locale.CHINA);
        return sdf.format(new java.util.Date(f.lastModified()));
    }

    private String stamp() {
        return new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    private File docsDir() {
        File d = new File(getFilesDir(), "docs");
        if (!d.exists()) {
            d.mkdirs();
        }
        return d;
    }

    // ==================================================================

    private void newDoc() {
        final EditText input = new EditText(this);
        input.setHint("给文档起个名字");
        input.setTextColor(0xFF1A1A1A);

        new AlertDialog.Builder(this)
                .setTitle("新建文档")
                .setView(input)
                .setPositiveButton("创建", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = "未命名";
                    }
                    String safe = name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
                    File f = new File(docsDir(), safe + "_" + stamp() + ".md");
                    try (FileWriter fw = new FileWriter(f)) {
                        fw.write("# " + name + "\n\n");
                    } catch (Exception e) {
                        Toast.makeText(this, "创建失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reload();
                    openDoc(f);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void renameDoc(final File f) {
        final EditText input = new EditText(this);
        input.setText(displayName(f));
        input.setTextColor(0xFF1A1A1A);
        new AlertDialog.Builder(this)
                .setTitle("重命名")
                .setView(input)
                .setPositiveButton("改", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    String safe = name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
                    File nf = new File(docsDir(), safe + "_" + stamp() + ".md");
                    if (f.renameTo(nf)) {
                        Toast.makeText(this, "已重命名", Toast.LENGTH_SHORT).show();
                    }
                    reload();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openDoc(final File f) {
        String content;
        try {
            StringBuilder sb = new StringBuilder();
            try (FileReader fr = new FileReader(f)) {
                char[] buf = new char[4096];
                int n;
                while ((n = fr.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
            }
            content = sb.toString();
        } catch (Exception e) {
            Toast.makeText(this, "读取失败", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText editor = new EditText(this);
        editor.setText(content);
        editor.setTextSize(14);
        editor.setTextColor(0xFFEDE8FF);
        editor.setBackgroundColor(0xFF14101F);
        editor.setPadding(dp(14), dp(14), dp(14), dp(14));
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setLineSpacing(dp(3), 1f);
        editor.setMinLines(12);

        ScrollView sv = new ScrollView(this);
        sv.addView(editor);

        new AlertDialog.Builder(this)
                .setTitle(displayName(f))
                .setView(sv)
                .setPositiveButton("保存", (d, w) -> {
                    try (FileWriter fw = new FileWriter(f)) {
                        fw.write(editor.getText().toString());
                        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                    }
                    reload();
                })
                .setNeutralButton("导出", (d, w) -> exportDoc(f, editor.getText().toString()))
                .setNegativeButton("关", null)
                .show();
    }

    private void exportDoc(File f, String content) {
        try {
            File outDir = new File(getExternalFilesDir(null), "docs");
            if (!outDir.exists()) {
                outDir.mkdirs();
            }
            File out = new File(outDir, f.getName());
            try (FileWriter fw = new FileWriter(out)) {
                fw.write(content);
            }
            new AlertDialog.Builder(this)
                    .setTitle("已导出")
                    .setMessage(out.getAbsolutePath()
                            + "\n\n在 Android/data/com.echoflow.chat/files/docs 下，"
                            + "用文件管理器能打开。")
                    .setPositiveButton("好", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
