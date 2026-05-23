package com.lebhas.creativesaas.billing.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Pricing plan response.")
public record PricingPlanResponse(
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
