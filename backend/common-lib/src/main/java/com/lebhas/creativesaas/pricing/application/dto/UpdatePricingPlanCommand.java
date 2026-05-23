package com.lebhas.creativesaas.pricing.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePricingPlanCommand(
        UUID pricingPlanId,
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
