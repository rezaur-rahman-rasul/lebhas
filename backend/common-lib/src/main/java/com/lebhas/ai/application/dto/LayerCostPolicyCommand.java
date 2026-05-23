package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;

public record LayerCostPolicyCommand(
        String policyCode,
        boolean enabled,
        int priorityOrder,
        String currency,
        BigDecimal maxCostPerRun,
        Map<String, Object> costRules,
        Map<String, Object> budgetMetadata
) {
}
