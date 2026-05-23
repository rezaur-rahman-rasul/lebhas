package com.lebhas.creativesaas.common.security.session;

import java.time.Instant;

public class NoOpAccessTokenRevocationStore implements AccessTokenRevocationStore {

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
    }

    @Override
    public boolean isRevoked(String tokenId) {
        return false;
    }
}
