package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.LayerRoutingStrategy;

import java.util.Map;

public record LayerRoutingPolicyCommand(
        String policyCode,
        LayerRoutingStrategy routingStrategy,
        int priorityOrder,
        boolean enabled,
        Map<String, Object> conditions,
        Map<String, Object> rules
) {
}
