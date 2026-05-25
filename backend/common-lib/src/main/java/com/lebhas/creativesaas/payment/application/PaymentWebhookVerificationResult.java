package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;

import java.util.Map;

public record PaymentWebhookVerificationResult(
        boolean verified,
        PaymentWebhookVerificationStatus verificationStatus,
        String providerTransactionId,
        String eventType,
        PaymentTransactionStatus paymentStatus,
        String failureReason,
        Map<String, String> providerPayload
) {
}
