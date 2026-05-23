package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.domain.CreativeLayerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpsertCreativePipelineLayerRequest(
        @NotNull CreativeLayerType layerType,
        @NotBlank @Size(max = 120) String layerCode,
        @NotBlank @Size(max = 180) String layerName,
        @Min(1) int sortOrder,
        boolean enabled,
        boolean required,
        boolean retryable,
        @NotNull Map<String, Object> configuration
) {
}
