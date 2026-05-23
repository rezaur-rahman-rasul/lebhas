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
        name = "download_usage_logs",
        schema = "platform",
        indexes = {
                @Index(name = "idx_download_usage_logs_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_download_usage_logs_generated_version_id", columnList = "generated_version_id"),
                @Index(name = "idx_download_usage_logs_asset_id", columnList = "asset_id"),
                @Index(name = "idx_download_usage_logs_downloaded_by", columnList = "downloaded_by")
        })
public class DownloadUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    @Column(name = "generated_version_id")
    private UUID generatedVersionId;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "downloaded_by")
    private UUID downloadedBy;

    @Column(name = "download_type", length = 60)
    private String downloadType;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DownloadUsageLog() {
    }

    public static DownloadUsageLog create(
            UUID workspaceId,
            UUID generatedVersionId,
            UUID assetId,
            UUID downloadedBy,
            String downloadType,
            String ipAddress,
            String userAgent
    ) {
        DownloadUsageLog log = new DownloadUsageLog();
        log.workspaceId = require(workspaceId, "workspaceId");
        log.generatedVersionId = generatedVersionId;
        log.assetId = assetId;
        log.downloadedBy = downloadedBy;
        log.downloadType = normalizeNullable(downloadType);
        log.ipAddress = normalizeNullable(ipAddress);
        log.userAgent = normalizeNullable(userAgent);
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

    public UUID getGeneratedVersionId() {
        return generatedVersionId;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public UUID getDownloadedBy() {
        return downloadedBy;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
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
