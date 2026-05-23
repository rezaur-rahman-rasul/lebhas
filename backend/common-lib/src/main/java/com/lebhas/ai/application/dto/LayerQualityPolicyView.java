package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record LayerQualityPolicyView(
        UUID id,
        UUID pipelineLayerId,
        String policyCode,
        boolean enabled,
        int priorityOrder,
        BigDecimal minQualityScore,
        Map<String, Object> qualityRules,
        Map<String, Object> evaluationMetadata
) {
}
