package com.lebhas.creativesaas.pricing.application.dto;

import java.math.BigDecimal;

public record CreatePricingPlanCommand(
        String name,
        String code,
        String description,
        BigDecimal monthlyPrice,
        BigDecimal yearlyPrice,
        String currency,
        boolean defaultPlan,
        boolean active,
        int sortOrder
) {
}
