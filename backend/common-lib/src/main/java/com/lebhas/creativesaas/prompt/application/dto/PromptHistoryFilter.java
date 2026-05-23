package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryStatus;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;

import java.time.Instant;
import java.util.UUID;

public record PromptHistoryFilter(
        UUID workspaceId,
        UUID projectId,
        UUID userId,
        SuggestionType suggestionType,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        PromptHistoryStatus status,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int size
) {
}
