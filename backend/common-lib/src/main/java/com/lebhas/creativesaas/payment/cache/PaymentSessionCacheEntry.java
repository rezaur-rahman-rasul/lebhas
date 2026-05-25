package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSessionCacheEntry(
        UUID paymentTransactionId,
        String referenceType,
        UUID referenceId,
        UUID subscriptionOrderId,
        UUID creditPurchaseOrderId,
        UUID invoiceId,
        UUID workspaceId,
        UUID pricingPlanId,
        UUID creditPackageId,
        UUID providerId,
        String providerCode,
        String providerSessionId,
        String providerTransactionId,
        String redirectUrl,
        PaymentPurpose paymentPurpose,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        Instant cachedAt
) {
}
