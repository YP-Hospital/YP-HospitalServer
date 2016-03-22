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
    private static final String PHRASE = "Signature";
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private void generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(PKI_ALGORITHM);
            keyGen.initialize(112);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
            System.out.println("Public key: " + getString(
                    publicKey.getEncoded()));
            System.out.println("Private key: " + getString(
                    privateKey.getEncoded()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PrivateKey getPrivateKey(String privateKey) {
        PrivateKey key = null;
        try {
            byte[] publicBytes = getBytes(privateKey);
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
            byte[] publicBytes = getBytes(publicKey);
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
            Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
            dsa.initSign(privateKey);
            dsa.update(plaintext.getBytes());
            byte[] signature = dsa.sign();
            return getString(signature);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifySignature(String plaintext, String signature) {
        try {
            Signature dsa = Signature.getInstance(SIGNATURE_ALGORITHM);
            dsa.initVerify(publicKey);

            dsa.update(plaintext.getBytes());
            boolean verifies = dsa.verify(getBytes(signature));
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
            ///System.out.println( "text is not encrypted: no dashes" );
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
        //System.out.println( "text is encrypted" );
        return true;
    }

    private static String getString(byte[] bytes) {
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

    private static byte[] getBytes(String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringTokenizer st = new StringTokenizer(str, "-", false);
        while (st.hasMoreTokens()) {
            int i = Integer.parseInt(st.nextToken());
            bos.write((byte) i);
        }
        return bos.toByteArray();
    }

    public static List<String> createKeysToUser(String id) {
        PKIHandler pki = new PKIHandler();
        pki.generateKeys();
        String signature = pki.sign(PHRASE);
        System.out.println("Signature: " + signature);
        TCPServer.setMessageFromAnotherClass(getString(pki.privateKey.getEncoded()));
        return getCertificateToInsert(id, pki, signature);
    }

    public static String checkPrivateKey(String privateKey) {
        PKIHandler pki = new PKIHandler();
        pki.privateKey = getPrivateKey(privateKey);
        if (pki.privateKey == null) {
            return "false";
        } else {
            DatabaseHandler databaseHandler = new DatabaseHandler();
            String certificates = databaseHandler.select(Arrays.asList(new String []{"certificates", "open_key", "signature"}));
            List<String> words = new ArrayList<>(Arrays.asList(certificates.split("]\\[")));
            for (int i = 2; i < words.size(); i++) {
                pki.publicKey = getPublicKey(words.get(i++));
                if (pki.verifySignature(PHRASE, words.get(i++))) {
                    return words.get(i-1);
                }
            }
        }
        return "false";
    }

    @NotNull
    private static List<String> getCertificateToInsert(String id, PKIHandler pki, String signature) {
        List<String> certificateToInsert = new ArrayList<>();
        certificateToInsert.add("certificates");
        certificateToInsert.add(getString(pki.publicKey.getEncoded()));
        certificateToInsert.add(signature);
        certificateToInsert.add(id);
        return certificateToInsert;
    }
}
