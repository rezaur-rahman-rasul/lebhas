package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.time.Instant;
import java.util.UUID;

public record PaymentProviderConfigurationView(
        UUID id,
        UUID providerId,
        PaymentEnvironmentType environmentType,
        String apiBaseUrl,
        String merchantId,
        boolean apiKeyConfigured,
        boolean secretConfigured,
        boolean webhookSecretConfigured,
        String successUrl,
        String failureUrl,
        String cancelUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
