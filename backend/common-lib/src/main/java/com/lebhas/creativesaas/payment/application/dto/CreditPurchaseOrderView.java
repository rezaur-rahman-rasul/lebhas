package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditPurchaseOrderView(
        UUID id,
        UUID workspaceId,
        UUID creditPackageId,
        UUID requestedBy,
        long credits,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        UUID paymentTransactionId,
        Instant createdAt,
        Instant updatedAt
) {
}
