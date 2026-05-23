package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;

import java.util.List;
import java.util.UUID;

public record UpdateCreativeRequestCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID brandId,
        UUID productServiceId,
        UUID projectCampaignId,
        String requestName,
        String sourcePrompt,
        String enhancedPrompt,
        BrandLanguagePreference languagePreference,
        String creativeObjective,
        String targetPlatform,
        String requestedFormat,
        Integer requestedVersions,
        List<UUID> selectedAssetIds
) {
}
