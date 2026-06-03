package com.lebhas.creativesaas.sharing.domain;

import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "share_links",
        schema = "platform",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_share_links_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "idx_share_links_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_share_links_generated_version_id", columnList = "generated_version_id"),
                @Index(name = "idx_share_links_workspace_generated_version_created_at", columnList = "workspace_id,generated_version_id,created_at"),
                @Index(name = "idx_share_links_expires_at", columnList = "expires_at")
        })
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "generated_version_id", nullable = false, updatable = false)
    private UUID generatedVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_version_id", nullable = false, insertable = false, updatable = false)
    private GeneratedVersionEntity generatedVersion;

    @Column(name = "token_hash", nullable = false, unique = true, length = 120)
    private String tokenHash;

    @Column(name = "token", length = 120)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "access_count", nullable = false)
    private long accessCount;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    protected ShareLink() {
    }

    public static ShareLink create(UUID workspaceId, UUID generatedVersionId, String tokenHash, Instant expiresAt, UUID createdBy) {
        ShareLink shareLink = new ShareLink();
        shareLink.workspaceId = require(workspaceId, "workspaceId");
        shareLink.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        shareLink.tokenHash = normalizeRequired(tokenHash, "tokenHash");
        shareLink.expiresAt = require(expiresAt, "expiresAt");
        shareLink.accessCount = 0L;
        shareLink.createdBy = require(createdBy, "createdBy");
        return shareLink;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public GeneratedVersionEntity getGeneratedVersion() {
        return generatedVersion;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    /**
     * Kept for older internal call sites; this value is a hash, never the raw public token.
     */
    public String getToken() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getRevokedBy() {
        return revokedBy;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void incrementAccessCount() {
        this.accessCount += 1L;
    }

    public void revoke(UUID revokedBy) {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
            this.revokedBy = revokedBy;
        }
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static Instant require(Instant value, String field) {
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
