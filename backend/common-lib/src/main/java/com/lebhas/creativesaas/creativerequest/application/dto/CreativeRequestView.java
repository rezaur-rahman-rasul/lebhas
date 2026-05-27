package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreativeRequestView(
        UUID id,
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId,
        UUID requestedBy,
        SafeProfileDisplayView requestedByDisplay,
        String requestName,
        String sourcePrompt,
        String enhancedPrompt,
        BrandLanguagePreference languagePreference,
        String creativeType,
        String creativeObjective,
        String targetPlatform,
        String campaignTone,
        String targetAudience,
        String ctaPreference,
        int requestedVersions,
        int generatedVersionCount,
        Instant generationStartedAt,
        Instant generationCompletedAt,
        String failureReason,
        String requestedFormat,
        List<UUID> selectedAssetIds,
        CreativeRequestStatus status,
        UUID creditReservationId,
        Instant createdAt,
        Instant updatedAt
) {
}
