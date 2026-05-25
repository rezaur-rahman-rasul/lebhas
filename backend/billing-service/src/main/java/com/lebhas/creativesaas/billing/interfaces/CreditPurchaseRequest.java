package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreditPurchaseRequest(
        @NotNull UUID creditPackageId,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode
) {
}
