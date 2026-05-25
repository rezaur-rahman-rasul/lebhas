package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.util.Map;
import java.util.UUID;

public record PaymentSessionResponse(
        UUID providerId,
        String providerCode,
        PaymentProviderType providerType,
        PaymentEnvironmentType environmentType,
        String providerSessionId,
        String providerTransactionId,
        String redirectUrl,
        PaymentTransactionStatus status,
        String message,
        Map<String, String> providerPayload
) {
}
