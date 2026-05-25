package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProviderRouter {

    private final PaymentProviderResolver resolver;
    private final PaymentProviderClientFactory clientFactory;

    public PaymentProviderRouter(PaymentProviderResolver resolver, PaymentProviderClientFactory clientFactory) {
        this.resolver = resolver;
        this.clientFactory = clientFactory;
    }

    @Transactional(readOnly = true)
    public PaymentSessionResponse createSession(PaymentSessionRequest request) {
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = resolver.resolve(request);
        PaymentProviderClient client = clientFactory.clientFor(resolvedProvider.provider());
        return client.createSession(resolvedProvider.provider(), resolvedProvider.configuration(), request);
    }

    @Transactional(readOnly = true)
    public PaymentVerificationResponse verifyPayment(PaymentVerificationRequest request) {
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = resolver.resolve(request);
        PaymentProviderClient client = clientFactory.clientFor(resolvedProvider.provider());
        return client.verifyPayment(resolvedProvider.provider(), resolvedProvider.configuration(), request);
    }

    @Transactional(readOnly = true)
    public PaymentWebhookVerificationResult verifyWebhook(String providerCode, String payload, String signature) {
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = resolver.resolveForWebhook(providerCode);
        PaymentProvider provider = resolvedProvider.provider();
        PaymentProviderConfiguration configuration = resolvedProvider.configuration();
        return clientFactory.clientFor(provider).verifyWebhook(provider, configuration, payload, signature);
    }
}
