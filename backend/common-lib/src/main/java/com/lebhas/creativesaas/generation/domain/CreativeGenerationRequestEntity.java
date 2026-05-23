package com.lebhas.creativesaas.generation.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creative_generation_requests", schema = "platform")
public class CreativeGenerationRequestEntity extends TenantAwareEntity {

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "prompt_history_id")
    private UUID promptHistoryId;

    @Column(name = "source_prompt", nullable = false, columnDefinition = "TEXT")
    private String sourcePrompt;

    @Column(name = "enhanced_prompt", columnDefinition = "TEXT")
    private String enhancedPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 40)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_objective", nullable = false, length = 40)
    private CampaignObjective campaignObjective;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_type", nullable = false, length = 40)
    private CreativeType creativeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 20)
    private CreativeOutputFormat outputFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 30)
    private PromptLanguage language;

    @Column(name = "brand_context_snapshot", columnDefinition = "TEXT")
    private String brandContextSnapshot;

    @Column(name = "asset_context_snapshot", columnDefinition = "TEXT")
    private String assetContextSnapshot;

    @Column(name = "generation_config", columnDefinition = "TEXT")
    private String generationConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativeGenerationStatus status;

    @Column(name = "ai_provider", length = 60)
    private String aiProvider;

    @Column(name = "ai_model", length = 120)
    private String aiModel;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected CreativeGenerationRequestEntity() {
    }

    public static CreativeGenerationRequestEntity queue(
            UUID workspaceId,
            UUID projectId,
            UUID userId,
            UUID promptHistoryId,
            String sourcePrompt,
            String enhancedPrompt,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            CreativeType creativeType,
            CreativeOutputFormat outputFormat,
            PromptLanguage language,
            String brandContextSnapshot,
            String assetContextSnapshot,
            String generationConfig,
            String aiProvider,
            String aiModel
    ) {
        CreativeGenerationRequestEntity request = new CreativeGenerationRequestEntity();
        request.assignWorkspace(workspaceId);
        request.projectId = requireProjectId(projectId);
        request.userId = userId;
        request.promptHistoryId = promptHistoryId;
        request.sourcePrompt = normalizeRequired(sourcePrompt);
        request.enhancedPrompt = normalizeNullable(enhancedPrompt);
        request.platform = platform;
        request.campaignObjective = campaignObjective;
        request.creativeType = creativeType;
        request.outputFormat = outputFormat;
        request.language = language;
        request.brandContextSnapshot = normalizeNullable(brandContextSnapshot);
        request.assetContextSnapshot = normalizeNullable(assetContextSnapshot);
        request.generationConfig = normalizeNullable(generationConfig);
        request.status = CreativeGenerationStatus.QUEUED;
        request.aiProvider = normalizeNullable(aiProvider);
        request.aiModel = normalizeNullable(aiModel);
        request.requestedAt = Instant.now();
        return request;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getPromptHistoryId() {
        return promptHistoryId;
    }

    public String getSourcePrompt() {
        return sourcePrompt;
    }

    public String getEnhancedPrompt() {
        return enhancedPrompt;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public CreativeType getCreativeType() {
        return creativeType;
    }

    public CreativeOutputFormat getOutputFormat() {
        return outputFormat;
    }

    public PromptLanguage getLanguage() {
        return language;
    }

    public String getBrandContextSnapshot() {
        return brandContextSnapshot;
    }

    public String getAssetContextSnapshot() {
        return assetContextSnapshot;
    }

    public String getGenerationConfig() {
        return generationConfig;
    }

    public CreativeGenerationStatus getStatus() {
        return status;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void markProcessing() {
        this.status = CreativeGenerationStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markCompleted(String aiProvider, String aiModel) {
        this.status = CreativeGenerationStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.failedAt = null;
        this.errorMessage = null;
        this.aiProvider = normalizeNullable(aiProvider);
        this.aiModel = normalizeNullable(aiModel);
    }

    public void markFailed(String errorMessage) {
        this.status = CreativeGenerationStatus.FAILED;
        this.failedAt = Instant.now();
        this.errorMessage = truncate(errorMessage);
    }

    public void markQueuedForRetry(String errorMessage) {
        this.status = CreativeGenerationStatus.QUEUED;
        this.errorMessage = truncate(errorMessage);
    }

    public void markCancelled(String reason) {
        this.status = CreativeGenerationStatus.CANCELLED;
        this.failedAt = Instant.now();
        this.errorMessage = truncate(reason);
    }

    public boolean isTerminal() {
        return status == CreativeGenerationStatus.COMPLETED
                || status == CreativeGenerationStatus.FAILED
                || status == CreativeGenerationStatus.CANCELLED;
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

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private static UUID requireProjectId(UUID projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        return projectId;
    }
}
