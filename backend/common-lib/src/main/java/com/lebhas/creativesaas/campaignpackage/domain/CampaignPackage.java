package com.lebhas.creativesaas.campaignpackage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_packages", schema = "platform")
public class CampaignPackage extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CampaignPackageStatus status;

    @Column(name = "r2_object_key", length = 600)
    private String r2ObjectKey;

    @Column(name = "export_url", length = 1000)
    private String exportUrl;

    @Column(name = "export_url_expires_at")
    private Instant exportUrlExpiresAt;

    protected CampaignPackage() {
    }

    public static CampaignPackage create(UUID workspaceId, UUID projectId, String name, String description) {
        CampaignPackage pack = new CampaignPackage();
        pack.assignWorkspace(workspaceId);
        pack.projectId = require(projectId, "projectId");
        pack.name = required(name, "name");
        pack.description = normalize(description);
        pack.status = CampaignPackageStatus.CREATED;
        return pack;
    }

    public void markExportRequested(String r2ObjectKey, String exportUrl, Instant expiresAt) {
        this.r2ObjectKey = normalize(r2ObjectKey);
        this.exportUrl = normalize(exportUrl);
        this.exportUrlExpiresAt = expiresAt;
        this.status = CampaignPackageStatus.EXPORT_REQUESTED;
    }

    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public CampaignPackageStatus getStatus() { return status; }
    public String getR2ObjectKey() { return r2ObjectKey; }
    public String getExportUrl() { return exportUrl; }
    public Instant getExportUrlExpiresAt() { return exportUrlExpiresAt; }

    private static UUID require(UUID value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " must not be null");
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
