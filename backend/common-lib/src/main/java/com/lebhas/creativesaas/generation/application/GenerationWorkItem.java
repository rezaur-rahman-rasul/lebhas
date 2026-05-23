package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.Map;
import java.util.UUID;

public record GenerationWorkItem(
        UUID jobId,
        UUID workspaceId,
        UUID requestId,
        UUID userId,
        CreativeType creativeType,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        String sourcePrompt,
        String enhancedPrompt,
        String brandContextSnapshot,
        String assetContextSnapshot,
        String generationConfig,
        Map<String, Object> generationConfigMap,
        Integer width,
        Integer height,
        Long duration,
        int attemptCount
) {
    public String providerPrompt() {
        return enhancedPrompt == null || enhancedPrompt.isBlank() ? sourcePrompt : enhancedPrompt;
    }
}
