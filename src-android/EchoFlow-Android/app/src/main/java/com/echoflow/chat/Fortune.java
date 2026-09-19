package com.echoflow.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 神社 · 每日运势（一次抽取的完整结果）。
 *
 * 存储：filesDir/fortunes/{cardId}.json —— 同一天同一角色只存在一条，
 * 抽到后立刻落库，之后一律回读，因此**刷不出来**。
 */
public class Fortune {

    public String id;
    public String cardId;
    /** yyyy-MM-dd，与 cardId 共同构成唯一键 */
    public String date;
    public int slipNo;
    /** 大吉 / 吉 / 中吉 / 小吉 / 末吉 / 凶 */
    public String rank;
    /** 综合 0~100 */
    public int score;
    public String summary;
    /** 六项运势 */
    public List<Dim> dimensions = new ArrayList<>();
    /** 幸运四件套 */
    public String luckyItem = "";
    public String luckyColorName = "";
    public String luckyColorHex = "#9B7BFF";
    public String luckyDirection = "";
    public int luckyNumber = 7;
    /** 宜 / 忌 */
    public List<String> good = new ArrayList<>();
    public List<String> bad = new ArrayList<>();
    /** 缘签：你与这位角色的缘分指数 */
    public int bondScore;
    public String bondRank = "中吉";
    /** 角色用自己口吻写的解读 */
    public String reading = "";
    public long drawnAt;

    public static class Dim {
        public String key = "";
        public int value;
        public String note = "";

        public Dim() {
        }

        public Dim(String key, int value, String note) {
            this.key = key;
            this.value = value;
            this.note = note;
        }

        public JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("key", key);
            o.put("value", value);
            o.put("note", note == null ? "" : note);
            return o;
        }

        public static Dim fromJson(JSONObject o) {
            Dim d = new Dim();
            d.key = o.optString("key", "");
            d.value = o.optInt("value", 50);
            d.note = o.optString("note", "");
            return d;
        }
    }

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("cardId", cardId);
        o.put("date", date);
        o.put("slipNo", slipNo);
        o.put("rank", rank);
        o.put("score", score);
        o.put("summary", summary);
        JSONArray dims = new JSONArray();
        for (Dim d : dimensions) {
            dims.put(d.toJson());
        }
        o.put("dimensions", dims);
        o.put("luckyItem", luckyItem);
        o.put("luckyColorName", luckyColorName);
        o.put("luckyColorHex", luckyColorHex);
        o.put("luckyDirection", luckyDirection);
        o.put("luckyNumber", luckyNumber);
        JSONArray g = new JSONArray();
        for (String s : good) {
            g.put(s);
        }
        o.put("good", g);
        JSONArray b = new JSONArray();
        for (String s : bad) {
            b.put(s);
        }
        o.put("bad", b);
        o.put("bondScore", bondScore);
        o.put("bondRank", bondRank);
        o.put("reading", reading);
        o.put("drawnAt", drawnAt);
        return o;
    }

    public static Fortune fromJson(JSONObject o) {
        Fortune f = new Fortune();
        f.id = o.optString("id", "");
        f.cardId = o.optString("cardId", "");
        f.date = o.optString("date", "");
        f.slipNo = o.optInt("slipNo", 1);
        f.rank = o.optString("rank", "中吉");
        f.score = o.optInt("score", 60);
        f.summary = o.optString("summary", "");
        JSONArray dims = o.optJSONArray("dimensions");
        if (dims != null) {
            for (int i = 0; i < dims.length(); i++) {
                JSONObject d = dims.optJSONObject(i);
                if (d != null) {
                    f.dimensions.add(Dim.fromJson(d));
                }
            }
        }
        f.luckyItem = o.optString("luckyItem", "");
        f.luckyColorName = o.optString("luckyColorName", "");
        f.luckyColorHex = o.optString("luckyColorHex", "#9B7BFF");
        f.luckyDirection = o.optString("luckyDirection", "");
        f.luckyNumber = o.optInt("luckyNumber", 7);
        JSONArray g = o.optJSONArray("good");
        if (g != null) {
            for (int i = 0; i < g.length(); i++) {
                f.good.add(g.optString(i, ""));
            }
        }
        JSONArray b = o.optJSONArray("bad");
        if (b != null) {
            for (int i = 0; i < b.length(); i++) {
                f.bad.add(b.optString(i, ""));
            }
        }
        f.bondScore = o.optInt("bondScore", 50);
        f.bondRank = o.optString("bondRank", "中吉");
        f.reading = o.optString("reading", "");
        f.drawnAt = o.optLong("drawnAt", 0);
        return f;
    }

    public String dateLabel() {
        return date == null ? "" : date.replace("-", ".");
    }

    /** 签号用汉字，和纸上的数字本来就是写的 */
    public String slipNoCn() {
        String[] cn = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        StringBuilder sb = new StringBuilder();
        String s = String.valueOf(slipNo);
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int idx = s.charAt(i) - '0';
            sb.append(idx >= 0 && idx < cn.length ? cn[idx] : s.charAt(i));
        }
        return sb.toString();
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
