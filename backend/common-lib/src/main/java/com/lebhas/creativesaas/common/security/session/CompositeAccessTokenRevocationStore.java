package com.lebhas.creativesaas.common.security.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class CompositeAccessTokenRevocationStore implements AccessTokenRevocationStore {

    private static final Logger log = LoggerFactory.getLogger(CompositeAccessTokenRevocationStore.class);

    private final PersistentAccessTokenRevocationStore persistentStore;
    private final RedisAccessTokenRevocationStore redisStore;

    public CompositeAccessTokenRevocationStore(
            PersistentAccessTokenRevocationStore persistentStore,
            RedisAccessTokenRevocationStore redisStore
    ) {
        this.persistentStore = persistentStore;
        this.redisStore = redisStore;
    }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        persistentStore.revoke(tokenId, expiresAt);
        try {
            redisStore.revoke(tokenId, expiresAt);
        } catch (RuntimeException exception) {
            log.warn("Access-token revocation cache write failed for tokenId={}: {}", tokenId, exception.getMessage());
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        try {
            if (redisStore.isRevoked(tokenId)) {
                return true;
            }
        } catch (RuntimeException exception) {
            log.warn("Access-token revocation cache lookup failed for tokenId={}: {}", tokenId, exception.getMessage());
        }
        return persistentStore.isRevoked(tokenId);
    }
}
