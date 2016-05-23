package com.company.Crypto;

import org.jetbrains.annotations.NotNull;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ExtraCrypto {

    public static String SYMMETRIC_ALGORITHM = "AES";
    public static String HASH_ALGORITHM = "SHA1";

    public static String getHexadecimalValue(byte[] result) {
        StringBuffer buf = new StringBuffer();
        for (byte bytes : result) {
            buf.append(String.format("%02x", bytes & 0xff));
        }
        return buf.toString();
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

    public static String textSymmetricKeyDecrypt(String toDecrypt, String key){
        String recovered = null;
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] encodedKey = decoder.decodeBuffer(key);
            Key publicSymmetricKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, SYMMETRIC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicSymmetricKey);
            byte[] recoveredBytes = cipher.doFinal(hexStringToByteArray(toDecrypt));
            recovered = new String(recoveredBytes);
            System.out.println(recovered);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            System.out.println("NoSuchPaddingException");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            System.out.println("InvalidKeyException");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            System.out.println("IllegalBlockSizeException");
            e.printStackTrace();
        } catch (BadPaddingException e) {
//            System.out.println("BadPaddingException");
//            e.printStackTrace();
            System.out.println("Wrong key!");
            return null;
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
        return  recovered;
    }

    public static String textSymmetricKeyEncrypt(String toEncrypt, String key) {
        String result = "";
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] encodedKey = decoder.decodeBuffer(key);
            Key publicSymmetricKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, SYMMETRIC_ALGORITHM);
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            byte[] encryptionBytes = null;
            System.out.println("Entered: " + toEncrypt);
            cipher.init(Cipher.ENCRYPT_MODE, publicSymmetricKey);
            byte[] inputBytes = toEncrypt.getBytes();
            encryptionBytes = cipher.doFinal(inputBytes);
            result = getHexadecimalValue(encryptionBytes);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String generateNewSymmetricKey() {
        BASE64Encoder encoder = new BASE64Encoder();
        Key key = null;
        try {
            key = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM).generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encoder.encode(key.getEncoded());
    }

    public static String textToHash(String toHash) {
        String hashText = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            messageDigest.reset();
            messageDigest.update(toHash.getBytes());

            byte result[] = messageDigest.digest();
            hashText = getHexadecimalValue(result);
            System.out.println(hashText);
        } catch (NoSuchAlgorithmException e1) {
            e1.getMessage();
        }
        return hashText;
    }
}
