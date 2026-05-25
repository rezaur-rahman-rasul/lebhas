package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePaymentProviderRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        String name,

        @NotBlank(message = ValidationMessages.REQUIRED)
        String code,

        @NotNull(message = ValidationMessages.REQUIRED)
        PaymentProviderType providerType,

        boolean enabled,
        boolean sandboxEnabled,
        boolean liveEnabled,

        @PositiveOrZero
        int priority
) {
}
