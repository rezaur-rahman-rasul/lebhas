package com.lebhas.creativesaas.prompt.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "prompt_history", schema = "platform")
public class PromptHistoryEntity extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "project_campaign_id")
    private UUID projectCampaignId;

    @Column(name = "creative_request_id")
    private UUID creativeRequestId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "source_prompt", nullable = false, columnDefinition = "TEXT")
    private String sourcePrompt;

    @Column(name = "enhanced_prompt", columnDefinition = "TEXT")
    private String enhancedPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 30)
    private PromptLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", length = 40)
    private CampaignObjective campaignObjective;

    @Column(name = "business_type", length = 80)
    private String businessType;

    @Column(name = "brand_context_snapshot", columnDefinition = "TEXT")
    private String brandContextSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 40)
    private SuggestionType suggestionType;

    @Column(name = "ai_provider", length = 60)
    private String aiProvider;

    @Column(name = "ai_model", length = 120)
    private String model;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PromptHistoryStatus status;

    protected PromptHistoryEntity() {
    }

    public static PromptHistoryEntity success(
            UUID workspaceId,
            UUID projectId,
            UUID userId,
            String sourcePrompt,
            String outputPayload,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel,
            Integer tokenUsage
    ) {
        return success(
                workspaceId,
                projectId,
                null,
                null,
                userId,
                sourcePrompt,
                outputPayload,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel,
                tokenUsage);
    }

    public static PromptHistoryEntity success(
            UUID workspaceId,
            UUID projectId,
            UUID projectCampaignId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            String outputPayload,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel,
            Integer tokenUsage
    ) {
        PromptHistoryEntity entity = new PromptHistoryEntity();
        entity.assignWorkspace(workspaceId);
        entity.projectId = requireProjectId(projectId);
        entity.projectCampaignId = projectCampaignId;
        entity.creativeRequestId = creativeRequestId;
        entity.userId = userId;
        entity.sourcePrompt = normalizeRequired(sourcePrompt);
        entity.enhancedPrompt = normalizeNullable(outputPayload);
        entity.language = language;
        entity.platform = platform;
        entity.campaignObjective = campaignObjective;
        entity.businessType = normalizeNullable(businessType);
        entity.brandContextSnapshot = normalizeNullable(brandContextSnapshot);
        entity.suggestionType = suggestionType;
        entity.aiProvider = normalizeNullable(aiProvider);
        entity.model = normalizeNullable(aiModel);
        entity.tokenUsage = tokenUsage;
        entity.status = PromptHistoryStatus.SUCCEEDED;
        return entity;
    }

    public static PromptHistoryEntity failure(
            UUID workspaceId,
            UUID projectId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel
    ) {
        return failure(
                workspaceId,
                projectId,
                null,
                null,
                userId,
                sourcePrompt,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel);
    }

    public static PromptHistoryEntity failure(
            UUID workspaceId,
            UUID projectId,
            UUID projectCampaignId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel
    ) {
        PromptHistoryEntity entity = new PromptHistoryEntity();
        entity.assignWorkspace(workspaceId);
        entity.projectId = requireProjectId(projectId);
        entity.projectCampaignId = projectCampaignId;
        entity.creativeRequestId = creativeRequestId;
        entity.userId = userId;
        entity.sourcePrompt = normalizeRequired(sourcePrompt);
        entity.language = language;
        entity.platform = platform;
        entity.campaignObjective = campaignObjective;
        entity.businessType = normalizeNullable(businessType);
        entity.brandContextSnapshot = normalizeNullable(brandContextSnapshot);
        entity.suggestionType = suggestionType;
        entity.aiProvider = normalizeNullable(aiProvider);
        entity.model = normalizeNullable(aiModel);
        entity.status = PromptHistoryStatus.FAILED;
        return entity;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getProjectCampaignId() {
        return projectCampaignId;
    }

    public UUID getCreativeRequestId() {
        return creativeRequestId;
    }

    public String getSourcePrompt() {
        return sourcePrompt;
    }

    public String getEnhancedPrompt() {
        return enhancedPrompt;
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

    public String getBusinessType() {
        return businessType;
    }

    public String getBrandContextSnapshot() {
        return brandContextSnapshot;
    }

    public SuggestionType getSuggestionType() {
        return suggestionType;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getModel() {
        return model;
    }

    public String getAiModel() {
        return model;
    }

    public Integer getTokenUsage() {
        return tokenUsage;
    }

    public PromptHistoryStatus getStatus() {
        return status;
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static UUID requireProjectId(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        return projectId;
    }
}
