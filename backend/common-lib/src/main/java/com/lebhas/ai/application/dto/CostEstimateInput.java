package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CostEstimateInput(
        UUID workspaceId,
        UUID creativeRequestId,
        BigDecimal requestedUnits,
        Map<String, Object> metadata
) {
    public CostEstimateInput {
        requestedUnits = requestedUnits == null || requestedUnits.signum() <= 0 ? BigDecimal.ONE : requestedUnits;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static CostEstimateInput defaultInput() {
        return new CostEstimateInput(null, null, BigDecimal.ONE, Map.of());
    }
}
