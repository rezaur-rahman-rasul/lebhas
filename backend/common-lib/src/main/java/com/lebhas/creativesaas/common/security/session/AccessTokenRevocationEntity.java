package com.lebhas.creativesaas.common.security.session;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "access_token_revocations",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_access_token_revocations_token_id", columnNames = "token_id")
)
public class AccessTokenRevocationEntity extends BaseEntity {

    @Column(name = "token_id", nullable = false, length = 80)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    protected AccessTokenRevocationEntity() {
    }

    public static AccessTokenRevocationEntity revoke(String tokenId, Instant expiresAt, Instant revokedAt) {
        AccessTokenRevocationEntity entity = new AccessTokenRevocationEntity();
        entity.tokenId = tokenId;
        entity.expiresAt = expiresAt;
        entity.revokedAt = revokedAt;
        return entity;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void refresh(Instant expiresAt, Instant revokedAt) {
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }
}
