package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SubmitCreativeGenerationCommand(
        UUID workspaceId,
        UUID promptHistoryId,
        String sourcePrompt,
        String enhancedPrompt,
        List<UUID> assetIds,
        CreativeType creativeType,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        Integer width,
        Integer height,
        Long duration,
        Map<String, Object> generationConfig,
        boolean useBrandContext,
        String clientIp
) {
}
