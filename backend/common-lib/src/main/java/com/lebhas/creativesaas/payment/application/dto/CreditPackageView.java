package com.lebhas.creativesaas.payment.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditPackageView(
        UUID id,
        String name,
        String code,
        long credits,
        long bonusCredits,
        long totalCredits,
        BigDecimal price,
        String currency,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
