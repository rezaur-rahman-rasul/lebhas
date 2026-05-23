package com.lebhas.creativesaas.prompt.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;

import java.util.UUID;

public record CreatePromptTemplateCommand(
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
        PromptTemplateStatus status
) {

    public CreatePromptTemplateCommand(
            UUID workspaceId,
            String name,
            String category,
            String description,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            PromptLanguage language,
            String templateText,
            boolean systemDefault,
            PromptTemplateStatus status
    ) {
        this(
                workspaceId,
                name,
                category,
                description,
                platform,
                campaignObjective,
                businessType,
                language,
                templateText,
                false,
                systemDefault,
                status);
    }
}
