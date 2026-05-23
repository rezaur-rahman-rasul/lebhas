package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Submits an asynchronous creative generation request. STATIC_IMAGE is fully implemented; video types are queued through the provider abstraction foundation.")
public record CreateCreativeGenerationRequest(
        UUID promptHistoryId,
        @Size(max = 32000)
        String sourcePrompt,
        @Size(max = 32000)
        String enhancedPrompt,
        @Size(max = 12)
        List<UUID> assetIds,
        CreativeType creativeType,
        PromptPlatform platform,
        CampaignObjective campaignObjective,
        CreativeOutputFormat outputFormat,
        PromptLanguage language,
        @Min(256)
        @Max(4096)
        Integer width,
        @Min(256)
        @Max(4096)
        Integer height,
        @Min(1)
        @Max(90)
        Long duration,
        Map<String, Object> generationConfig,
        boolean useBrandContext
) {
}
