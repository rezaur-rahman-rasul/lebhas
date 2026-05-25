package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditPurchasePaymentSessionView(
        UUID creditPurchaseOrderId,
        UUID paymentTransactionId,
        UUID workspaceId,
        UUID creditPackageId,
        String creditPackageCode,
        String creditPackageName,
        long credits,
        BigDecimal amount,
        String currency,
        PaymentPurpose paymentPurpose,
        UUID providerId,
        String providerCode,
        String providerSessionId,
        String providerTransactionId,
        String redirectUrl,
        PaymentTransactionStatus status,
        String message
) {
}
