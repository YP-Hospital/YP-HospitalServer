package com.company.Crypto.Shamir;

import java.math.BigInteger;
import java.util.Random;

public final class Shamir {

    private BigInteger prime;

    private final int neededKeys;
    private final int allKeys;
    private final Random random;

    private static final int CERTAINTY = 50;

    public Shamir(final int neededKeys, final int allKeys) {
        this.neededKeys = neededKeys;
        this.allKeys = allKeys;

        random = new Random();
    }

    public SecretShare[] split(final BigInteger secret) {
        final int modLength = secret.bitLength() + 1;

        prime = new BigInteger(modLength, CERTAINTY, random);
        final BigInteger[] coeff = new BigInteger[neededKeys - 1];

        System.out.println("Prime Number: " + prime);

        for (int i = 0; i < neededKeys - 1; i++) {
            coeff[i] = randomZp(prime);
        }

        final SecretShare[] shares = new SecretShare[allKeys];
        for (int i = 1; i <= allKeys; i++) {
            BigInteger accum = secret;

            for (int j = 1; j < neededKeys; j++) {
                final BigInteger t1 = BigInteger.valueOf(i).modPow(BigInteger.valueOf(j), prime);
                final BigInteger t2 = coeff[j - 1].multiply(t1).mod(prime);

                accum = accum.add(t2).mod(prime);
            }
            shares[i - 1] = new SecretShare(i - 1, accum);
            System.out.println("Share " + shares[i - 1]);
        }

        return shares;
    }

    public BigInteger getPrime() {
        return prime;
    }

    public BigInteger combine(final SecretShare[] shares, final BigInteger primeNum) {
        BigInteger accum = BigInteger.ZERO;
        for (int i = 0; i < neededKeys; i++) {
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < neededKeys; j++) {
                if (i != j) {
                    num = num.multiply(BigInteger.valueOf(-j - 1)).mod(primeNum);
                    den = den.multiply(BigInteger.valueOf(i - j)).mod(primeNum);
                }
            }
            final BigInteger value = shares[i].getShare();

            final BigInteger tmp = value.multiply(num).multiply(den.modInverse(primeNum)).mod(primeNum);
            accum = accum.add(primeNum).add(tmp).mod(primeNum);
        }
        return accum;
    }

    private BigInteger randomZp(final BigInteger p) {
        while (true) {
            final BigInteger r = new BigInteger(p.bitLength(), random);
            if (r.compareTo(BigInteger.ZERO) > 0 && r.compareTo(p) < 0) {
                return r;
            }
        }
    }

    public static InfoToShamir getKeysByShamir(String text) {
        final Shamir shamir = new Shamir(2, 2);
        final BigInteger secret = new BigInteger(text.getBytes());
        System.out.println(secret);
        final SecretShare[] shares = shamir.split(secret);
        final BigInteger prime = shamir.getPrime();
        return new InfoToShamir(shares, prime);
    }

    public static String getSecretBack(InfoToShamir info) {
        final Shamir shamir2 = new Shamir(2, 2);
        final BigInteger result = shamir2.combine(info.getShares(), info.getPrime());
        System.out.println(new String(result.toByteArray()));
        return  new String(result.toByteArray());
    }

}