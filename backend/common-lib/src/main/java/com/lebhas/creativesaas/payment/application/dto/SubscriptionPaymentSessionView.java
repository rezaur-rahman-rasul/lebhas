package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPaymentSessionView(
        UUID subscriptionOrderId,
        UUID paymentTransactionId,
        UUID invoiceId,
        String invoiceNumber,
        UUID workspaceId,
        UUID pricingPlanId,
        String pricingPlanCode,
        String pricingPlanName,
        BillingCycle billingCycle,
        PaymentPurpose paymentPurpose,
        BigDecimal amount,
        String currency,
        UUID providerId,
        String providerCode,
        String providerSessionId,
        String providerTransactionId,
        String redirectUrl,
        PaymentTransactionStatus paymentStatus,
        String message
) {
}
