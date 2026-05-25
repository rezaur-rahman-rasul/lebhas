package com.lebhas.creativesaas.payment.infrastructure.provider;

import com.lebhas.creativesaas.payment.application.PaymentProviderClient;
import com.lebhas.creativesaas.payment.application.PaymentSessionRequest;
import com.lebhas.creativesaas.payment.application.PaymentSessionResponse;
import com.lebhas.creativesaas.payment.application.PaymentVerificationRequest;
import com.lebhas.creativesaas.payment.application.PaymentVerificationResponse;
import com.lebhas.creativesaas.payment.application.PaymentWebhookVerificationResult;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

abstract class AbstractFoundationPaymentClient implements PaymentProviderClient {

    @Override
    public PaymentSessionResponse createSession(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentSessionRequest request
    ) {
        Map<String, String> payload = basePayload(configuration);
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            payload.put("idempotencyKey", request.idempotencyKey().trim());
        }
        return new PaymentSessionResponse(
                provider.getId(),
                provider.getCode(),
                provider.getProviderType(),
                configuration.getEnvironmentType(),
                provider.getCode() + "-" + UUID.randomUUID(),
                null,
                configuration.getSuccessUrl(),
                PaymentTransactionStatus.PENDING,
                provider.getName() + " payment session foundation created",
                payload
        );
    }

    @Override
    public PaymentVerificationResponse verifyPayment(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentVerificationRequest request
    ) {
        return new PaymentVerificationResponse(
                provider.getId(),
                provider.getCode(),
                provider.getProviderType(),
                configuration.getEnvironmentType(),
                request.providerSessionId(),
                request.providerTransactionId(),
                PaymentTransactionStatus.PENDING,
                null,
                null,
                provider.getName() + " verification foundation is pending provider API integration",
                request.providerPayload() == null ? Map.of() : Map.copyOf(request.providerPayload())
        );
    }

    @Override
    public PaymentWebhookVerificationResult verifyWebhook(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            String payload,
            String signature
    ) {
        if (payload == null || payload.isBlank()) {
            return new PaymentWebhookVerificationResult(
                    false,
                    PaymentWebhookVerificationStatus.FAILED,
                    null,
                    "UNKNOWN",
                    PaymentTransactionStatus.FAILED,
                    "Webhook payload is empty",
                    Map.of()
            );
        }
        return new PaymentWebhookVerificationResult(
                false,
                PaymentWebhookVerificationStatus.PENDING,
                null,
                "PROVIDER_WEBHOOK",
                PaymentTransactionStatus.PENDING,
                "Webhook signature verification requires provider-specific API details",
                Map.of("providerCode", provider.getCode(), "environment", configuration.getEnvironmentType().name())
        );
    }

    private Map<String, String> basePayload(PaymentProviderConfiguration configuration) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (configuration.getApiBaseUrl() != null) {
            payload.put("apiBaseUrl", configuration.getApiBaseUrl());
        }
        if (configuration.getMerchantId() != null) {
            payload.put("merchantId", configuration.getMerchantId());
        }
        if (configuration.getFailureUrl() != null) {
            payload.put("failureUrl", configuration.getFailureUrl());
        }
        if (configuration.getCancelUrl() != null) {
            payload.put("cancelUrl", configuration.getCancelUrl());
        }
        return Map.copyOf(payload);
    }
}
