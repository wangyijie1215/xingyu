package com.echoflow.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 角色卡编辑器。
 */
public class CardEditActivity extends AppCompatActivity {

    private ImageView avatarPreview;
    private EditText inputName, inputDesc, inputPersonality, inputScenario,
            inputFirst, inputExample, inputSystem, inputPost, inputTags,
            inputCreator, inputVersion, inputModel;
    private String cardId;
    private byte[] pendingAvatar;

    private final ActivityResultLauncher<String> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                    if (bmp != null) {
                        avatarPreview.setImageBitmap(bmp);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                        pendingAvatar = bos.toByteArray();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "头像读取失败", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_edit);

        avatarPreview = findViewById(R.id.avatar_preview);
        inputName = findViewById(R.id.input_name);
        inputDesc = findViewById(R.id.input_description);
        inputPersonality = findViewById(R.id.input_personality);
        inputScenario = findViewById(R.id.input_scenario);
        inputFirst = findViewById(R.id.input_first_mes);
        inputExample = findViewById(R.id.input_mes_example);
        inputSystem = findViewById(R.id.input_system_prompt);
        inputPost = findViewById(R.id.input_post_history);
        inputTags = findViewById(R.id.input_tags);
        inputCreator = findViewById(R.id.input_creator);
        inputVersion = findViewById(R.id.input_version);
        inputModel = findViewById(R.id.input_model);

        Button btnPick = findViewById(R.id.btn_pick_avatar);
        btnPick.setOnClickListener(v -> pickAvatar.launch("image/*"));

        cardId = getIntent().getStringExtra("card_id");
        CharacterCard card = null;
        if (cardId != null) {
            card = CardStore.getCard(this, cardId);
        }
        if (card != null) {
            inputName.setText(card.name);
            inputDesc.setText(card.description);
            inputPersonality.setText(card.personality);
            inputScenario.setText(card.scenario);
            inputFirst.setText(card.firstMes);
            inputExample.setText(card.mesExample);
            inputSystem.setText(card.systemPrompt);
            inputPost.setText(card.postHistoryInstructions);
            inputTags.setText(String.join(", ", card.tags));
            inputCreator.setText(card.creator);
            inputVersion.setText(card.characterVersion);
            inputModel.setText(card.model);
            Bitmap bmp = CardStore.loadAvatar(this, card.id);
            if (bmp != null) {
                avatarPreview.setImageBitmap(bmp);
            }
        }

        Button btnSave = findViewById(R.id.btn_save_card);
        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "角色名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        CharacterCard card;
        if (cardId != null) {
            card = CardStore.getCard(this, cardId);
            if (card == null) {
                card = new CharacterCard();
            }
        } else {
            card = new CharacterCard();
        }
        card.name = name;
        card.description = inputDesc.getText().toString();
        card.personality = inputPersonality.getText().toString();
        card.scenario = inputScenario.getText().toString();
        card.firstMes = inputFirst.getText().toString();
        card.mesExample = inputExample.getText().toString();
        card.systemPrompt = inputSystem.getText().toString();
        card.postHistoryInstructions = inputPost.getText().toString();
        card.creator = inputCreator.getText().toString().trim();
        card.characterVersion = inputVersion.getText().toString().trim();
        card.model = inputModel.getText().toString().trim();

        String tagsStr = inputTags.getText().toString().trim();
        card.tags.clear();
        if (!tagsStr.isEmpty()) {
            String[] parts = tagsStr.split("[,，]");
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) {
                    card.tags.add(t);
                }
            }
        }

        try {
            CardStore.saveCard(this, card);
            if (pendingAvatar != null) {
                CardStore.saveAvatar(this, card.id, pendingAvatar);
            }
            Toast.makeText(this, "已保存：" + card.name, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
