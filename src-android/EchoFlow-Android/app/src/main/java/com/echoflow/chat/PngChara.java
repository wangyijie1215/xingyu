package com.echoflow.chat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * SillyTavern PNG Character Card 解析。
 * PNG = 8字节签名 + 一连串 chunk。tEXt chunk 格式: keyword\0text
 * 角色卡: keyword="chara", text=base64(JSON)。
 */
public class PngChara {

    /**
     * 从 PNG 字节里取出角色卡 JSON（null 表示不是角色卡 PNG）。
     */
    public static JSONObject extractCharaJson(byte[] png) {
        if (png == null || png.length < 8) {
            return null;
        }
        // PNG 签名 89 50 4E 47 0D 0A 1A 0A
        for (int i = 0; i < 8; i++) {
            if (png[i] != new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}[i]) {
                return null;
            }
        }
        int pos = 8;
        try {
            while (pos + 8 <= png.length) {
                int len = readInt(png, pos);
                int type1 = png[pos + 4] & 0xFF;
                int type2 = png[pos + 5] & 0xFF;
                int type3 = png[pos + 6] & 0xFF;
                int type4 = png[pos + 7] & 0xFF;
                int dataStart = pos + 8;
                if (type1 == 't' && type2 == 'E' && type3 == 'X' && type4 == 't') {
                    // tEXt: keyword\0text
                    int end = dataStart + len;
                    int sep = -1;
                    for (int i = dataStart; i < end; i++) {
                        if (png[i] == 0) {
                            sep = i;
                            break;
                        }
                    }
                    if (sep > dataStart) {
                        String keyword = new String(png, dataStart, sep - dataStart, "ISO-8859-1");
                        if ("chara".equals(keyword)) {
                            String b64 = new String(png, sep + 1, end - sep - 1, "ISO-8859-1");
                            byte[] jsonBytes = Base64.decode(b64, Base64.DEFAULT);
                            return new JSONObject(new String(jsonBytes, "UTF-8"));
                        }
                    }
                }
                pos = dataStart + len + 4; // +CRC
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    /** 从 PNG 字节解码出头像 Bitmap */
    public static Bitmap decodeBitmap(byte[] png) {
        try {
            return BitmapFactory.decodeByteArray(png, 0, png.length);
        } catch (Exception e) {
            return null;
        }
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16)
                | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }
}
