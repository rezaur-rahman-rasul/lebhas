package com.lebhas.creativesaas.texttool.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.texttool.domain.CreativeTextQualityMode;

import java.util.List;
import java.util.UUID;

public record CreativeTextToolRequest(
        UUID brandId,
        UUID productServiceId,
        PromptPlatform platform,
        PromptLanguage language,
        String tone,
        CampaignObjective campaignObjective,
        String sourceIdea,
        CreativeTextQualityMode qualityMode,
        List<UUID> selectedAssetIds
) {
}
