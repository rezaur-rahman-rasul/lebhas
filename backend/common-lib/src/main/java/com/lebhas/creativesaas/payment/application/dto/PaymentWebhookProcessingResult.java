package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;

import java.util.UUID;

public record PaymentWebhookProcessingResult(
        UUID webhookLogId,
        UUID paymentTransactionId,
        String providerCode,
        String eventType,
        PaymentWebhookVerificationStatus verificationStatus,
        PaymentTransactionStatus paymentStatus,
        boolean processed,
        boolean duplicate,
        String message
) {
}
