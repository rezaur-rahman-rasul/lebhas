package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryStatus;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PromptHistoryView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID userId,
        String sourcePrompt,
        String enhancedPrompt,
        PromptLanguage language,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        String businessType,
        Map<String, Object> brandContextSnapshot,
        SuggestionType suggestionType,
        String aiProvider,
        String aiModel,
        Integer tokenUsage,
        PromptHistoryStatus status,
        Instant createdAt
) {
}
