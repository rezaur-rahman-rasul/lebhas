package com.lebhas.creativesaas.download.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "download_logs", schema = "platform")
public class DownloadLogEntity extends TenantAwareEntity {

    @Column(name = "generated_version_id", updatable = false)
    private UUID generatedVersionId;

    @Column(name = "asset_id", updatable = false)
    private UUID assetId;

    @Column(name = "downloaded_by")
    private UUID downloadedBy;

    @Column(name = "download_type", length = 60)
    private String downloadType;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    protected DownloadLogEntity() {
    }

    public static DownloadLogEntity create(
            UUID workspaceId,
            UUID generatedVersionId,
            UUID downloadedBy,
            String downloadType,
            String ipAddress,
            String userAgent
    ) {
        DownloadLogEntity entity = new DownloadLogEntity();
        entity.assignWorkspace(workspaceId);
        entity.generatedVersionId = generatedVersionId;
        entity.downloadedBy = downloadedBy;
        entity.downloadType = downloadType;
        entity.ipAddress = ipAddress;
        entity.userAgent = userAgent;
        return entity;
    }

    public static DownloadLogEntity createForAsset(
            UUID workspaceId,
            UUID assetId,
            UUID downloadedBy,
            String downloadType,
            String ipAddress,
            String userAgent
    ) {
        DownloadLogEntity entity = new DownloadLogEntity();
        entity.assignWorkspace(workspaceId);
        entity.assetId = assetId;
        entity.downloadedBy = downloadedBy;
        entity.downloadType = downloadType;
        entity.ipAddress = ipAddress;
        entity.userAgent = userAgent;
        return entity;
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

    public String getDownloadSource() {
        return downloadType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public java.time.Instant getDownloadedAt() {
        return getCreatedAt();
    }
}
