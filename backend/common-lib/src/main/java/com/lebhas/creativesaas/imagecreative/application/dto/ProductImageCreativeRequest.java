package com.lebhas.creativesaas.imagecreative.application.dto;

import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeFormat;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;

import java.util.UUID;

public record ProductImageCreativeRequest(
        UUID promptDraftId,
        String sourcePrompt,
        UUID productAssetId,
        UUID logoAssetId,
        ImageCreativeFormat creativeFormat,
        PromptPlatform platform,
        PromptLanguage language,
        ImageCreativeQualityMode qualityMode,
        Integer requestedVersionCount,
        String stylePreset,
        String backgroundStyle,
        String headline,
        String subheadline,
        String offerText,
        String cta,
        Boolean includeCta,
        Boolean includeTypography
) {
}
