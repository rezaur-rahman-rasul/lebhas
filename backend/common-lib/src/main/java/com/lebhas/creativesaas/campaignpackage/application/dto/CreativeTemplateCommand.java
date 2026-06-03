package com.lebhas.creativesaas.campaignpackage.application.dto;

import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateCategory;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.Map;
import java.util.UUID;

public record CreativeTemplateCommand(
        UUID workspaceId,
        String name,
        CreativeTemplateCategory category,
        String description,
        PromptPlatform platform,
        PromptLanguage language,
        CampaignObjective campaignObjective,
        Map<String, Object> templatePayload,
        CreativeTemplateStatus status
) {
}
