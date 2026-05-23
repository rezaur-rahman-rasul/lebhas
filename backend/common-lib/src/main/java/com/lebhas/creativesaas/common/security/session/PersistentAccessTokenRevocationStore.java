package com.lebhas.creativesaas.common.security.session;

import java.time.Clock;
import java.time.Instant;

public class PersistentAccessTokenRevocationStore implements AccessTokenRevocationStore {

    private final AccessTokenRevocationRepository repository;
    private final Clock clock;

    public PersistentAccessTokenRevocationStore(AccessTokenRevocationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null || !expiresAt.isAfter(clock.instant())) {
            return;
        }
        Instant now = clock.instant();
        AccessTokenRevocationEntity entity = repository.findByTokenIdAndDeletedFalse(tokenId)
                .orElseGet(() -> AccessTokenRevocationEntity.revoke(tokenId, expiresAt, now));
        entity.refresh(expiresAt, now);
        repository.save(entity);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return true;
        }
        return repository.existsByTokenIdAndExpiresAtAfterAndDeletedFalse(tokenId, clock.instant());
    }
}
