package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderConnectionTestStatus;
import com.lebhas.ai.domain.ProviderEnvironment;

import java.time.Instant;

public record ProviderConnectionTestResult(
        String providerKey,
        String providerCode,
        String displayName,
        String category,
        ProviderEnvironment environment,
        boolean success,
        String status,
        ProviderConnectionTestStatus testStatus,
        Long latencyMs,
        Instant testedAt,
        String message
) {
}
