package com.lebhas.creativesaas.campaign.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "project_campaigns", schema = "platform")
public class ProjectCampaignEntity extends TenantAwareEntity {

    @Column(name = "brand_id", nullable = false, updatable = false)
    private UUID brandId;

    @Column(name = "product_service_id", nullable = false, updatable = false)
    private UUID productServiceId;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "campaign_objective", length = 160)
    private String campaignObjective;

    @Column(name = "target_platform", length = 120)
    private String targetPlatform;

    @Column(name = "campaign_type", length = 120)
    private String campaignType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectCampaignStatus status;

    protected ProjectCampaignEntity() {
    }

    public static ProjectCampaignEntity create(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID createdByUserId,
            String name,
            String description,
            String campaignObjective,
            String targetPlatform,
            String campaignType
    ) {
        ProjectCampaignEntity entity = new ProjectCampaignEntity();
        entity.assignWorkspace(workspaceId);
        entity.brandId = require(brandId, "brandId");
        entity.productServiceId = require(productServiceId, "productServiceId");
        entity.createdByUserId = require(createdByUserId, "createdByUserId");
        entity.name = normalizeRequired(name, "name");
        entity.description = normalizeNullable(description);
        entity.campaignObjective = normalizeNullable(campaignObjective);
        entity.targetPlatform = normalizeNullable(targetPlatform);
        entity.campaignType = normalizeNullable(campaignType);
        entity.status = ProjectCampaignStatus.ACTIVE;
        return entity;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public UUID getProductServiceId() {
        return productServiceId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCampaignObjective() {
        return campaignObjective;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public String getCampaignType() {
        return campaignType;
    }

    public ProjectCampaignStatus getStatus() {
        return status;
    }

    public void update(
            String name,
            String description,
            String campaignObjective,
            String targetPlatform,
            String campaignType
    ) {
        this.name = normalizeRequired(name, "name");
        this.description = normalizeNullable(description);
        this.campaignObjective = normalizeNullable(campaignObjective);
        this.targetPlatform = normalizeNullable(targetPlatform);
        this.campaignType = normalizeNullable(campaignType);
    }

    public void changeStatus(ProjectCampaignStatus status) {
        this.status = status == null ? ProjectCampaignStatus.ACTIVE : status;
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

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
