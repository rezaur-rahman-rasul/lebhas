package com.lebhas.creativesaas.usage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "share_usage_logs",
        schema = "platform",
        indexes = {
                @Index(name = "idx_share_usage_logs_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_share_usage_logs_share_link_id", columnList = "share_link_id"),
                @Index(name = "idx_share_usage_logs_generated_version_id", columnList = "generated_version_id"),
                @Index(name = "idx_share_usage_logs_accessed_by_user_id", columnList = "accessed_by_user_id")
        })
public class ShareUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "share_link_id", nullable = false)
    private UUID shareLinkId;

    @Column(name = "generated_version_id", nullable = false)
    private UUID generatedVersionId;

    @Column(name = "accessed_by_user_id")
    private UUID accessedByUserId;

    @Column(name = "access_ip", length = 80)
    private String accessIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referrer", length = 1000)
    private String referrer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ShareUsageLog() {
    }

    public static ShareUsageLog create(
            UUID workspaceId,
            UUID shareLinkId,
            UUID generatedVersionId,
            UUID accessedByUserId,
            String accessIp,
            String userAgent,
            String referrer
    ) {
        ShareUsageLog log = new ShareUsageLog();
        log.workspaceId = require(workspaceId, "workspaceId");
        log.shareLinkId = require(shareLinkId, "shareLinkId");
        log.generatedVersionId = require(generatedVersionId, "generatedVersionId");
        log.accessedByUserId = accessedByUserId;
        log.accessIp = normalizeNullable(accessIp);
        log.userAgent = normalizeNullable(userAgent);
        log.referrer = normalizeNullable(referrer);
        return log;
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

    public UUID getShareLinkId() {
        return shareLinkId;
    }

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getAccessedByUserId() {
        return accessedByUserId;
    }

    public String getAccessIp() {
        return accessIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
