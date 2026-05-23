package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;

public record LayerQualityPolicyCommand(
        String policyCode,
        boolean enabled,
        int priorityOrder,
        BigDecimal minQualityScore,
        Map<String, Object> qualityRules,
        Map<String, Object> evaluationMetadata
) {
}
