package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import org.springframework.stereotype.Service;

@Service
public class PaymentWebhookVerifier {

    private final PaymentProviderRouter paymentProviderRouter;

    public PaymentWebhookVerifier(PaymentProviderRouter paymentProviderRouter) {
        this.paymentProviderRouter = paymentProviderRouter;
    }

    public PaymentWebhookVerificationResult verify(PaymentProvider provider, String payload, String signature) {
        return paymentProviderRouter.verifyWebhook(provider.getCode(), payload, signature);
    }
}
