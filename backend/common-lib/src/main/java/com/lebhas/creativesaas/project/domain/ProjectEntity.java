package com.lebhas.creativesaas.project.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "projects", schema = "platform")
public class ProjectEntity extends TenantAwareEntity {

    @Column(name = "brand_id", nullable = false, updatable = false)
    private UUID brandId;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_platform", length = 40)
    private PromptPlatform targetPlatform;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    protected ProjectEntity() {
    }

    public static ProjectEntity create(
            UUID workspaceId,
            UUID brandId,
            String name,
            String description,
            CampaignObjective campaignObjective,
            PromptPlatform targetPlatform
    ) {
        ProjectEntity project = new ProjectEntity();
        project.assignWorkspace(workspaceId);
        project.brandId = requireBrandId(brandId);
        project.name = normalizeRequired(name);
        project.description = normalizeNullable(description);
        project.campaignObjective = campaignObjective;
        project.targetPlatform = targetPlatform;
        project.status = ProjectStatus.ACTIVE;
        return project;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public PromptPlatform getTargetPlatform() {
        return targetPlatform;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void update(
            String name,
            String description,
            CampaignObjective campaignObjective,
            PromptPlatform targetPlatform
    ) {
        this.name = normalizeRequired(name);
        this.description = normalizeNullable(description);
        this.campaignObjective = campaignObjective;
        this.targetPlatform = targetPlatform;
    }

    public void changeStatus(ProjectStatus status) {
        this.status = status == null ? ProjectStatus.ACTIVE : status;
    }

    private static UUID requireBrandId(UUID brandId) {
        if (brandId == null) {
            throw new IllegalArgumentException("brandId must not be null");
        }
        return brandId;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
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
