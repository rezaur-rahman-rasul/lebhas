package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.Map;
import java.util.UUID;

public record CreativeGenerationContext(
        UUID promptHistoryId,
        UUID projectId,
        String sourcePrompt,
        String enhancedPrompt,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeType creativeType,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        Integer width,
        Integer height,
        Long duration,
        String brandContextSnapshot,
        String assetContextSnapshot,
        String generationConfigJson,
        Map<String, Object> generationConfig,
        int assetCount
) {
    public String providerPrompt() {
        return enhancedPrompt == null || enhancedPrompt.isBlank() ? sourcePrompt : enhancedPrompt;
    }
}
