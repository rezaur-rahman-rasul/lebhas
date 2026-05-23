package com.lebhas.creativesaas.common.security.session;

import java.time.Instant;

public interface AccessTokenRevocationStore {

    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}
