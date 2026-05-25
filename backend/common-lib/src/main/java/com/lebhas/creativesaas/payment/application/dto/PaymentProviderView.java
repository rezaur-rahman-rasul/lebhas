package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentProviderType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentProviderView(
        UUID id,
        String name,
        String code,
        PaymentProviderType providerType,
        boolean enabled,
        boolean sandboxEnabled,
        boolean liveEnabled,
        int priority,
        List<PaymentProviderConfigurationView> configurations,
        Instant createdAt,
        Instant updatedAt
) {
}
