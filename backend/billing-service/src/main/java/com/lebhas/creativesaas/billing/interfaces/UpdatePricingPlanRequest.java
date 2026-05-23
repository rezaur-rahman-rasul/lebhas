package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload to update a pricing plan.")
public record UpdatePricingPlanRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String name,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 60)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$")
        String code,
        @Size(max = 1000)
        String description,
        @NotNull(message = ValidationMessages.REQUIRED)
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal monthlyPrice,
        @NotNull(message = ValidationMessages.REQUIRED)
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal yearlyPrice,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency,
        boolean defaultPlan,
        boolean active,
        @Min(0)
        int sortOrder
) {
}
