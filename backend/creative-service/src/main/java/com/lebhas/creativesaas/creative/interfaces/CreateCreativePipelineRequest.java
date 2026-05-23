package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.domain.CreativePipelineStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateCreativePipelineRequest(
        @NotBlank @Size(max = 120) String pipelineCode,
        @NotBlank @Size(max = 180) String pipelineName,
        @Size(max = 4000) String description,
        CreativePipelineStatus status,
        boolean active,
        @Min(1) int version,
        @NotNull Map<String, Object> metadata
) {
}
