package com.lebhas.ai.creative.dto;

import com.lebhas.ai.creative.enums.CreativePlatform;
import com.lebhas.ai.creative.enums.CreativeType;
import com.lebhas.ai.creative.enums.ModelQuality;
import com.lebhas.ai.creative.enums.OutputFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreativeCreditAvailabilityRequest(
        @NotBlank(message = "workspaceId is required")
        String workspaceId,
        @NotBlank(message = "brandId is required")
        String brandId,
        String productServiceId,
        String campaignId,
        @NotNull(message = "platform is required")
        CreativePlatform platform,
        @NotNull(message = "creativeType is required")
        CreativeType creativeType,
        @NotNull(message = "modelQuality is required")
        ModelQuality modelQuality,
        @NotNull(message = "versions is required")
        @Min(value = 1, message = "versions must be greater than or equal to 1")
        Integer versions,
        @NotNull(message = "hasProductImage is required")
        Boolean hasProductImage,
        @NotNull(message = "hasLogoImage is required")
        Boolean hasLogoImage,
        @NotNull(message = "hasReferenceImage is required")
        Boolean hasReferenceImage,
        @NotNull(message = "hasMaskImage is required")
        Boolean hasMaskImage,
        @NotNull(message = "outputFormat is required")
        OutputFormat outputFormat,
        @NotBlank(message = "background is required")
        String background,
        @NotBlank(message = "size is required")
        String size
) {
    public UUID workspaceUuid() {
        return parseUuid(workspaceId, "workspaceId");
    }

    public UUID brandUuid() {
        return parseUuid(brandId, "brandId");
    }

    public UUID productServiceUuid() {
        return parseOptionalUuid(productServiceId, "productServiceId");
    }

    public UUID campaignUuid() {
        return parseOptionalUuid(campaignId, "campaignId");
    }

    @AssertTrue(message = "background must be opaque or transparent")
    public boolean isBackgroundValid() {
        return "opaque".equalsIgnoreCase(background) || "transparent".equalsIgnoreCase(background);
    }

    @AssertTrue(message = "size must be one of 1024x1024, 1024x1536, or 1536x1024")
    public boolean isSizeValid() {
        return "1024x1024".equals(size) || "1024x1536".equals(size) || "1536x1024".equals(size);
    }

    private static UUID parseOptionalUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseUuid(value, field);
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " must be a valid UUID");
        }
    }
}
