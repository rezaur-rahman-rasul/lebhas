package com.lebhas.ai.creative.dto;

import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.CreativeTone;
import com.lebhas.ai.creative.enums.CreativeType;
import com.lebhas.ai.creative.enums.ModelQuality;
import com.lebhas.ai.creative.enums.OutputFormat;

import java.util.UUID;

public record TextCreativeRequest(
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID campaignId,
        CreativePlatform platform,
        CreativeType creativeType,
        String headline,
        String cta,
        String brandName,
        String productDescription,
        String targetAudience,
        String campaignObjective,
        String language,
        CreativeTone tone,
        ModelQuality modelQuality,
        Boolean noHumanModel,
        String size,
        CreativeQuality quality,
        OutputFormat outputFormat,
        String background
) {
}
