package com.lebhas.creativesaas.sharing.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legacy_public_share_links", schema = "platform")
@Deprecated(forRemoval = true)
public class PublicShareLinkEntity extends TenantAwareEntity {

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @Column(name = "token", nullable = false, unique = true, length = 120)
    private String token;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "password_protected", nullable = false)
    private boolean passwordProtected;

    @Column(name = "access_count", nullable = false)
    private long accessCount;

    @Column(name = "created_by_user_id", updatable = false)
    private UUID createdByUserId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected PublicShareLinkEntity() {
    }

    public static PublicShareLinkEntity create(UUID workspaceId, UUID generatedVersionId, String token, Instant expiresAt) {
        return create(workspaceId, generatedVersionId, token, expiresAt, false, null);
    }

    public static PublicShareLinkEntity create(
            UUID workspaceId,
            UUID generatedVersionId,
            String token,
            Instant expiresAt,
            boolean passwordProtected,
            UUID createdByUserId
    ) {
        PublicShareLinkEntity entity = new PublicShareLinkEntity();
        entity.assignWorkspace(workspaceId);
        entity.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        entity.token = normalizeRequired(token, "token");
        entity.expiresAt = expiresAt;
        entity.passwordProtected = passwordProtected;
        entity.accessCount = 0L;
        entity.createdByUserId = createdByUserId;
        entity.active = true;
        return entity;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isPasswordProtected() {
        return passwordProtected;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired(Instant referenceTime) {
        return expiresAt != null && referenceTime != null && expiresAt.isBefore(referenceTime);
    }

    public void incrementAccessCount() {
        this.accessCount += 1L;
    }

    public void deactivate() {
        this.active = false;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
