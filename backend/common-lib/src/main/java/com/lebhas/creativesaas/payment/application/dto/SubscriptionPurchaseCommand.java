package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.util.UUID;

public record SubscriptionPurchaseCommand(
        UUID workspaceId,
        UUID requestedBy,
        UUID pricingPlanId,
        BillingCycle billingCycle,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode
) {
}
