package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;

import java.util.UUID;

public record CreditPurchaseCommand(
        UUID workspaceId,
        UUID requestedBy,
        UUID creditPackageId,
        PaymentEnvironmentType environmentType,
        String preferredProviderCode
) {
}
