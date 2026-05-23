package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.creativerequest.cache.dto.CreativeRequestCacheEntry;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class CreativeRequestViewMapper {

    public CreativeRequestView toView(CreativeRequestEntity entity) {
        return new CreativeRequestView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getBrandId(),
                entity.getProductServiceId(),
                entity.getProjectCampaignId(),
                entity.getRequestedBy(),
                entity.getRequestName(),
                entity.getSourcePrompt(),
                entity.getEnhancedPrompt(),
                entity.getLanguagePreference(),
                entity.getCreativeType() == null ? null : entity.getCreativeType().name(),
                entity.getCreativeObjective(),
                entity.getTargetPlatform(),
                entity.getCampaignTone(),
                entity.getTargetAudience(),
                entity.getCtaPreference(),
                entity.getRequestedVersions(),
                entity.getGeneratedVersionCount(),
                entity.getGenerationStartedAt(),
                entity.getGenerationCompletedAt(),
                entity.getFailureReason(),
                entity.getRequestedFormat(),
                entity.getSelectedAssetIds(),
                entity.getStatus(),
                entity.getCreditReservationId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public CreativeRequestView toView(CreativeRequestCacheEntry entry) {
        return new CreativeRequestView(
                entry.id(),
                entry.workspaceId(),
                entry.brandId(),
                entry.productServiceId(),
                entry.projectCampaignId(),
                entry.createdByUserId(),
                entry.title(),
                entry.sourcePrompt(),
                entry.enhancedPrompt(),
                entry.languagePreference(),
                entry.creativeType() == null ? null : entry.creativeType().name(),
                entry.campaignObjective() == null ? null : entry.campaignObjective().name(),
                entry.platform() == null ? null : entry.platform().name(),
                entry.campaignTone(),
                entry.targetAudience(),
                entry.ctaPreference(),
                entry.requestedVersions(),
                entry.generatedVersionCount(),
                entry.generationStartedAt(),
                entry.generationCompletedAt(),
                entry.failureReason(),
                entry.requestedFormat(),
                entry.selectedAssetIds(),
                entry.status(),
                entry.creditReservationId(),
                entry.createdAt(),
                entry.updatedAt());
    }
}
