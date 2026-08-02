package com.sccothe.fridgeclear.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmCrypto {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCrypto(@Value("${fridgeclear.ai.encryption-key:}") String encodedKey) {
        try {
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("fridgeclear.ai.encryption-key 必须是 Base64 编码的 32 字节密钥", exception);
        }
    }

    public String encrypt(String plaintext) {
        try {
            ensureKey();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("API Key 加密失败", exception);
        }
    }

    public String decrypt(String encoded) {
        try {
            ensureKey();
            byte[] input = Base64.getDecoder().decode(encoded);
            byte[] iv = java.util.Arrays.copyOfRange(input, 0, IV_LENGTH);
            byte[] ciphertext = java.util.Arrays.copyOfRange(input, IV_LENGTH, input.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("API Key 解密失败", exception);
        }
    }

    private void ensureKey() {
        if (key.length != 32) {
            throw new IllegalStateException("请先在 .env 配置 AI_PROVIDER_ENCRYPTION_KEY（Base64 编码的 32 字节密钥）");
        }
    }
}
