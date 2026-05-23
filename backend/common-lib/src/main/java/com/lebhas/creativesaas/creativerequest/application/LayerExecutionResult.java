package com.lebhas.creativesaas.creativerequest.application;

import java.util.Map;

public record LayerExecutionResult(
        boolean success,
        String message,
        Map<String, Object> metadata
) {
    public LayerExecutionResult {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static LayerExecutionResult foundationSuccess(String message, Map<String, Object> metadata) {
        return new LayerExecutionResult(true, message, metadata);
    }

    public static LayerExecutionResult failure(String message, Map<String, Object> metadata) {
        return new LayerExecutionResult(false, message, metadata);
    }
}
