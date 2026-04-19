package com.study.english.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 使用 MD5 + 密钥做密码哈希，与明文同时落库。
 * 密钥参与运算：hash = MD5(密钥 + 明文)，校验时用同一密钥重算后比较。
 */
public class Md5KeyPasswordEncoder implements PasswordEncoder {

    private static final String ALGORITHM = "MD5";
    /** 后端密钥（与 init-data 中生成的 hash 一致） */
    private static final String SECRET_KEY = "english-saas-secret-2024";

    @Override
    public String encode(CharSequence rawPassword) {
        return md5Hex(SECRET_KEY + rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return encode(rawPassword).equalsIgnoreCase(encodedPassword);
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
