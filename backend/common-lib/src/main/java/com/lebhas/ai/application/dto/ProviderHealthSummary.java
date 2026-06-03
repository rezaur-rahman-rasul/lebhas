package com.lebhas.ai.application.dto;

public record ProviderHealthSummary(
        long totalProviders,
        long healthy,
        long degraded,
        long failed
) {
}
