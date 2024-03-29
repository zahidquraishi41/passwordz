package com.zapps.passwordz.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MrCipher {
    private static final String TAG = "ZQ-MrCipher";
    public static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static String getSHA256(String s) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hash = messageDigest.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Converts key to SecretKey
     */
    private static SecretKey getSecretKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes());
        byte[] keyBytes = new byte[16];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String encrypt(String message, String key) throws Exception {
        // generating iv bytes
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);

        // generating cipher bytes
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key), new IvParameterSpec(ivBytes));
        byte[] cipherBytes = cipher.doFinal(message.getBytes());

        // combining iv bytes with cipher bytes
        byte[] combined = new byte[ivBytes.length + cipherBytes.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(cipherBytes, 0, combined, ivBytes.length, cipherBytes.length);

        // converting the combined bytes to string
        return Base64.getEncoder().encodeToString(combined);
    }

    public static String decrypt(String cipherText, String key) throws Exception {
        // decoding cipherText into bytes then extracting iv bytes and message bytes
        byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
        byte[] ivBytes = Arrays.copyOfRange(decodedBytes, 0, 16);
        byte[] messageBytes = Arrays.copyOfRange(decodedBytes, 16, decodedBytes.length);

        // converting message byte (which is encrypted) to plain bytes
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key), new IvParameterSpec(ivBytes));
        byte[] plainBytes = cipher.doFinal(messageBytes);

        // converting plain bytes to string
        return new String(plainBytes);
    }
}
