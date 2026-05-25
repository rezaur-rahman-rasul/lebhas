package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentProviderType;

import java.util.UUID;

public record PaymentProviderCommand(
        UUID providerId,
        String name,
        String code,
        PaymentProviderType providerType,
        boolean enabled,
        boolean sandboxEnabled,
        boolean liveEnabled,
        int priority
) {
}
