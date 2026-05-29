package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentProviderConfigurationRequest(
        @NotNull(message = ValidationMessages.REQUIRED)
        UUID providerId,

        @NotNull(message = ValidationMessages.REQUIRED)
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
