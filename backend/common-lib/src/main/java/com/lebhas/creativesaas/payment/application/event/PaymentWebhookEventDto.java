package com.lebhas.creativesaas.payment.application.event;

import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;

import java.util.UUID;

public record PaymentWebhookEventDto(
        UUID workspaceId,
        UUID webhookLogId,
        UUID transactionId,
        UUID providerId,
        String providerCode,
        String providerTransactionId,
        String eventType,
        PaymentWebhookVerificationStatus verificationStatus,
        PaymentTransactionStatus paymentStatus,
        boolean processed,
        boolean duplicate,
        String failureReason
) {
}
