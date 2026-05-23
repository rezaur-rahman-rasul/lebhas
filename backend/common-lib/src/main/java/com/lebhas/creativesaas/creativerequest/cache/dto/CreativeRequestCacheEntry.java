package com.lebhas.creativesaas.creativerequest.cache.dto;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreativeRequestCacheEntry(
        UUID id,
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
        int requestedVersions,
        int generatedVersionCount,
        Instant generationStartedAt,
        Instant generationCompletedAt,
        String failureReason,
        String requestedFormat,
        List<UUID> selectedAssetIds,
        UUID creditReservationId,
        Instant createdAt,
        Instant updatedAt
) {

    public static CreativeRequestCacheEntry from(CreativeRequestEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CreativeRequestCacheEntry(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getBrandId(),
                entity.getProductServiceId(),
                entity.getProjectCampaignId(),
                entity.getCreatedByUserId(),
                entity.getTitle(),
                entity.getSourcePrompt(),
                entity.getEnhancedPrompt(),
                entity.getLanguagePreference(),
                entity.getPlatform(),
                entity.getCreativeType(),
                entity.getCampaignObjective(),
                entity.getCampaignTone(),
                entity.getTargetAudience(),
                entity.getCtaPreference(),
                entity.getStatus(),
                entity.getRequestedVersions(),
                entity.getGeneratedVersionCount(),
                entity.getGenerationStartedAt(),
                entity.getGenerationCompletedAt(),
                entity.getFailureReason(),
                entity.getRequestedFormat(),
                entity.getSelectedAssetIds(),
                entity.getCreditReservationId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static CreativeRequestCacheEntry from(CreativeRequestView view) {
        if (view == null) {
            return null;
        }
        return new CreativeRequestCacheEntry(
                view.id(),
                view.workspaceId(),
                view.brandId(),
                view.productServiceId(),
                view.projectCampaignId(),
                view.requestedBy(),
                view.requestName(),
                view.sourcePrompt(),
                view.enhancedPrompt(),
                view.languagePreference(),
                view.targetPlatform() == null ? null : PromptPlatform.valueOf(view.targetPlatform()),
                view.creativeType() == null ? null : CreativeType.valueOf(view.creativeType()),
                view.creativeObjective() == null ? null : CampaignObjective.valueOf(view.creativeObjective()),
                view.campaignTone(),
                view.targetAudience(),
                view.ctaPreference(),
                view.status(),
                view.requestedVersions(),
                view.generatedVersionCount(),
                view.generationStartedAt(),
                view.generationCompletedAt(),
                view.failureReason(),
                view.requestedFormat(),
                view.selectedAssetIds(),
                view.creditReservationId(),
                view.createdAt(),
                view.updatedAt());
    }
}
