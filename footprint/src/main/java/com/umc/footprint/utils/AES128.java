package com.umc.footprint.utils;

import static java.nio.charset.StandardCharsets.*;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AES128 {

    private static String key;

    public static IvParameterSpec getIv() {
        return new IvParameterSpec(key.substring(0, 16).getBytes());
    }

    public static SecretKey getKeySpec() {
        byte[] keyBytes = new byte[16];
        System.arraycopy(key.getBytes(UTF_8), 0, keyBytes, 0, keyBytes.length);
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Value("${encrypt.key}")
    public void setKey(String key) {
        AES128.key = key;
    }

    //암호화 관련 함수
    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), getIv());
            byte[] encrypted = cipher.doFinal(value.getBytes(UTF_8));
            return new String(Base64.getEncoder().encode(encrypted));
        } catch (Exception exception) {
            throw new IllegalArgumentException("암호화 실패", exception);
        }
    }

    //복호화 관련함수
    public static String decrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), getIv());
            byte[] decrypted = Base64.getDecoder().decode(value.getBytes());

            return new String(cipher.doFinal(decrypted), UTF_8);
        } catch (Exception exception) {
            throw new IllegalArgumentException("복호화 실패", exception);
        }
    }
}
