package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.UUID;

public record PromptDraftView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID createdByUserId,
        String title,
        String promptText,
        PromptLanguage language,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        UUID templateId,
        Instant createdAt,
        Instant updatedAt
) {
}
