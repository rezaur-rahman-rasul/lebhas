package com.lebhas.creativesaas.texttool.application.dto;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.texttool.domain.CreativeTextQualityMode;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreativeTextToolOutputView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID brandId,
        UUID productServiceId,
        CreativeTextToolType toolType,
        String toolCode,
        CreativeTextQualityMode qualityMode,
        PromptPlatform platform,
        PromptLanguage language,
        String tone,
        CampaignObjective campaignObjective,
        String sourceIdea,
        BigDecimal creditCost,
        UUID creditReservationId,
        Map<String, Object> output,
        Instant createdAt
) {
}
