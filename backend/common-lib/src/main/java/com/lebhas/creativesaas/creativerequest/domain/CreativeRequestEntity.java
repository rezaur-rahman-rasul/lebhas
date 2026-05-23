package com.lebhas.creativesaas.creativerequest.domain;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "creative_requests", schema = "platform")
public class CreativeRequestEntity extends TenantAwareEntity {

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "product_service_id")
    private UUID productServiceId;

    @Column(name = "project_campaign_id", nullable = false, updatable = false)
    private UUID projectCampaignId;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "request_name", nullable = false, length = 180)
    private String title;

    @Column(name = "source_prompt", nullable = false, columnDefinition = "TEXT")
    private String sourcePrompt;

    @Column(name = "enhanced_prompt", columnDefinition = "TEXT")
    private String enhancedPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_preference", nullable = false, length = 20)
    private BrandLanguagePreference languagePreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_platform", length = 120)
    private PromptPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_type", length = 40)
    private CreativeType creativeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "creative_objective", length = 160)
    private CampaignObjective campaignObjective;

    @Column(name = "campaign_tone", length = 160)
    private String campaignTone;

    @Column(name = "target_audience", length = 240)
    private String targetAudience;

    @Column(name = "cta_preference", length = 160)
    private String ctaPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CreativeRequestStatus status;

    @Column(name = "requested_versions", nullable = false)
    private int requestedVersions;

    @Column(name = "generated_version_count", nullable = false)
    private int generatedVersionCount;

    @Column(name = "generation_started_at")
    private Instant generationStartedAt;

    @Column(name = "generation_completed_at")
    private Instant generationCompletedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "requested_format", length = 120)
    private String requestedFormat;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_asset_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> selectedAssetIds = new ArrayList<>();

    @Column(name = "credit_reservation_id")
    private UUID creditReservationId;

    protected CreativeRequestEntity() {
    }

    public static CreativeRequestEntity create(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectCampaignId,
            UUID createdByUserId,
            String title,
            String sourcePrompt,
            String enhancedPrompt,
            BrandLanguagePreference languagePreference,
            PromptPlatform platform,
            CreativeType creativeType,
            CampaignObjective campaignObjective,
            String campaignTone,
            String targetAudience,
            String ctaPreference,
            int requestedVersions
    ) {
        return create(
                workspaceId,
                brandId,
                productServiceId,
                projectCampaignId,
                createdByUserId,
                title,
                sourcePrompt,
                enhancedPrompt,
                languagePreference,
                platform,
                creativeType,
                campaignObjective,
                campaignTone,
                targetAudience,
                ctaPreference,
                CreativeRequestStatus.DRAFT,
                requestedVersions);
    }

    public static CreativeRequestEntity create(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectCampaignId,
            UUID createdByUserId,
            String title,
            String sourcePrompt,
            String enhancedPrompt,
            BrandLanguagePreference languagePreference,
            PromptPlatform platform,
            CreativeType creativeType,
            CampaignObjective campaignObjective,
            String campaignTone,
            String targetAudience,
            String ctaPreference,
            CreativeRequestStatus status,
            int requestedVersions
    ) {
        CreativeRequestEntity entity = new CreativeRequestEntity();
        entity.assignWorkspace(workspaceId);
        entity.brandId = require(brandId, "brandId");
        entity.productServiceId = require(productServiceId, "productServiceId");
        entity.projectCampaignId = require(projectCampaignId, "projectCampaignId");
        entity.createdByUserId = require(createdByUserId, "createdByUserId");
        entity.title = normalizeRequired(title, "title");
        entity.sourcePrompt = normalizeRequired(sourcePrompt, "sourcePrompt");
        entity.enhancedPrompt = normalizeNullable(enhancedPrompt);
        entity.languagePreference = normalizeLanguagePreference(languagePreference);
        entity.platform = platform;
        entity.creativeType = creativeType;
        entity.campaignObjective = campaignObjective;
        entity.campaignTone = normalizeNullable(campaignTone);
        entity.targetAudience = normalizeNullable(targetAudience);
        entity.ctaPreference = normalizeNullable(ctaPreference);
        entity.status = status == null ? CreativeRequestStatus.DRAFT : status;
        entity.requestedVersions = normalizeRequestedVersions(requestedVersions);
        entity.generatedVersionCount = 0;
        entity.requestedFormat = defaultRequestedFormat(creativeType);
        entity.selectedAssetIds = new ArrayList<>();
        return entity;
    }

    public static CreativeRequestEntity create(
            UUID workspaceId,
            UUID projectCampaignId,
            UUID requestedBy,
            String requestName,
            String sourcePrompt,
            String enhancedPrompt,
            String creativeObjective,
            String targetPlatform,
            String requestedFormat,
            Collection<UUID> selectedAssetIds,
            UUID creditReservationId
    ) {
        return create(
                workspaceId,
                projectCampaignId,
                requestedBy,
                requestName,
                sourcePrompt,
                enhancedPrompt,
                creativeObjective,
                targetPlatform,
                requestedFormat,
                selectedAssetIds,
                CreativeRequestStatus.DRAFT,
                creditReservationId);
    }

    public static CreativeRequestEntity create(
            UUID workspaceId,
            UUID projectCampaignId,
            UUID requestedBy,
            String requestName,
            String sourcePrompt,
            String enhancedPrompt,
            String creativeObjective,
            String targetPlatform,
            String requestedFormat,
            Collection<UUID> selectedAssetIds,
            CreativeRequestStatus status,
            UUID creditReservationId
    ) {
        CreativeRequestEntity entity = new CreativeRequestEntity();
        entity.assignWorkspace(workspaceId);
        entity.projectCampaignId = require(projectCampaignId, "projectCampaignId");
        entity.createdByUserId = require(requestedBy, "requestedBy");
        entity.title = normalizeRequired(requestName, "requestName");
        entity.sourcePrompt = normalizeRequired(sourcePrompt, "sourcePrompt");
        entity.enhancedPrompt = normalizeNullable(enhancedPrompt);
        entity.languagePreference = BrandLanguagePreference.BOTH;
        entity.platform = parseEnum(targetPlatform, PromptPlatform.class);
        entity.creativeType = inferCreativeType(requestedFormat);
        entity.campaignObjective = parseEnum(creativeObjective, CampaignObjective.class);
        entity.campaignTone = null;
        entity.targetAudience = null;
        entity.ctaPreference = null;
        entity.selectedAssetIds = normalizeAssetIds(selectedAssetIds);
        entity.creditReservationId = creditReservationId;
        entity.requestedFormat = normalizeNullable(requestedFormat);
        entity.status = status == null ? CreativeRequestStatus.DRAFT : status;
        entity.requestedVersions = 1;
        entity.generatedVersionCount = 0;
        return entity;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public UUID getProductServiceId() {
        return productServiceId;
    }

    public UUID getProjectCampaignId() {
        return projectCampaignId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourcePrompt() {
        return sourcePrompt;
    }

    public String getEnhancedPrompt() {
        return enhancedPrompt;
    }

    public BrandLanguagePreference getLanguagePreference() {
        return languagePreference;
    }

    public PromptPlatform getPlatform() {
        return platform;
    }

    public CreativeType getCreativeType() {
        return creativeType;
    }

    public CampaignObjective getCampaignObjective() {
        return campaignObjective;
    }

    public String getCampaignTone() {
        return campaignTone;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public String getCtaPreference() {
        return ctaPreference;
    }

    public CreativeRequestStatus getStatus() {
        return status;
    }

    public int getRequestedVersions() {
        return requestedVersions;
    }

    public int getGeneratedVersionCount() {
        return generatedVersionCount;
    }

    public Instant getGenerationStartedAt() {
        return generationStartedAt;
    }

    public Instant getGenerationCompletedAt() {
        return generationCompletedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRequestedFormat() {
        return requestedFormat;
    }

    public List<UUID> getSelectedAssetIds() {
        return List.copyOf(selectedAssetIds);
    }

    public UUID getCreditReservationId() {
        return creditReservationId;
    }

    public UUID getRequestedBy() {
        return createdByUserId;
    }

    public String getRequestName() {
        return title;
    }

    public String getCreativeObjective() {
        return campaignObjective == null ? null : campaignObjective.name();
    }

    public String getTargetPlatform() {
        return platform == null ? null : platform.name();
    }

    public void replaceSelectedAssetIds(Collection<UUID> selectedAssetIds) {
        this.selectedAssetIds = normalizeAssetIds(selectedAssetIds);
    }

    public void attachCreditReservation(UUID creditReservationId) {
        this.creditReservationId = creditReservationId;
    }

    public void revise(
            UUID brandId,
            UUID productServiceId,
            String title,
            String sourcePrompt,
            String enhancedPrompt,
            BrandLanguagePreference languagePreference,
            PromptPlatform platform,
            CreativeType creativeType,
            CampaignObjective campaignObjective,
            String campaignTone,
            String targetAudience,
            String ctaPreference,
            int requestedVersions,
            String requestedFormat,
            Collection<UUID> selectedAssetIds
    ) {
        this.brandId = require(brandId, "brandId");
        this.productServiceId = require(productServiceId, "productServiceId");
        this.title = normalizeRequired(title, "title");
        this.sourcePrompt = normalizeRequired(sourcePrompt, "sourcePrompt");
        this.enhancedPrompt = normalizeNullable(enhancedPrompt);
        this.languagePreference = normalizeLanguagePreference(languagePreference);
        this.platform = platform;
        this.creativeType = creativeType;
        this.campaignObjective = campaignObjective;
        this.campaignTone = normalizeNullable(campaignTone);
        this.targetAudience = normalizeNullable(targetAudience);
        this.ctaPreference = normalizeNullable(ctaPreference);
        this.requestedVersions = normalizeRequestedVersions(requestedVersions);
        this.requestedFormat = normalizeNullable(requestedFormat);
        this.selectedAssetIds = normalizeAssetIds(selectedAssetIds);
        this.failureReason = null;
    }

    public void updateGeneratedVersionCount(int generatedVersionCount) {
        this.generatedVersionCount = Math.max(generatedVersionCount, 0);
    }

    public void markGenerationStarted(Instant generationStartedAt) {
        this.generationStartedAt = generationStartedAt;
        this.status = CreativeRequestStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markGenerationCompleted(Instant generationCompletedAt, int generatedVersionCount) {
        this.generationCompletedAt = generationCompletedAt;
        this.generatedVersionCount = Math.max(generatedVersionCount, 0);
        this.status = CreativeRequestStatus.COMPLETED;
        this.failureReason = null;
    }

    public void markGenerationFailed(String failureReason, Instant generationCompletedAt) {
        this.failureReason = normalizeNullable(failureReason);
        this.generationCompletedAt = generationCompletedAt;
        this.status = CreativeRequestStatus.FAILED;
    }

    public void queue() {
        this.status = CreativeRequestStatus.QUEUED;
    }

    public void markProcessing() {
        this.status = CreativeRequestStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = CreativeRequestStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = CreativeRequestStatus.FAILED;
    }

    public void markFailed(String failureReason) {
        this.failureReason = normalizeNullable(failureReason);
        this.generationCompletedAt = Instant.now();
        this.status = CreativeRequestStatus.FAILED;
    }

    public void cancel() {
        this.status = CreativeRequestStatus.CANCELLED;
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static int normalizeRequestedVersions(int requestedVersions) {
        if (requestedVersions < 1) {
            throw new IllegalArgumentException("requestedVersions must be greater than zero");
        }
        return requestedVersions;
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

    private static BrandLanguagePreference normalizeLanguagePreference(BrandLanguagePreference languagePreference) {
        return languagePreference == null ? BrandLanguagePreference.BOTH : languagePreference;
    }

    private static CreativeType inferCreativeType(String requestedFormat) {
        CreativeOutputFormat outputFormat = parseEnum(requestedFormat, CreativeOutputFormat.class);
        if (outputFormat == null) {
            return null;
        }
        return outputFormat.isVideo() ? CreativeType.SHORT_VIDEO : CreativeType.STATIC_IMAGE;
    }

    private static String defaultRequestedFormat(CreativeType creativeType) {
        if (creativeType == null) {
            return null;
        }
        return creativeType.isVideo() ? CreativeOutputFormat.MP4.name() : CreativeOutputFormat.PNG.name();
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumType) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static List<UUID> normalizeAssetIds(Collection<UUID> selectedAssetIds) {
        if (selectedAssetIds == null || selectedAssetIds.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<UUID> normalized = new LinkedHashSet<>();
        for (UUID assetId : selectedAssetIds) {
            if (assetId != null) {
                normalized.add(assetId);
            }
        }
        return new ArrayList<>(normalized);
    }
}
