package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record LayerCostPolicyView(
        UUID id,
        UUID pipelineLayerId,
        String policyCode,
        boolean enabled,
        int priorityOrder,
        String currency,
        BigDecimal maxCostPerRun,
        Map<String, Object> costRules,
        Map<String, Object> budgetMetadata
) {
}
