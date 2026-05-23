package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;

import java.util.UUID;

public record PromptTemplateFilter(
        UUID workspaceId,
        String category,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        PromptLanguage language,
        String businessType,
        PromptTemplateStatus status,
        String search,
        Boolean systemDefault,
        boolean includeSystemDefaults
) {
}
