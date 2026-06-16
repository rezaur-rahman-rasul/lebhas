package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateMasterProviderRequest(
        String displayName,
        ProviderType providerType,
        String description,
        ProviderStatus status,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment,
        String baseUrl,
        String defaultModel,
        String modelsEndpoint,
        String modelsEndpointAuth,
        String apiKeyQueryParam,
        String sendSmsEndpoint,
        String balanceEndpoint,
        String requestMethod,
        String senderId,
        Integer otpLength,
        Integer otpExpiryMinutes,
        Integer resendCooldownSeconds,
        Integer maxAttempts,
        Boolean balanceMonitoringEnabled,
        Boolean healthCheckEnabled,
        List<String> supportedCapabilities,
        Integer priority,
        Integer rateLimitPerMinute,
        BigDecimal costMultiplier,
        String openAiAdminApiKey,
        BigDecimal providerTopUpAmountUsd,
        LocalDate providerTopUpDate,
        BigDecimal providerManualBalanceUsd,
        Boolean costSyncEnabled,
        String metadataJson
) {
}
