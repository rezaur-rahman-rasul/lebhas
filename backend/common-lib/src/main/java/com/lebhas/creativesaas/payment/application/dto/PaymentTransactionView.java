package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionView(
        UUID id,
        UUID workspaceId,
        UUID userId,
        SafeProfileDisplayView requestedByDisplay,
        UUID providerId,
        PaymentPurpose paymentPurpose,
        String referenceType,
        UUID referenceId,
        BigDecimal amount,
        String currency,
        String providerTransactionId,
        String providerSessionId,
        PaymentTransactionStatus status,
        String failureReason,
        Instant initiatedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
