package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record AssignLayerToolRequest(
        @NotNull UUID providerId,
        UUID modelId,
        UUID capabilityId,
        @NotBlank @Size(max = 120) String mappingCode,
        @Min(1) int priorityOrder,
        @Min(0) int routingWeight,
        boolean enabled,
        boolean fallbackEligible,
        @NotNull Map<String, Object> routingMetadata
) {
}
