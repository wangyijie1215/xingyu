package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色库：角色列表 + 新建 + 导入（JSON/PNG）+ 长按菜单。
 */
public class CardListActivity extends AppCompatActivity {

    private static final int REQ_IMPORT = 1001;
    private static final int REQ_AVATAR = 1002;

    private RecyclerView list;
    private CardAdapter adapter;
    private final List<CharacterCard> cards = new ArrayList<>();
    private String editingCardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        list = findViewById(R.id.card_list);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new CardAdapter();
        list.setAdapter(adapter);

        MaterialButton btnNew = findViewById(R.id.btn_new_card);
        btnNew.setOnClickListener(v -> {
            editingCardId = null;
            startActivity(new Intent(this, CardEditActivity.class));
        });

        MaterialButton btnImport = findViewById(R.id.btn_import);
        btnImport.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mime = {"image/png", "application/json", "application/octet-stream"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mime);
            try {
                startActivityForResult(Intent.createChooser(i, "导入角色卡"), REQ_IMPORT);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cards.clear();
        cards.addAll(CardStore.listCards(this));
        adapter.notifyDataSetChanged();
        findViewById(R.id.empty_hint).setVisibility(
                cards.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQ_IMPORT) {
            importFromUri(uri);
        }
    }

    private void importFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            byte[] bytes = bos.toByteArray();

            // 先试 PNG 角色卡
            org.json.JSONObject json = PngChara.extractCharaJson(bytes);
            Bitmap avatar = null;
            if (json != null) {
                avatar = PngChara.decodeBitmap(bytes);
            } else {
                // 当 JSON 解析
                try {
                    String s = new String(bytes, "UTF-8");
                    json = new org.json.JSONObject(s);
                } catch (Exception ignore) {
                }
            }
            if (json == null) {
                Toast.makeText(this, "不是有效的角色卡文件", Toast.LENGTH_LONG).show();
                return;
            }
            CharacterCard card = CharacterCard.fromImportJson(json);
            CardStore.saveCard(this, card);
            if (avatar != null) {
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                avatar.compress(Bitmap.CompressFormat.PNG, 100, pngOut);
                CardStore.saveAvatar(this, card.id, pngOut.toByteArray());
            }
            Toast.makeText(this, "已导入：" + card.name, Toast.LENGTH_SHORT).show();
            onResume();
        } catch (Exception e) {
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CharacterCard c = cards.get(position);
            h.name.setText(c.name.isEmpty() ? "未命名" : c.name);
            String desc = c.description;
            if (desc != null && desc.length() > 80) {
                desc = desc.substring(0, 80) + "…";
            }
            h.desc.setText(desc == null ? "" : desc);
            h.tags.setText(c.tags.isEmpty() ? "" : "#" + String.join("  #", c.tags));

            Bitmap bmp = CardStore.loadAvatar(CardListActivity.this, c.id);
            if (bmp != null) {
                h.avatar.setImageBitmap(bmp);
                h.avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                h.avatar.setImageResource(R.drawable.app_icon);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(CardListActivity.this, CharacterProfileActivity.class);
                i.putExtra("card_id", c.id);
                startActivity(i);
            });
            h.itemView.setOnLongClickListener(v -> {
                showCardMenu(c, v);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView name;
            TextView desc;
            TextView tags;

            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.avatar);
                name = itemView.findViewById(R.id.card_name);
                desc = itemView.findViewById(R.id.card_desc);
                tags = itemView.findViewById(R.id.card_tags);
            }
        }
    }

    private void showCardMenu(CharacterCard card, View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "编辑");
        pm.getMenu().add(0, 2, 1, "导出 JSON");
        pm.getMenu().add(0, 3, 2, "复制");
        pm.getMenu().add(0, 4, 3, "删除");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                Intent i = new Intent(this, CardEditActivity.class);
                i.putExtra("card_id", card.id);
                startActivity(i);
            } else if (id == 2) {
                // 导出 JSON 到 Download
                byte[] data = CardStore.exportCardJson(card);
                try {
                    java.io.File out = new java.io.File(
                            android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOWNLOADS),
                            "echoflow_" + card.name + ".json");
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                        fos.write(data);
                    }
                    Toast.makeText(this, "已导出到 Download: " + out.getName(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else if (id == 3) {
                CharacterCard copy = new CharacterCard();
                copy.name = card.name + " (副本)";
                copy.description = card.description;
                copy.personality = card.personality;
                copy.scenario = card.scenario;
                copy.firstMes = card.firstMes;
                copy.alternateGreetings = new ArrayList<>(card.alternateGreetings);
                copy.mesExample = card.mesExample;
                copy.systemPrompt = card.systemPrompt;
                copy.postHistoryInstructions = card.postHistoryInstructions;
                copy.tags = new ArrayList<>(card.tags);
                copy.creator = card.creator;
                copy.characterVersion = card.characterVersion;
                try {
                    CardStore.saveCard(this, copy);
                } catch (Exception ex) {
                    Toast.makeText(this, "复制失败: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                onResume();
            } else if (id == 4) {
                CardStore.deleteCard(this, card.id);
                onResume();
            }
            return true;
        });
        pm.show();
    }
}
