package me.gurupras.otp_helper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SimpleCrypto {
    private SecretKey aesKey;
    private SecureRandom random;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    SimpleCrypto(SecretKey aesKey) {
        this.aesKey = aesKey;
        this.random = new SecureRandom();
    }

    public byte[] randomBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }

    public String encrypt(String plainText) throws Exception {
        return encrypt(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(byte[] plainBytes) throws Exception {
        byte[] ivBytes = randomBytes(16);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        byte[] encrypted = cipher.doFinal(plainBytes);
        String encryptedHex = toHex(encrypted);
        String ivHex = toHex(ivBytes);
        return ivHex + ":" + encryptedHex;
    }

    public String decrypt(String encryptedText) throws Exception {
        String ivHex = encryptedText.substring(0, encryptedText.indexOf(":"));
        String encryptedHex = encryptedText.substring(ivHex.length() + 1);

        byte[] ivBytes = hexStringToByteArray(ivHex);
        byte[] encryptedBytes = hexStringToByteArray(encryptedHex);

        SecretKeySpec skeySpec = new SecretKeySpec(aesKey.getEncoded(), "AES");
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        byte[] original = cipher.doFinal(encryptedBytes);
        return new String(original);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
