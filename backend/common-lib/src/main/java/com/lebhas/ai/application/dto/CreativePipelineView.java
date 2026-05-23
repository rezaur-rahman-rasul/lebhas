package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativePipelineStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativePipelineView(
        UUID id,
        String pipelineCode,
        String pipelineName,
        String description,
        CreativePipelineStatus status,
        boolean active,
        int version,
        Map<String, Object> metadata,
        List<CreativePipelineLayerView> layers
) {
}
