package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateCategory;
import com.lebhas.creativesaas.campaignpackage.domain.CreativeTemplateStatus;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreativeTemplateApiRequest(
        @NotBlank String name,
        CreativeTemplateCategory category,
        String description,
        PromptPlatform platform,
        PromptLanguage language,
        CampaignObjective campaignObjective,
        Map<String, Object> templatePayload,
        CreativeTemplateStatus status
) {
}
