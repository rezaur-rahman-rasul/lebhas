package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionCacheEntry(
        UUID paymentTransactionId,
        UUID workspaceId,
        UUID userId,
        UUID providerId,
        PaymentPurpose paymentPurpose,
        String referenceType,
        UUID referenceId,
        BigDecimal amount,
        String currency,
        String providerTransactionId,
        String providerSessionId,
        PaymentTransactionStatus status,
        Instant cachedAt
) {
}
