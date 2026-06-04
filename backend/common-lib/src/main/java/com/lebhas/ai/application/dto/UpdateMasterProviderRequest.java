package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;

import java.math.BigDecimal;
import java.util.List;

public record UpdateMasterProviderRequest(
        String displayName,
        String description,
        ProviderStatus status,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment,
        String baseUrl,
        String defaultModel,
        List<String> supportedCapabilities,
        Integer priority,
        Integer rateLimitPerMinute,
        BigDecimal costMultiplier,
        String metadataJson
) {
}
