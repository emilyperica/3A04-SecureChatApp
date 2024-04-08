// Kerberos citation: C. Paar, J. Pelzl, Understanding Cryptography: A Textbook for Students and Practitioners. Berlin: Springer, 2010.
// AES citation: GfG, “What is java AES encryption and decryption?,” GeeksforGeeks, https://www.geeksforgeeks.org/what-is-java-aes-encryption-and-decryption/.

package com.example.macmessenger;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthenticateAndEncrypt {
    public String senderID; // TODO: senderID and receiverID very ambiguous, need to be renamed
    public String receiverID;
    private byte[] sessionKey;
    private DataSnapshot snapshot;
    private DatabaseReference db;
    final private String dbStr = "https://easy-chat-backend-0-default-rtdb.firebaseio.com/";
    public AuthenticateAndEncrypt(String senderID, String senderPW, String receiverID) {
        // TODO: Delete user DBs on log out AND app termination AND session key expiry
        this.senderID = senderID;
        this.receiverID = receiverID;
        db = FirebaseDatabase.getInstance(dbStr).getReference();

        try {
            snapshot = Tasks.await(db.get());
            if (snapshot.child(senderID).exists()) {
                if (snapshot.child(senderID).child("sessionKey").exists()) {
                    // TODO: Test that db and key successfully retrieved
                    String sessionKeyStr = String.valueOf(snapshot.child(senderID).child("sessionKey").getValue());
                    this.sessionKey = sessionKeyStr.getBytes();
                }
                else {
                    decryptSessionKey(senderPW);
                }
            }
            else {
                initiateAuth(senderPW);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public byte[] encrypt(String plainText) {
        return AES.encrypt(plainText, sessionKey);
    }

    public String decrypt(byte[] cipherText) {
        return AES.decrypt(cipherText, sessionKey);
    }

    private boolean getDB() throws ExecutionException, InterruptedException {
        DatabaseReference database = FirebaseDatabase.getInstance("https://easy-chat-backend-0-default-rtdb.firebaseio.com/").getReference();
        DataSnapshot snapshot = Tasks.await(database.get());
        return snapshot.child("").exists();
    }

    private void initiateAuth(String privateKey) {
        // TODO: unify variable naming conventions
        KDC kdc = new KDC();
        byte[] nonce = new byte[64];
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(nonce);

        byte[][] keys = kdc.getUserKeys(senderID, receiverID, nonce);
        byte[] senderSessionKey = keys[0]; // y_A
        byte[] receiverSessionKey = keys[1]; // y_B

        String[] plainText = AES.decrypt(senderSessionKey, privateKey.getBytes()).split(":");

        if (!plainText[1].equals(Arrays.toString(nonce)) || !plainText[2].equals(receiverID)) {
            // TODO: KDC not trustworthy; raise an error
        }
        else {
            sessionKey = plainText[0].getBytes();
            db.child(senderID).child("sessionKey").setValue(sessionKey);
        }

        long timeStamp = System.currentTimeMillis();
        byte[] sharedSessionKey = AES.encrypt(senderID + ":" + timeStamp, sessionKey); // y_AB

        db.child(receiverID).child("sharedSessionKey").setValue(sharedSessionKey);
        db.child(receiverID).child("sessionKey").setValue(receiverSessionKey);

    }

    private void decryptSessionKey(String privateKey) {
        // TODO: TEST - make sure keys being correctly accessed
        String sharedSessionKey = String.valueOf(snapshot.child(senderID).child("sharedSessionKey").getValue());
        String senderSessionKey = String.valueOf(snapshot.child(senderID).child("mySessionKey").getValue());

        String[] plainText1 = AES.decrypt(senderSessionKey.getBytes(), privateKey.getBytes()).split(":");
        String[] plainText2 = AES.decrypt(sharedSessionKey.getBytes(), plainText1[0].getBytes()).split(":");

        // TODO: Lifetime and time stamp verifications
        if (!plainText1[1].equals(plainText2[0])) {
            // TODO: KDC not trustworthy; raise an error
        }
        else {
            sessionKey = plainText1[0].getBytes();
            db.child(senderID).child("sessionKey").setValue(sessionKey);
        }


    }

    private class KDC {
        private byte[] sessionKey = new byte[64];
        public long lifetime = System.currentTimeMillis() + 1000*60*60; // 1 hour
        public KDC() {
            SecureRandom rand = new SecureRandom();
            rand.nextBytes(sessionKey);
            db.child("KDC").child("sessionKey").setValue(sessionKey);
        }
        public byte[][] getUserKeys(String clientA, String clientB, byte[] nonce) {
            // TODO: unify variable naming conventions
            if (System.currentTimeMillis() > lifetime) { // check if key is expired
                SecureRandom rand = new SecureRandom();
                rand.nextBytes(sessionKey);
            }

            String messageA = Arrays.toString(sessionKey) + ":" + Arrays.toString(nonce) + ":" + lifetime + ":" + clientB;
            String messageB = Arrays.toString(sessionKey) + ":" + clientA + ":" + lifetime;

            String privateKeyA = String.valueOf(snapshot.child("users").child(clientA).child("password").getValue());
            String privateKeyB = String.valueOf(snapshot.child("users").child(clientB).child("password").getValue());

            byte[] sessionKeyA = AES.encrypt(messageA, privateKeyA.getBytes());
            byte[] sessionKeyB = AES.encrypt(messageB, privateKeyB.getBytes());

            return new byte[][] {sessionKeyA, sessionKeyB};
        }
    }

    private static class AES {
        static byte[] encrypt(String plainTextStr, byte[] key) {
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
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

        static String decrypt(byte[] cipherText, byte[] key) {
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
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
