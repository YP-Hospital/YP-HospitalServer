package com.company.handlers;

import com.company.TCPServer;
import org.jetbrains.annotations.NotNull;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.io.*;

public class PKIHandler {
    private static final String PKI_ALGORITHM = "EC";
    private final static String HASH_ALGORITHM = "SHA1";
    private final static String SIGNATURE_ALGORITHM = HASH_ALGORITHM + "with" + PKI_ALGORITHM + "DSA";
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private void generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(PKI_ALGORITHM);
            keyGen.initialize(112);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            System.out.println("Public key: " + getStringFromBytes(
                    publicKey.getEncoded()));
            System.out.println("Private key: " + getStringFromBytes(
                    privateKey.getEncoded()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PrivateKey getPrivateKey(String privateKey) {
        PrivateKey key = null;
        try {
            byte[] publicBytes = getBytesFromString(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(PKI_ALGORITHM);
            key = keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return key;
    }

    private static PublicKey getPublicKey(String publicKey) {
        PublicKey key = null;
        try {
            byte[] publicBytes = getBytesFromString(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(PKI_ALGORITHM);
            key = keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return key;
    }

    private String sign(String plaintext) {
        try {
            Signature ecdsa = Signature.getInstance(SIGNATURE_ALGORITHM);
            ecdsa.initSign(privateKey);
            ecdsa.update(plaintext.getBytes());
            byte[] signature = ecdsa.sign();
            return getStringFromBytes(signature);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifySignature(String plaintext, String signature) {
        try {
            Signature ecdsa = Signature.getInstance(SIGNATURE_ALGORITHM);
            ecdsa.initVerify(publicKey);
            ecdsa.update(plaintext.getBytes());
            boolean verifies = ecdsa.verify(getBytesFromString(signature));
            System.out.println("signature verifies: " + verifies);
            return verifies;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns true if the specified text is encrypted, false otherwise
     */
    public static boolean isEncrypted(String text) {
        // If the string does not have any separators then it is not
        // encrypted
        if (text.indexOf('-') == -1) {
            return false;
        }

        StringTokenizer st = new StringTokenizer(text, "-", false);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() > 3) {
                return false;
            }
            for (int i = 0; i < token.length(); i++) {
                if (!Character.isDigit(token.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String getStringFromBytes(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            sb.append((int) (0x00FF & b));
            if (i + 1 < bytes.length) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    private static byte[] getBytesFromString(String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringTokenizer st = new StringTokenizer(str, "-", false);
        while (st.hasMoreTokens()) {
            int i = Integer.parseInt(st.nextToken());
            bos.write((byte) i);
        }
        return bos.toByteArray();
    }

    public static List<String> createKeysToUser(String userId) {
        PKIHandler pki = new PKIHandler();
        pki.generateKeys();
        TCPServer.setMessageFromAnotherClass(getStringFromBytes(pki.privateKey.getEncoded()));
        return getCertificateToInsert(userId, pki);
    }

    public static String getNewSignature(String privateKey, String text) {
        if (!isEncrypted(privateKey) || privateKey.equals("null")) {
            return "false";
        }
        PKIHandler pki = new PKIHandler();
        pki.privateKey = getPrivateKey(privateKey);
        if (pki.privateKey == null) {
            return "false";
        } else {
            String signature = pki.sign(text);
            return signature;
        }
    }

    @NotNull
    private static List<String> getCertificateToInsert(String userId, PKIHandler pki) {
        List<String> certificateToInsert = new ArrayList<>();
        certificateToInsert.add("certificates");
        certificateToInsert.add(getStringFromBytes(pki.publicKey.getEncoded()));
        certificateToInsert.add(userId);
        return certificateToInsert;
    }
}
