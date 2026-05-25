package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PaymentVerificationResponse(
        UUID providerId,
        String providerCode,
        PaymentProviderType providerType,
        PaymentEnvironmentType environmentType,
        String providerSessionId,
        String providerTransactionId,
        PaymentTransactionStatus status,
        BigDecimal verifiedAmount,
        String verifiedCurrency,
        String message,
        Map<String, String> providerPayload
) {
}
