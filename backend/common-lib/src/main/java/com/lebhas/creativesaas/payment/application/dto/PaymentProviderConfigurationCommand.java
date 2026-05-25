package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.util.UUID;

public record PaymentProviderConfigurationCommand(
        UUID configurationId,
        UUID providerId,
        PaymentEnvironmentType environmentType,
        String apiBaseUrl,
        String merchantId,
        String apiKey,
        String secret,
        String webhookSecret,
        String successUrl,
        String failureUrl,
        String cancelUrl,
        boolean active
) {
}
