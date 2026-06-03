package com.lebhas.ai.application.dto;

import java.util.Map;
import java.util.UUID;

public record ProviderRoutingPolicyView(
        UUID id,
        String policyCode,
        UUID toolId,
        String qualityMode,
        UUID providerId,
        UUID modelId,
        UUID fallbackProviderId,
        UUID fallbackModelId,
        int priorityOrder,
        boolean enabled,
        int circuitFailureThreshold,
        Map<String, Object> metadata
) {
}
