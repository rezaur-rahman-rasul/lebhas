package com.lebhas.creativesaas.payment.infrastructure.provider;

import com.lebhas.creativesaas.payment.application.PaymentSessionRequest;
import com.lebhas.creativesaas.payment.application.PaymentSessionResponse;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ManualPaymentClient extends AbstractFoundationPaymentClient {

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.MANUAL;
    }

    @Override
    public PaymentSessionResponse createSession(
            PaymentProvider provider,
            PaymentProviderConfiguration configuration,
            PaymentSessionRequest request
    ) {
        return new PaymentSessionResponse(
                provider.getId(),
                provider.getCode(),
                provider.getProviderType(),
                configuration.getEnvironmentType(),
                provider.getCode() + "-" + UUID.randomUUID(),
                null,
                null,
                PaymentTransactionStatus.PENDING,
                "Manual payment session foundation created",
                Map.of("environment", configuration.getEnvironmentType().name())
        );
    }
}
