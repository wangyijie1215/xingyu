package com.echoflow.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户 Persona（用户自己的角色设定）。
 */
public class Persona {
    public String id = "";
    public String name = "";
    public String description = "";

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("description", description);
        return o;
    }

    public static Persona fromJson(JSONObject o) {
        Persona p = new Persona();
        p.id = o.optString("id", "");
        p.name = o.optString("name", "");
        p.description = o.optString("description", "");
        return p;
    }

    public static List<Persona> listFromJson(String json) {
        List<Persona> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(fromJson(arr.optJSONObject(i)));
            }
        } catch (Exception ignore) {
        }
        return list;
    }

    public static String listToJson(List<Persona> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Persona p : list) {
                arr.put(p.toJson());
            }
        } catch (Exception ignore) {
        }
        return arr.toString();
    }
}
