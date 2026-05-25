package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscriptionPurchaseRequest(
        @NotNull UUID pricingPlanId,
        BillingCycle billingCycle,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode
) {
}
