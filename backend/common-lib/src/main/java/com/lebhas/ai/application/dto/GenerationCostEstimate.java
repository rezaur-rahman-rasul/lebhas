package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GenerationCostEstimate(
        UUID pipelineId,
        UUID workspaceId,
        UUID creativeRequestId,
        BigDecimal totalEstimatedCostUsd,
        List<LayerCostEstimate> layerEstimates
) {
    public GenerationCostEstimate {
        layerEstimates = layerEstimates == null ? List.of() : List.copyOf(layerEstimates);
    }
}
