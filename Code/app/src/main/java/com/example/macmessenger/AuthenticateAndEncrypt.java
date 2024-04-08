// Kerberos citation: C. Paar, J. Pelzl, Understanding Cryptography: A Textbook for Students and Practitioners. Berlin: Springer, 2010.
// AES citation: GfG, “What is java AES encryption and decryption?,” GeeksforGeeks, https://www.geeksforgeeks.org/what-is-java-aes-encryption-and-decryption/.

package com.example.macmessenger;

import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AuthenticateAndEncrypt {
    public String senderID;
    public String receiverID;
    private String sessionKey;
    private DataSnapshot snapshot;
    private DatabaseReference db;
    final private String dbStr = "https://easy-chat-backend-0-default-rtdb.firebaseio.com/";
    public AuthenticateAndEncrypt(String senderID, String senderPW, String receiverID) {
        // TODO: Delete user DBs on log out AND app termination AND session key expiry
        this.senderID = senderID;
        this.receiverID = receiverID;
        db = FirebaseDatabase.getInstance(dbStr).getReference();

        try {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    snapshot = dataSnapshot;
                    if (dataSnapshot.hasChild(senderID)) {
                        if (dataSnapshot.child(senderID).hasChild("sessionKey")) {
                            // TODO: Test that db and key successfully retrieved
                            sessionKey = String.valueOf(dataSnapshot.child(senderID).child("sessionKey").getValue());
                        } else {
                            decryptSessionKey(senderPW);
                        }
                    } else {
                        initiateAuth(senderPW);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            db.addValueEventListener(listener);
        }catch (Exception e) {
            System.out.print(e.getMessage());
        }

    }

    public String encrypt(String plainText) {
        return AES.encrypt(plainText, sessionKey);
    }

    public String decrypt(String cipherText) {
        return AES.decrypt(cipherText, sessionKey);
    }

    private boolean getDB() throws ExecutionException, InterruptedException {
        DatabaseReference database = FirebaseDatabase.getInstance("https://easy-chat-backend-0-default-rtdb.firebaseio.com/").getReference();
        DataSnapshot snapshot = Tasks.await(database.get());
        return snapshot.child("").exists();
    }

    private void initiateAuth(String privateKey) {
        KDC kdc = new KDC();
        byte[] nonce = new byte[64];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(nonce);

        String[] keys = kdc.getUserKeys(senderID, receiverID, nonce);
        String senderSessionKey = keys[0]; // y_A
        String receiverSessionKey = keys[1]; // y_B

        String plainText = new String(Base64.getDecoder().decode(AES.decrypt(senderSessionKey, privateKey).getBytes(StandardCharsets.UTF_8)));

        String[] plainTextLs = plainText.split(":");

        if (!plainTextLs[1].equals(Arrays.toString(nonce)) || !plainTextLs[3].equals(receiverID)) {
            // TODO: KDC not trustworthy; raise an error
        }
        else {
            sessionKey = plainTextLs[0];
            db.child(senderID).child("sessionKey").setValue(sessionKey);
        }

        long timeStamp = System.currentTimeMillis();
        String sharedSessionKey = AES.encrypt(senderID + ":" + timeStamp, sessionKey); // y_AB

        db.child(receiverID).child("sharedSessionKey").setValue(sharedSessionKey);
        db.child(receiverID).child("publicSessionKey").setValue(receiverSessionKey);

    }

    private void decryptSessionKey(String privateKey) {
        String sharedSessionKey = String.valueOf(snapshot.child(senderID).child("sharedSessionKey").getValue());
        String senderSessionKey = String.valueOf(snapshot.child(senderID).child("publicSessionKey").getValue());

        String[] plainText1 = AES.decrypt(senderSessionKey, privateKey).split(":");
        String[] plainText2 = AES.decrypt(sharedSessionKey, plainText1[0]).split(":");

        if (!plainText1[1].equals(plainText2[0])) {
            // KDC not trustworthy
        }
        else {
            sessionKey = plainText1[0];
            db.child(senderID).child("sessionKey").setValue(sessionKey);
        }
    }

    private class KDC {
        private byte[] keyBytes = new byte[64];
        private String sessionKey;
        public long lifetime = System.currentTimeMillis() + 1000*60*60; // 1 hour
        public KDC() {
            SecureRandom rand = new SecureRandom();
            rand.nextBytes(keyBytes);
            sessionKey = new String(keyBytes, StandardCharsets.UTF_8);
            db.child("KDC").child("sessionKey").setValue(sessionKey);
        }
        public String[] getUserKeys(String clientA, String clientB, byte[] nonce) {
            if (System.currentTimeMillis() > lifetime) { // check if key is expired
                SecureRandom rand = new SecureRandom();
                rand.nextBytes(keyBytes);
                sessionKey = Base64.getEncoder().encodeToString(keyBytes);
            }

            String messageA = sessionKey + ":" + Arrays.toString(nonce) + ":" + lifetime + ":" + clientB;
            String messageB = sessionKey + ":" + clientA + ":" + lifetime;

            String privateKeyA = String.valueOf(snapshot.child("users").child(clientA).child("password").getValue());
            String privateKeyB = String.valueOf(snapshot.child("users").child(clientB).child("password").getValue());

            String sessionKeyA = AES.encrypt(messageA, privateKeyA);
            String sessionKeyB = AES.encrypt(messageB, privateKeyB);

            return new String[] {sessionKeyA, sessionKeyB};
        }
    }

    private static class AES {
        static String encrypt(String plainTextStr, String key) {
            try {
                byte[] iv = new byte[16];
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);

                SecretKeyFactory factory
                        = SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(key.toCharArray(), salt,16384, 128);
                SecretKeySpec keySpec = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

                return Base64.getEncoder().encodeToString(cipher.doFinal(plainTextStr.getBytes(StandardCharsets.UTF_8)));
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }

            return null;
        }

        static String decrypt(String cipherText, String key) {
            try {
                byte[] iv = new byte[16];
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);

                SecretKeyFactory factory
                        = SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(key.toCharArray(), salt,16384, 128);
                SecretKeySpec keySpec = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

                return Base64.getEncoder().encodeToString(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }

            return null;
        }
    }
}
