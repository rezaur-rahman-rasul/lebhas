package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;

public interface PaymentProviderClient {

    PaymentProviderType providerType();

    PaymentSessionResponse createSession(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentSessionRequest request
    );

    PaymentVerificationResponse verifyPayment(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentVerificationRequest request
    );

    PaymentWebhookVerificationResult verifyWebhook(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            String payload,
            String signature
    );
}
