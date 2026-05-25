package com.lebhas.creativesaas.payment.infrastructure.provider;

import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import org.springframework.stereotype.Component;

@Component
public class BkashPaymentClient extends AbstractFoundationPaymentClient {

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.BKASH;
    }
}
