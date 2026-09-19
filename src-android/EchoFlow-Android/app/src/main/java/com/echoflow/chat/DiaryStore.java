package com.echoflow.chat;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 角色日记存储。filesDir/diaries/{cardId}.json
 *
 * 同时提供「心情曲线"的聚合计算：把情绪历史按天归并成柱状数据，
 * 值 = 该天主导情绪的积极度 × 100，颜色由主导情绪决定。
 */
public class DiaryStore {

    private static File diaryFile(Context ctx, String cardId) {
        File dir = new File(ctx.getFilesDir(), "diaries");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, cardId + ".json");
    }

    public static List<DiaryEntry> list(Context ctx, String cardId) {
        List<DiaryEntry> out = new ArrayList<>();
        File f = diaryFile(ctx, cardId);
        if (!f.exists()) {
            return out;
        }
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(f)) {
            char[] buf = new char[4096];
            int n;
            while ((n = fr.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    out.add(DiaryEntry.fromJson(o));
                }
            }
        } catch (Exception ignore) {
        }
        // 新的在前
        Collections.sort(out, (a, b) -> {
            if (a.date == null || b.date == null) {
                return 0;
            }
            return b.date.compareTo(a.date);
        });
        return out;
    }

    /** 取某天的日记，不存在返回 null */
    public static DiaryEntry get(Context ctx, String cardId, String date) {
        for (DiaryEntry d : list(ctx, cardId)) {
            if (date.equals(d.date)) {
                return d;
            }
        }
        return null;
    }

    public static DiaryEntry today(Context ctx, String cardId) {
        return get(ctx, cardId, Fortune.today());
    }

    /** 同一角色同一天只保留一篇 */
    public static void save(Context ctx, String cardId, DiaryEntry entry) {
        List<DiaryEntry> all = list(ctx, cardId);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).date.equals(entry.date)) {
                all.set(i, entry);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(entry);
        }
        // 只保留最近 180 篇
        if (all.size() > 180) {
            Collections.sort(all, (a, b) -> a.date.compareTo(b.date));
            all = new ArrayList<>(all.subList(all.size() - 180, all.size()));
        }
        JSONArray arr = new JSONArray();
        for (DiaryEntry d : all) {
            try {
                arr.put(d.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(diaryFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    public static void delete(Context ctx, String cardId, String date) {
        List<DiaryEntry> all = list(ctx, cardId);
        all.removeIf(d -> d.date.equals(date));
        JSONArray arr = new JSONArray();
        for (DiaryEntry d : all) {
            try {
                arr.put(d.toJson());
            } catch (Exception ignore) {
            }
        }
        try (FileWriter fw = new FileWriter(diaryFile(ctx, cardId))) {
            fw.write(arr.toString());
        } catch (Exception ignore) {
        }
    }

    // ==================================================================
    // 心情曲线
    // ==================================================================

    /** 情绪 → 积极度（0~1）。情绪不是随机值，这里只是把它投影成可画的高度。 */
    public static double positivity(String mood) {
        if (mood == null) {
            return 0.55;
        }
        switch (mood) {
            case "开心": return 1.00;
            case "期待": return 0.88;
            case "害羞": return 0.78;
            case "惊讶": return 0.62;
            case "平静": return 0.55;
            case "紧张": return 0.38;
            case "疲惫": return 0.30;
            case "孤独": return 0.26;
            case "生气": return 0.22;
            case "悲伤": return 0.20;
            default: return 0.55;
        }
    }

    /** 情绪 → 主题色（与设计系统的语义色一致） */
    public static int moodColor(String mood) {
        if (mood == null) {
            return 0xFF8B84A8;
        }
        switch (mood) {
            case "开心": case "期待": return 0xFFFFA657;      // 羁绊橙
            case "害羞": return 0xFFFF9BB0;                   // 情绪玫瑰
            case "平静": case "惊讶": return 0xFF7FE0C4;      // 记忆薄荷
            case "紧张": return 0xFF79D3F5;                   // 语音天青
            default: return 0xFF8B84A8;                       // 疲惫/孤独/悲伤/生气
        }
    }

    public static class MoodPoint {
        public String date;     // M/d
        public String mood;
        public int value;       // 0 ~ 100
    }

    /**
     * 把日记按天聚合成心情曲线。
     *
     * 数据来源优先级：当天日记的情绪快照 > 当天无记录则跳过。
     * 诚实做法：没有记录就不编造 —— 曲线只画真实存在的那些天。
     */
    public static List<MoodPoint> moodCurve(List<DiaryEntry> diaries, int days) {
        List<MoodPoint> out = new ArrayList<>();
        if (diaries == null || diaries.isEmpty()) {
            return out;
        }
        // 先建日期索引
        java.util.Map<String, DiaryEntry> byDate = new java.util.HashMap<>();
        for (DiaryEntry d : diaries) {
            if (d.date != null && !d.date.isEmpty()) {
                byDate.put(d.date, d);
            }
        }
        String today = Fortune.today();
        for (int i = days - 1; i >= 0; i--) {
            String key = FortuneService.shiftDate(today, -i);
            DiaryEntry d = byDate.get(key);
            if (d == null) {
                continue;
            }
            MoodPoint p = new MoodPoint();
            p.date = key.length() >= 10 ? key.substring(5).replace('-', '/') : key;
            p.mood = d.emotion;
            p.value = (int) Math.round(positivity(d.emotion) * 100);
            out.add(p);
        }
        return out;
    }

    /** 曲线里出现过的情绪种类，用于图例 */
    public static List<String> moodsIn(List<MoodPoint> points) {
        List<String> out = new ArrayList<>();
        for (MoodPoint p : points) {
            if (!out.contains(p.mood)) {
                out.add(p.mood);
            }
        }
        return out;
    }
}