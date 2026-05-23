package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RoutingOptimizationRequest(
        UUID workspaceId,
        UUID layerId,
        UUID creativeRequestId,
        BigDecimal requestedUnits,
        Map<String, Object> metadata
) {
    public RoutingOptimizationRequest {
        requestedUnits = requestedUnits == null || requestedUnits.signum() <= 0 ? BigDecimal.ONE : requestedUnits;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public CostEstimateInput toCostEstimateInput() {
        return new CostEstimateInput(workspaceId, creativeRequestId, requestedUnits, metadata);
    }
}
