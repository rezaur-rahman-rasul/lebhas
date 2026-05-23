package com.lebhas.creativesaas.prompt.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "prompt_templates", schema = "platform")
public class PromptTemplateEntity extends BaseEntity {

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Column(name = "business_type", length = 80)
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 30)
    private PromptLanguage language;

    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    private String templateBody;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_system_default", nullable = false)
    private boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PromptTemplateStatus status;

    protected PromptTemplateEntity() {
    }

    public static PromptTemplateEntity create(
            UUID workspaceId,
            String name,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String templateText,
            boolean systemDefault,
            PromptTemplateStatus status
    ) {
        return create(
                workspaceId,
                name,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                templateText,
                false,
                systemDefault,
                status);
    }

    public static PromptTemplateEntity create(
            UUID workspaceId,
            String name,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String templateText,
            boolean isPublic,
            boolean systemDefault,
            PromptTemplateStatus status
    ) {
        return create(
                workspaceId,
                name,
                null,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                templateText,
                isPublic,
                systemDefault,
                status);
    }

    public static PromptTemplateEntity create(
            UUID workspaceId,
            String name,
            String category,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String promptBody,
            boolean isDefault,
            PromptTemplateStatus status
    ) {
        return create(
                workspaceId,
                name,
                category,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                promptBody,
                false,
                isDefault,
                status);
    }

    public static PromptTemplateEntity create(
            UUID workspaceId,
            String name,
            String category,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String promptBody,
            boolean isPublic,
            boolean isDefault,
            PromptTemplateStatus status
    ) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.workspaceId = isDefault ? null : workspaceId;
        entity.name = normalizeRequired(name);
        entity.category = normalizeNullable(category);
        entity.description = normalizeNullable(description);
        entity.platform = platform;
        entity.campaignObjective = campaignObjective;
        entity.businessType = normalizeNullable(businessType);
        entity.language = language;
        entity.templateBody = normalizeRequired(promptBody);
        entity.isPublic = isPublic || isDefault;
        entity.isDefault = isDefault;
        entity.status = status == null ? PromptTemplateStatus.ACTIVE : status;
        return entity;
    }

    public static PromptTemplateEntity create(
            UUID workspaceId,
            String name,
            String category,
            PromptLanguage language,
            String templateBody,
            boolean isPublic
    ) {
        return create(
                workspaceId,
                name,
                category,
                null,
                null,
                null,
                null,
                language,
                templateBody,
                isPublic,
                false,
                PromptTemplateStatus.ACTIVE);
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public String getBusinessType() {
        return businessType;
    }

    public PromptLanguage getLanguage() {
        return language;
    }

    public String getPromptBody() {
        return templateBody;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public String getTemplateText() {
        return templateBody;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isSystemDefault() {
        return isDefault;
    }

    public PromptTemplateStatus getStatus() {
        return status;
    }

    public boolean isActiveTemplate() {
        return status == PromptTemplateStatus.ACTIVE && !isDeleted();
    }

    public boolean isAccessibleInWorkspace(UUID requestedWorkspaceId) {
        return isDefault || (workspaceId != null && workspaceId.equals(requestedWorkspaceId));
    }

    public void update(
            UUID workspaceId,
            String name,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String templateText,
            boolean systemDefault,
            PromptTemplateStatus status
    ) {
        update(
                workspaceId,
                name,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                templateText,
                false,
                systemDefault,
                status);
    }

    public void update(
            UUID workspaceId,
            String name,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String templateText,
            boolean isPublic,
            boolean systemDefault,
            PromptTemplateStatus status
    ) {
        update(
                workspaceId,
                name,
                this.category,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                templateText,
                isPublic,
                systemDefault,
                status);
    }

    public void update(
            UUID workspaceId,
            String name,
            String category,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String promptBody,
            boolean isDefault,
            PromptTemplateStatus status
    ) {
        update(
                workspaceId,
                name,
                category,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                promptBody,
                false,
                isDefault,
                status);
    }

    public void update(
            UUID workspaceId,
            String name,
            String category,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String promptBody,
            boolean isPublic,
            boolean isDefault,
            PromptTemplateStatus status
    ) {
        this.workspaceId = isDefault ? null : workspaceId;
        this.name = normalizeRequired(name);
        this.category = normalizeNullable(category);
        this.description = normalizeNullable(description);
        this.platform = platform;
        this.campaignObjective = campaignObjective;
        this.businessType = normalizeNullable(businessType);
        this.language = language;
        this.templateBody = normalizeRequired(promptBody);
        this.isPublic = isPublic || isDefault;
        this.isDefault = isDefault;
        this.status = status == null ? PromptTemplateStatus.ACTIVE : status;
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
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
