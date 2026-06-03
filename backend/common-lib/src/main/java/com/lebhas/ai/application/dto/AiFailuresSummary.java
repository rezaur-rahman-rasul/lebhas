package com.lebhas.ai.application.dto;

public record AiFailuresSummary(
        long failureCount,
        long retryCount,
        long fallbackCount
) {
}
