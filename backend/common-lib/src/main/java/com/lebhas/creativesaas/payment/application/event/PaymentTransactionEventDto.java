package com.lebhas.creativesaas.payment.application.event;

import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentTransactionEventDto(
        UUID workspaceId,
        UUID transactionId,
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
        String failureReason
) {
}
