package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativePipelineStatus;

import java.util.Map;

public record CreativePipelineCommand(
        String pipelineCode,
        String pipelineName,
        String description,
        CreativePipelineStatus status,
        boolean active,
        int version,
        Map<String, Object> metadata
) {
}
