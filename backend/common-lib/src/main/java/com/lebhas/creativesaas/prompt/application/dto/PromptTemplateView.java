package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;

import java.time.Instant;
import java.util.UUID;

public record PromptTemplateView(
        UUID id,
        UUID workspaceId,
        String name,
        String category,
        String description,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        String businessType,
        PromptLanguage language,
        String templateText,
        boolean isPublic,
        boolean systemDefault,
        PromptTemplateStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
