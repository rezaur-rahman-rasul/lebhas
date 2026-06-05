package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;

import java.time.Instant;
import java.util.Map;

public record ProviderModelsJsonView(
        String providerKey,
        String providerCode,
        String displayName,
        ProviderEnvironment environment,
        String endpoint,
        int httpStatus,
        Instant retrievedAt,
        Map<String, Object> modelsJson
) {
}
