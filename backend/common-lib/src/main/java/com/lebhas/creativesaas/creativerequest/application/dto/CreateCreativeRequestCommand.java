package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;

import java.util.List;
import java.util.UUID;

public record CreateCreativeRequestCommand(
        UUID workspaceId,
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

    public CreateCreativeRequestCommand(
            UUID workspaceId,
            UUID projectCampaignId,
            String requestName,
            String sourcePrompt,
            String enhancedPrompt,
            String creativeObjective,
            String targetPlatform,
            String requestedFormat,
            List<UUID> selectedAssetIds
    ) {
        this(
                workspaceId,
                null,
                null,
                projectCampaignId,
                requestName,
                sourcePrompt,
                enhancedPrompt,
                null,
                creativeObjective,
                targetPlatform,
                requestedFormat,
                1,
                selectedAssetIds);
    }
}
