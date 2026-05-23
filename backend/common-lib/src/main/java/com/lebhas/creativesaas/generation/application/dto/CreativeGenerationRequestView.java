package com.lebhas.creativesaas.generation.application.dto;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativeGenerationRequestView(
        UUID id,
        UUID workspaceId,
        UUID userId,
        UUID promptHistoryId,
        String sourcePrompt,
        String enhancedPrompt,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeType creativeType,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        Map<String, Object> brandContextSnapshot,
        List<Map<String, Object>> assetContextSnapshot,
        Map<String, Object> generationConfig,
        CreativeGenerationStatus status,
        String aiProvider,
        String aiModel,
        Instant requestedAt,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
