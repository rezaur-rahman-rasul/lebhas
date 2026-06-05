package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderType;

import java.math.BigDecimal;
import java.util.List;

public record CreateMasterProviderRequest(
        String providerCode,
        String displayName,
        ProviderType providerType,
        String description,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment,
        boolean active,
        String baseUrl,
        String defaultModel,
        String modelsEndpoint,
        String modelsEndpointAuth,
        String apiKeyQueryParam,
        List<String> supportedCapabilities,
        Integer priority,
        Integer rateLimitPerMinute,
        BigDecimal costMultiplier,
        String metadataJson
) {
}
