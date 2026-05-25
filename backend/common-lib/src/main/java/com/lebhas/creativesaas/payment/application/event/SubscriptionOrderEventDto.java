package com.lebhas.creativesaas.payment.application.event;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionOrderEventDto(
        UUID workspaceId,
        UUID subscriptionOrderId,
        UUID transactionId,
        UUID pricingPlanId,
        String pricingPlanCode,
        UUID requestedBy,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        PaymentOrderStatus status,
        Instant startsAt,
        Instant expiresAt
) {
}
