package com.lebhas.ai.creative.dto;

import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeQuality;
import com.lebhas.ai.creative.enums.CreativeTone;
import com.lebhas.ai.creative.enums.CreativeType;
import com.lebhas.ai.creative.enums.ModelQuality;
import com.lebhas.ai.creative.enums.OutputFormat;

import java.util.UUID;

public record AiCreativeGenerateRequest(
        UUID workspaceId,
        UUID brandId,
        UUID productServiceId,
        UUID campaignId,
        CreativePlatform platform,
        String language,
        CreativeType creativeType,
        OutputFormat outputFormat,
        CreativeTone tone,
        ModelQuality modelQuality,
        String campaignIdea,
        String headline,
        String subheadline,
        String offerText,
        String cta,
        String campaignObjective,
        String targetAudience,
        String productDescription,
        Boolean includeCta,
        Boolean includeLogo,
        Boolean includeTypography,
        Integer versions,
        UUID existingAssetId,
        UUID logoAssetId,
        Boolean noHumanModel,
        String size,
        CreativeQuality quality,
        String background,
        String promptTitlePreview,
        String generationModeHint,
        UUID requestedByUserId
) {
}
