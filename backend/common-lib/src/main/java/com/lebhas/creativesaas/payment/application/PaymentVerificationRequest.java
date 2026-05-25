package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.util.Map;

public record PaymentVerificationRequest(
        String providerCode,
        PaymentEnvironmentType environmentType,
        String providerSessionId,
        String providerTransactionId,
        Map<String, String> providerPayload
) {
}
