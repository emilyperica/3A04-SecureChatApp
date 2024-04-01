package com.example.macmessenger;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

public class Encryption extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    static class AES {
        // citation: https://www.geeksforgeeks.org/what-is-java-aes-encryption-and-decryption/
        // AES-128
        private static byte[] KEY; // 128-bits
        private static final int Nb = 4;
        private static final int Nr = 10;

        static byte[] encrypt(String plainTextStr, String key) {
            KEY = new byte[] {0}; // generate from KDC

            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
                SecretKey secretKey = factory.generateSecret(keySpec);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);

                return cipher.doFinal(plainTextStr.getBytes());
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }

            return null;
        }

        static String decrypt(byte[] cipherText, String key) {
            KEY = new byte[] {0}; // generate key from KDC

            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(KEY, "AES");
                SecretKey secretKey = factory.generateSecret(keySpec);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);

                return new String (cipher.doFinal(cipherText));
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }

            return null;
        }
    }
}