package com.company.Crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ExtraCrypto {

    public static String SYMMETRIC_ALGORITHM = "AES";
    public static String HASH_ALGORITHM = "SHA-1";
    static Key publicSymmetricKey;
    private static String textField = "text";
    public static String encryptedBySymmetric;

    private static String getHexadecimalValue(byte[] result) {
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

    public static String textSymmetricKeyDecrypt(){
        String recovered = null;
        try {
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicSymmetricKey);
            byte[] recoveredBytes = cipher.doFinal(hexStringToByteArray(encryptedBySymmetric));
            recovered = new String(recoveredBytes);
            System.out.println(recovered);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return  recovered;
    }

    public static void textSymmetricKeyEncrypt(String t) {
        try {
            publicSymmetricKey = new SecretKeySpec(t.getBytes(), 0, 16, SYMMETRIC_ALGORITHM);
            //publicSymmetricKey = KeyGenerator.getInstance(SYMMETRIC_ALGORITHM).generateKey();
            Cipher cipher = Cipher.getInstance(SYMMETRIC_ALGORITHM);
            byte[] encryptionBytes = null;
            System.out.println("Entered: " + textField);
            cipher.init(Cipher.ENCRYPT_MODE, publicSymmetricKey);
            byte[] inputBytes = textField.getBytes();
            encryptionBytes = cipher.doFinal(inputBytes);
            encryptedBySymmetric = getHexadecimalValue(encryptionBytes);
            System.out.println(encryptedBySymmetric);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String textToHash(BigInteger textField) {
        String hashText = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            messageDigest.reset();
            messageDigest.update(textField.toByteArray());

            byte result[] = messageDigest.digest();
            hashText = getHexadecimalValue(result);
            System.out.println(hashText);
        } catch (NoSuchAlgorithmException e1) {
            e1.getMessage();
        }
        return hashText;
    }
}
