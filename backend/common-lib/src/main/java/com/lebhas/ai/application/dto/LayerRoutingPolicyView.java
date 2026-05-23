package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.LayerRoutingStrategy;

import java.util.Map;
import java.util.UUID;

public record LayerRoutingPolicyView(
        UUID id,
        UUID pipelineLayerId,
        String policyCode,
        LayerRoutingStrategy routingStrategy,
        int priorityOrder,
        boolean enabled,
        Map<String, Object> conditions,
        Map<String, Object> rules
) {
}
