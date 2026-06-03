package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.UUID;

public record PromptDraftCommand(
        UUID workspaceId,
        UUID projectId,
        String title,
        String promptText,
        PromptLanguage language,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        UUID templateId
) {
}
