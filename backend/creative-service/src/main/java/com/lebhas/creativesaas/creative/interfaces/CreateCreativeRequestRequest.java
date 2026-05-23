package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request body for creating a creative generation request.")
public record CreateCreativeRequestRequest(
        @Schema(description = "Human-readable request name.", example = "Eid Facebook Banner")
        @NotBlank
        @Size(max = 180)
        String requestName,

        @Schema(description = "Original user prompt.", example = "Luxury panjabi promotion for Eid collection")
        @NotBlank
        @Size(max = 32000)
        String sourcePrompt,

        @Schema(description = "Optional enhanced prompt from prompt intelligence.", example = "Create a premium Facebook ad visual for an Eid panjabi collection...")
        @Size(max = 32000)
        String enhancedPrompt,

        @Schema(description = "Requested language preference. If omitted, the brand language preference is applied.")
        BrandLanguagePreference languagePreference,

        @Schema(description = "Creative objective.", example = "CONVERSIONS")
        @NotBlank
        @Size(max = 160)
        String creativeObjective,

        @Schema(description = "Target platform.", example = "FACEBOOK")
        @NotBlank
        @Size(max = 120)
        String targetPlatform,

        @Schema(description = "Requested output format.", example = "IMAGE_PORTRAIT")
        @NotBlank
        @Size(max = 120)
        String requestedFormat,

        @Schema(description = "Number of generated versions requested. Validated against the active plan feature policy.")
        @Min(1)
        Integer requestedVersions,

        @Schema(description = "Selected project asset identifiers.")
        List<@NotNull UUID> selectedAssetIds
) {
}
