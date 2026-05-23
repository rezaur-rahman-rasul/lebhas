package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.domain.LayerRoutingStrategy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ConfigureLayerRoutingPolicyRequest(
        @NotBlank @Size(max = 120) String policyCode,
        LayerRoutingStrategy routingStrategy,
        @Min(1) int priorityOrder,
        boolean enabled,
        @NotNull Map<String, Object> conditions,
        @NotNull Map<String, Object> rules
) {
}
