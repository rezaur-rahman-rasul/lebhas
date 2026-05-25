package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionOrderView(
        UUID id,
        UUID workspaceId,
        UUID pricingPlanId,
        UUID requestedBy,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        UUID paymentTransactionId,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
