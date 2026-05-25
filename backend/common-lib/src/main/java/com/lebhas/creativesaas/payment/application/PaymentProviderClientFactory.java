package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProviderClientFactory {

    private final Map<PaymentProviderType, PaymentProviderClient> clients;

    public PaymentProviderClientFactory(List<PaymentProviderClient> clients) {
        this.clients = new EnumMap<>(PaymentProviderType.class);
        for (PaymentProviderClient client : clients) {
            this.clients.put(client.providerType(), client);
        }
    }

    public PaymentProviderClient clientFor(PaymentProvider provider) {
        PaymentProviderClient client = clients.get(provider.getProviderType());
        if (client == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "No payment client registered for provider type " + provider.getProviderType()
            );
        }
        return client;
    }
}
