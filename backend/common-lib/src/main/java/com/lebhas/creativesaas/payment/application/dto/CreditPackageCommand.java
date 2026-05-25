package com.lebhas.creativesaas.payment.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditPackageCommand(
        UUID creditPackageId,
        String name,
        String code,
        long credits,
        long bonusCredits,
        BigDecimal price,
        String currency,
        boolean active,
        int sortOrder
) {
}
