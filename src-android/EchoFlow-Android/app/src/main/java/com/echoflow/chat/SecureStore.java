package com.echoflow.chat;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * API Key 安全存储：Android Keystore 中生成 AES-256 密钥，
 * GCM 模式加密后存入 SharedPreferences，密钥材料由系统保护。
 */
public class SecureStore {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "echoflow_api_key_alias";
    private static final String PREFS = "echoflow_secure";
    private static final String PREF_KEY = "api_key_enc";
    private static final int IV_LEN = 12;

    public static void save(Context ctx, String rawKey) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        SecretKey secretKey;
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            kg.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            secretKey = kg.generateKey();
        } else {
            secretKey = (SecretKey) ks.getKey(KEY_ALIAS, null);
        }

        // Android Keystore 的 GCM 不允许调用方指定 IV，必须由 Keystore 生成
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] enc = cipher.doFinal(rawKey.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();

        ByteBuffer buf = ByteBuffer.allocate(iv.length + enc.length);
        buf.put(iv).put(enc);
        String b64 = Base64.encodeToString(buf.array(), Base64.NO_WRAP);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY, b64).apply();
    }

    public static String get(Context ctx) {
        try {
            String b64 = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(PREF_KEY, null);
            if (b64 == null || b64.isEmpty()) {
                return null;
            }
            byte[] all = Base64.decode(b64, Base64.NO_WRAP);
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LEN);
            byte[] data = Arrays.copyOfRange(all, IV_LEN, all.length);

            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            SecretKey secretKey = (SecretKey) ks.getKey(KEY_ALIAS, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] dec = cipher.doFinal(data);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
