package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiModelView(
        UUID id,
        UUID providerId,
        String modelCode,
        String modelName,
        ProviderStatus status,
        boolean enabled,
        boolean defaultModel,
        List<String> capabilities,
        Map<String, Object> costMetadata,
        Map<String, Object> qualityMetadata,
        Map<String, Object> rateLimitMetadata
) {
}
