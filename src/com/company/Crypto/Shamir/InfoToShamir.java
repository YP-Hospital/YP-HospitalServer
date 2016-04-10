package com.company.Crypto.Shamir;

import java.math.BigInteger;

public final class InfoToShamir {
    private final SecretShare[] shares;
    private final BigInteger prime;

    public InfoToShamir(SecretShare[] shares, BigInteger prime) {
        this.shares = shares;
        this.prime = prime;
    }

    public InfoToShamir(BigInteger prime, String ... shares) {
        this.shares = new SecretShare[shares.length];
        for (int i = 0; i < shares.length; i++) {
            this.shares[i] = new SecretShare(i, new BigInteger(shares[i]));
        }
        this.prime = prime;
    }

    public SecretShare[] getShares() {
        return shares;
    }

    public BigInteger getPrime() {
        return prime;
    }
}
