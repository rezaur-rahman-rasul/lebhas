package com.lebhas.creativesaas.creative.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request body for creating a creative request from a saved prompt draft.")
public record CreateCreativeRequestFromPromptRequest(
        @NotNull
        UUID promptDraftId,

        @Size(max = 180)
        String requestName,

        @Size(max = 32000)
        String enhancedPrompt,

        @Size(max = 120)
        String requestedFormat,

        @Min(1)
        Integer requestedVersions,

        List<@NotNull UUID> selectedAssetIds
) {
}
