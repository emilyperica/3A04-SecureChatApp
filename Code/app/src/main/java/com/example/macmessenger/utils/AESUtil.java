package com.example.macmessenger.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;

public class AESUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 16; // 128 bit
    private static SecretKeySpec keySpec;
    private static IvParameterSpec ivSpec;

    // Static initializer block to set up key and IV
    static {
        String key = "1234567890123456"; // Example key, ensure to use a secure method to generate/manage keys
        if (key == null || key.length() != KEY_SIZE) {
            throw new IllegalArgumentException("Key must be exactly " + KEY_SIZE + " characters long.");
        }
        keySpec = new SecretKeySpec(key.getBytes(), KEY_ALGORITHM);

        byte[] iv = new byte[KEY_SIZE]; // Ensure IV size matches block size
        new SecureRandom().nextBytes(iv);
        ivSpec = new IvParameterSpec(iv);
    }

    public static String encrypt(String input) {
        String encryptedString = input;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(input.getBytes());
            encryptedString = Base64.getEncoder().encodeToString(encrypted);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return encryptedString;

    }

    public static String decrypt(String input) {
        String decrypted = input;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(input));
            decrypted =  new String(original);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return decrypted;
    }
}