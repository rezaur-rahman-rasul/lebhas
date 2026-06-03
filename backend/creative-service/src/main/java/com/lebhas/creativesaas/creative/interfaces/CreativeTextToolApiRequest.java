package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.texttool.domain.CreativeTextQualityMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreativeTextToolApiRequest(
        UUID brandId,
        UUID productServiceId,
        @NotNull PromptPlatform platform,
        @NotNull PromptLanguage language,
        @Size(max = 120) String tone,
        CampaignObjective campaignObjective,
        @Size(max = 2000) String sourceIdea,
        CreativeTextQualityMode qualityMode,
        List<UUID> selectedAssetIds
) {
}
