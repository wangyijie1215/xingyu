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
 * 动态本地存储：filesDir/posts.json + filesDir/posts/ 图片。
 */
public class PostStore {

    private static File storeFile(Context ctx) {
        return new File(ctx.getFilesDir(), "posts.json");
    }

    public static List<Post> listPosts(Context ctx) {
        List<Post> out = new ArrayList<>();
        File f = storeFile(ctx);
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
        } catch (Exception e) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                out.add(Post.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignore) {
        }
        // 新的在前
        Collections.sort(out, (a, b) -> Long.compare(b.time, a.time));
        return out;
    }

    public static void savePost(Context ctx, Post p) throws Exception {
        List<Post> all = listPosts(ctx);
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(p.id)) {
                all.set(i, p);
                found = true;
                break;
            }
        }
        if (!found) {
            all.add(p);
        }
        writeAll(ctx, all);
    }

    public static void deletePost(Context ctx, String id) {
        List<Post> all = listPosts(ctx);
        Post target = null;
        for (Post p : all) {
            if (p.id.equals(id)) {
                target = p;
                break;
            }
        }
        if (target != null) {
            all.remove(target);
            try {
                writeAll(ctx, all);
            } catch (Exception ignore) {
            }
        }
    }

    public static void like(Context ctx, String id) {
        List<Post> all = listPosts(ctx);
        for (Post p : all) {
            if (p.id.equals(id)) {
                p.likes++;
                try {
                    writeAll(ctx, all);
                } catch (Exception ignore) {
                }
                return;
            }
        }
    }

    private static void writeAll(Context ctx, List<Post> all) throws Exception {
        JSONArray arr = new JSONArray();
        for (Post p : all) {
            arr.put(p.toJson());
        }
        try (FileWriter fw = new FileWriter(storeFile(ctx))) {
            fw.write(arr.toString());
        }
    }
}
