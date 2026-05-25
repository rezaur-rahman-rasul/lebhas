package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditPurchaseCreditCommand(
        UUID workspaceId,
        BigDecimal creditsAmount,
        String referenceType,
        UUID referenceId,
        String description,
        UUID createdBy
) {
}
