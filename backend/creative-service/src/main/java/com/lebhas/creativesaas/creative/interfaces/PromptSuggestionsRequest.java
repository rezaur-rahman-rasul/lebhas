package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.CreativeStyle;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTone;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PromptSuggestionsRequest(
        @Size(min = 5, max = 5000)
        String customPrompt,
        List<UUID> assetIds,
        UUID templateId,
        @Size(max = 80)
        String businessType,
        CampaignObjective campaignObjective,
        PromptPlatform platform,
        CreativeStyle creativeStyle,
        PromptLanguage language,
        PromptTone tone,
        @Size(max = 160)
        String targetAudience,
        @Size(max = 600)
        String offerDetails,
        @Size(max = 120)
        String ctaPreference,
        boolean useBrandProfile,
        Set<SuggestionType> suggestionTypes
) {
}
