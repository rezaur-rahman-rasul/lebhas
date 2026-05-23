package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePromptTemplateRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String name,
        @Size(max = 80)
        String category,
        @Size(max = 500)
        String description,
        @NotNull(message = ValidationMessages.REQUIRED)
        PromptPlatform platform,
        @NotNull(message = ValidationMessages.REQUIRED)
        CampaignObjective campaignObjective,
        @Size(max = 80)
        String businessType,
        @NotNull(message = ValidationMessages.REQUIRED)
        PromptLanguage language,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(min = 5, max = 5000)
        String templateText,
        boolean systemDefault,
        PromptTemplateStatus status
) {
}
