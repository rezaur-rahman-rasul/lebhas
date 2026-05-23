package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ConfigureLayerRetryPolicyRequest(
        boolean retryable,
        @NotNull Map<String, Object> configuration
) {
}
