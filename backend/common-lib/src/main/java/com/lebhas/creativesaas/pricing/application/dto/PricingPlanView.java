package com.lebhas.creativesaas.pricing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PricingPlanView(
        UUID id,
        String name,
        String code,
        String description,
        BigDecimal monthlyPrice,
        BigDecimal yearlyPrice,
        String currency,
        boolean defaultPlan,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
