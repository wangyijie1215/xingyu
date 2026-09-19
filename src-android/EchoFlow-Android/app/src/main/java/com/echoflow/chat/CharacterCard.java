package com.echoflow.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Character Card V2 数据模型（兼容 SillyTavern / Tavern Card V1/V2）。
 */
public class CharacterCard {

    public String id = "";
    public String name = "";
    public String description = "";
    public String personality = "";
    public String scenario = "";
    public String firstMes = "";
    public List<String> alternateGreetings = new ArrayList<>();
    public String mesExample = "";
    public String systemPrompt = "";
    public String postHistoryInstructions = "";
    public List<String> tags = new ArrayList<>();
    public String creator = "";
    public String characterVersion = "1.0";
    public String note = "";
    // 角色级覆盖（空则用全局）
    public String model = "";
    public String baseUrl = "";
    public long updatedAt = 0;

    /** 序列化为 V2 导出格式 */
    public JSONObject toV2Json() throws Exception {
        JSONObject data = new JSONObject();
        data.put("name", name);
        data.put("description", description);
        data.put("personality", personality);
        data.put("scenario", scenario);
        data.put("first_mes", firstMes);
        JSONArray alt = new JSONArray();
        for (String g : alternateGreetings) {
            alt.put(g);
        }
        data.put("alternate_greetings", alt);
        data.put("mes_example", mesExample);
        data.put("system_prompt", systemPrompt);
        data.put("post_history_instructions", postHistoryInstructions);
        JSONArray tg = new JSONArray();
        for (String t : tags) {
            tg.put(t);
        }
        data.put("tags", tg);
        data.put("creator", creator);
        data.put("character_version", characterVersion);
        data.put("creator_notes", note);

        JSONObject root = new JSONObject();
        root.put("spec", "chara_card_v2");
        root.put("spec_version", "2.0");
        root.put("data", data);
        return root;
    }

    /** 从导入的 JSON（V1 或 V2）构建 CharacterCard */
    public static CharacterCard fromImportJson(JSONObject root) {
        CharacterCard c = new CharacterCard();
        JSONObject data = root;
        // V2: 有 data 字段
        if (root.has("data") && root.optJSONObject("data") != null) {
            data = root.optJSONObject("data");
        }
        c.name = data.optString("name", "未命名");
        c.description = data.optString("description", "");
        c.personality = data.optString("personality", "");
        c.scenario = data.optString("scenario", "");
        c.firstMes = data.optString("first_mes", data.optString("first_message", ""));
        c.mesExample = data.optString("mes_example", data.optString("example_dialogue", ""));
        c.systemPrompt = data.optString("system_prompt", "");
        c.postHistoryInstructions = data.optString("post_history_instructions", "");
        c.creator = data.optString("creator", "");
        c.characterVersion = data.optString("character_version", "1.0");
        c.note = data.optString("creator_notes", data.optString("notes", ""));
        c.id = "c" + System.currentTimeMillis();
        c.updatedAt = System.currentTimeMillis();

        JSONArray alt = data.optJSONArray("alternate_greetings");
        if (alt != null) {
            for (int i = 0; i < alt.length(); i++) {
                c.alternateGreetings.add(alt.optString(i, ""));
            }
        }
        JSONArray tg = data.optJSONArray("tags");
        if (tg != null) {
            for (int i = 0; i < tg.length(); i++) {
                c.tags.add(tg.optString(i, ""));
            }
        }
        return c;
    }

    /** 本地持久化格式（额外字段 id/model/baseUrl） */
    public JSONObject toLocalJson() throws Exception {
        JSONObject o = toV2Json();
        o.put("_id", id);
        o.put("_model", model);
        o.put("_baseUrl", baseUrl);
        o.put("_updatedAt", updatedAt);
        return o;
    }

    public static CharacterCard fromLocalJson(JSONObject o) {
        CharacterCard c = fromImportJson(o);
        c.id = o.optString("_id", c.id);
        c.model = o.optString("_model", "");
        c.baseUrl = o.optString("_baseUrl", "");
        c.updatedAt = o.optLong("_updatedAt", System.currentTimeMillis());
        return c;
    }
}
