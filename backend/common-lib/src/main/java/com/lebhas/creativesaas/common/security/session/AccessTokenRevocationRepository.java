package com.lebhas.creativesaas.common.security.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccessTokenRevocationRepository extends JpaRepository<AccessTokenRevocationEntity, UUID> {

    Optional<AccessTokenRevocationEntity> findByTokenIdAndDeletedFalse(String tokenId);

    boolean existsByTokenIdAndExpiresAtAfterAndDeletedFalse(String tokenId, Instant expiresAt);
}
