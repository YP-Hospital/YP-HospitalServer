package com.company.Crypto;

import com.company.Crypto.Shamir.InfoToShamir;
import com.company.Crypto.Shamir.Shamir;
import com.company.TCPServer;
import com.company.handlers.DatabaseHandler;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class PKI {
    private static final String PKI_ALGORITHM = "EC";
    private final static String HASH_ALGORITHM = "SHA1";
    private final static String SIGNATURE_ALGORITHM = HASH_ALGORITHM + "with" + PKI_ALGORITHM + "DSA";
    private static String partToInputIntoDB;
    private static InfoToShamir info;
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
            byte[] privateBytes = getBytesFromString(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
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

    public static String getStringFromBytes(byte[] bytes) {
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

    public static byte[] getBytesFromString(String str) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StringTokenizer st = new StringTokenizer(str, "-", false);
        while (st.hasMoreTokens()) {
            int i = Integer.parseInt(st.nextToken());
            bos.write((byte) i);
        }
        return bos.toByteArray();
    }

    public static List<String> createKeysToUser(String userId) {
        PKI pki = new PKI();
        pki.generateKeys();
        String shorteKey = generateShorterKey(getStringFromBytes(pki.privateKey.getEncoded()));
        TCPServer.setMessageFromAnotherClass(shorteKey);
        return getCertificateToInsert(userId, pki);
    }

    private static String generateShorterKey(String privateKey) {
        String[] separatedKey = privateKey.split("-");
        String partToSeparateByShamir =  separatedKey[separatedKey.length-1];
        info = Shamir.getKeysByShamir(partToSeparateByShamir);
        partToInputIntoDB = privateKey.substring(0, privateKey.length() - partToSeparateByShamir.length());
        partToInputIntoDB =  encryptFirstPartOfKey(partToInputIntoDB);
        return info.getShares()[1].getShare().toString();
    }

    private static String encryptFirstPartOfKey(String partToInputIntoDB) {
        String result = ExtraCrypto.textSymmetricKeyEncrypt(partToInputIntoDB, info.getShares()[1].getShare().toString());
        return result;
    }

    public static String getNewSignature(String key, String text) {
        if (key.equals("null")) {
            return "false";
        }
        String privateKey = restorePrivateKey(key);
        if (privateKey == null) {
            return "false";
        }
        PKI pki = new PKI();
        pki.privateKey = getPrivateKey(privateKey);
        if (pki.privateKey == null) {
            return "false";
        } else {
            String signature = pki.sign(text);
            return signature;
        }
    }

    private static String restorePrivateKey(String key) {
        String privateKey = "";
        DatabaseHandler databaseHandler = new DatabaseHandler();
        String certificates = databaseHandler.select(new ArrayList<>(Arrays.asList(new String[]{"certificates", "first_part_key",
                                                                          "servers_key", "prime"})));
        System.out.println(certificates);
        List<String> certificatesData = new ArrayList<>(Arrays.asList(certificates.split(DatabaseHandler.separatorForSplit)));
        for (int i = 4; i < certificatesData.size(); i+=4) {
            privateKey += ExtraCrypto.textSymmetricKeyDecrypt(certificatesData.get(i), key);
            if (isEncrypted(privateKey)) {
                InfoToShamir info = new InfoToShamir(new BigInteger(certificatesData.get(i+2)), certificatesData.get(i+1), key);
                privateKey += Shamir.getSecretBack(info);
                return privateKey;
            }
            privateKey = "";
        }
        return null;
    }

    public static Boolean isAdminKey(String key) {
        String privateKey = restorePrivateKey(key);
        if (privateKey == null) {
            return false;
        }
        DatabaseHandler databaseHandler = new DatabaseHandler();
        String certificate = databaseHandler.select(new ArrayList<>(Arrays.asList(new String[]{"certificates", "doctor_id",
                "where", "prime", info.getPrime().toString()})));
        String userRole = databaseHandler.select(new ArrayList<>(Arrays.asList(new String[]{"users", "role",
                                                "where", "id", certificate.split(DatabaseHandler.separatorForSplit)[1]})));
        return userRole.split(DatabaseHandler.separatorForSplit)[1].equals("Admin");
    }

    @NotNull
    private static List<String> getCertificateToInsert(String userId, PKI pki) {
        List<String> certificateToInsert = new ArrayList<>();
        certificateToInsert.add("certificates");
        certificateToInsert.add(getStringFromBytes(pki.publicKey.getEncoded()));
        certificateToInsert.add(userId);
        certificateToInsert.add(partToInputIntoDB);
        certificateToInsert.add(info.getShares()[0].getShare().toString());
        certificateToInsert.add(info.getPrime().toString());
        return certificateToInsert;
    }
}
