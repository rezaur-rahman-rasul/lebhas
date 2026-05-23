package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.CreativeStyle;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTone;

import java.util.List;
import java.util.UUID;

public record PromptEnhancementCommand(
        UUID workspaceId,
        UUID projectId,
        UUID creativeRequestId,
        String customPrompt,
        List<UUID> assetIds,
        UUID templateId,
        String businessType,
        CampaignObjective campaignObjective,
        PromptPlatform platform,
        CreativeStyle creativeStyle,
        PromptLanguage language,
        PromptTone tone,
        String targetAudience,
        String offerDetails,
        String ctaPreference,
        boolean useBrandProfile,
        String clientIp
) {

    public PromptEnhancementCommand(
            UUID workspaceId,
            UUID projectId,
            String customPrompt,
            List<UUID> assetIds,
            UUID templateId,
            String businessType,
            CampaignObjective campaignObjective,
            PromptPlatform platform,
            CreativeStyle creativeStyle,
            PromptLanguage language,
            PromptTone tone,
            String targetAudience,
            String offerDetails,
            String ctaPreference,
            boolean useBrandProfile,
            String clientIp
    ) {
        this(
                workspaceId,
                projectId,
                null,
                customPrompt,
                assetIds,
                templateId,
                businessType,
                campaignObjective,
                platform,
                creativeStyle,
                language,
                tone,
                targetAudience,
                offerDetails,
                ctaPreference,
                useBrandProfile,
                clientIp);
    }
}
