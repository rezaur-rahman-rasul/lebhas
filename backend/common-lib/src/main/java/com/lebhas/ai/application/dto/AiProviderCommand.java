package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;

import java.util.List;
import java.util.Map;

public record AiProviderCommand(
        String providerCode,
        String providerName,
        ProviderType providerType,
        ProviderStatus status,
        boolean enabled,
        List<String> supportedLayers,
        String credentialConfigKey,
        boolean fallbackEligible,
        boolean workspaceRoutingEligible,
        boolean planRoutingEligible,
        Map<String, Object> costMetadata,
        Map<String, Object> qualityMetadata,
        Map<String, Object> rateLimitMetadata
) {
}
