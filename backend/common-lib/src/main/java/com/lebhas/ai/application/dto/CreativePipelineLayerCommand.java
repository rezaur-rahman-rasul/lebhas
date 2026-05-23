package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativeLayerType;

import java.util.Map;

public record CreativePipelineLayerCommand(
        CreativeLayerType layerType,
        String layerCode,
        String layerName,
        int sortOrder,
        boolean enabled,
        boolean required,
        boolean retryable,
        Map<String, Object> configuration
) {
}
