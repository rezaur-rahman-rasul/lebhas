package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.CreativeStyle;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTone;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PromptSuggestionCommand(
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
        Set<SuggestionType> suggestionTypes,
        String clientIp
) {
}
