package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

public record SubscriptionRenewRequest(
        BillingCycle billingCycle,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode
) {
}
