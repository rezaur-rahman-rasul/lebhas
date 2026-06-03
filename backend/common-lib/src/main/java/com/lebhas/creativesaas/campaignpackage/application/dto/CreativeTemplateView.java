package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateCategory;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreativeTemplateView(
        UUID id,
        UUID workspaceId,
        String name,
        CreativeTemplateCategory category,
        String description,
        PromptPlatform platform,
        PromptLanguage language,
        CampaignObjective campaignObjective,
        boolean masterTemplate,
        CreativeTemplateStatus status,
        Map<String, Object> templatePayload,
        Instant createdAt,
        Instant updatedAt
) {
}
