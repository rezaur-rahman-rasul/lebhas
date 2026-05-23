package com.lebhas.creativesaas.generation.provider;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.Map;
import java.util.UUID;

public record AiGenerationRequest(
        UUID workspaceId,
        UUID requestId,
        CreativeType creativeType,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        String prompt,
        String brandContextSnapshot,
        String assetContextSnapshot,
        Map<String, Object> generationConfig,
        Integer width,
        Integer height,
        Long duration
) {
}
