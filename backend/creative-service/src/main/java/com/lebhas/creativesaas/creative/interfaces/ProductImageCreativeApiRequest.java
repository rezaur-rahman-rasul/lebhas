package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeFormat;
import com.lebhas.creativesaas.imagecreative.domain.ImageCreativeQualityMode;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductImageCreativeApiRequest(
        UUID promptDraftId,
        @Size(max = 4000) String sourcePrompt,
        UUID productAssetId,
        UUID logoAssetId,
        @NotNull ImageCreativeFormat creativeFormat,
        @NotNull PromptPlatform platform,
        @NotNull PromptLanguage language,
        ImageCreativeQualityMode qualityMode,
        @Min(1) @Max(20) Integer requestedVersionCount,
        @Size(max = 120) String stylePreset,
        @Size(max = 120) String backgroundStyle,
        @Size(max = 1000) String headline,
        @Size(max = 1000) String subheadline,
        @Size(max = 1000) String offerText,
        @Size(max = 160) String cta,
        Boolean includeCta,
        Boolean includeTypography
) {
}
