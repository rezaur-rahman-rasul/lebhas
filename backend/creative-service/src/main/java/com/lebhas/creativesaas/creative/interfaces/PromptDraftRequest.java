package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PromptDraftRequest(
        @NotBlank
        @Size(max = 160)
        String title,
        @NotBlank
        @Size(min = 5, max = 5000)
        String promptText,
        PromptLanguage language,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        UUID templateId
) {
}
