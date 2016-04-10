package com.company.Crypto.Shamir;

import java.math.BigInteger;

public final class SecretShare {

    private final int num;
    private final BigInteger share;

    public SecretShare(final int num, final BigInteger share) {
        this.num = num;
        this.share = share;
    }

    public int getNum() {
        return num;
    }

    public BigInteger getShare() {
        return share;
    }

    @Override
    public String toString() {
        return "SecretShare [num=" + num + ", share=" + share + "]";
    }
}
