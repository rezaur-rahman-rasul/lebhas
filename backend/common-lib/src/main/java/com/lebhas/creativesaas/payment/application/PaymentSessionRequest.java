package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PaymentSessionRequest(
        UUID workspaceId,
        UUID userId,
        PaymentPurpose paymentPurpose,
        BigDecimal amount,
        String currency,
        String referenceType,
        UUID referenceId,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode,
        String idempotencyKey,
        Map<String, String> metadata
) {
}
