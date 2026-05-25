package com.lebhas.creativesaas.payment.application.dto;

public record PaymentWebhookCommand(
        String providerCode,
        String payload,
        String signature
) {
}
