package com.lebhas.creativesaas.payment.application.event;

import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditPurchaseEventDto(
        UUID workspaceId,
        UUID creditPurchaseOrderId,
        UUID transactionId,
        UUID creditPackageId,
        String creditPackageCode,
        UUID requestedBy,
        long credits,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        UUID ledgerId
) {
}
