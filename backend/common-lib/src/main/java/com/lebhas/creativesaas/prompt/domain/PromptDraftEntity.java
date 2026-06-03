package com.lebhas.creativesaas.prompt.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "prompt_drafts", schema = "platform")
public class PromptDraftEntity extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 30)
    private PromptLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Column(name = "template_id")
    private UUID templateId;

    protected PromptDraftEntity() {
    }

    public static PromptDraftEntity create(
            UUID workspaceId,
            UUID projectId,
            UUID createdByUserId,
            String title,
            String promptText,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            UUID templateId
    ) {
        PromptDraftEntity entity = new PromptDraftEntity();
        entity.assignWorkspace(workspaceId);
        entity.projectId = require(projectId, "projectId");
        entity.createdByUserId = require(createdByUserId, "createdByUserId");
        entity.update(title, promptText, language, platform, campaignObjective, templateId);
        return entity;
    }

    public void update(
            String title,
            String promptText,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            UUID templateId
    ) {
        this.title = normalizeRequired(title, "title");
        this.promptText = normalizeRequired(promptText, "promptText");
        this.language = language;
        this.platform = platform;
        this.campaignObjective = campaignObjective;
        this.templateId = templateId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getPromptText() {
        return promptText;
    }

    public PromptLanguage getLanguage() {
        return language;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public UUID getTemplateId() {
        return templateId;
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
