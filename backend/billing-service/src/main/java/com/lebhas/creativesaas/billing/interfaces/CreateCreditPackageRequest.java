package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateCreditPackageRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        String name,

        @NotBlank(message = ValidationMessages.REQUIRED)
        String code,

        @PositiveOrZero
        long credits,

        @PositiveOrZero
        long bonusCredits,

        @NotNull(message = ValidationMessages.REQUIRED)
        @DecimalMin(value = "0.00")
        BigDecimal price,

        @NotBlank(message = ValidationMessages.REQUIRED)
        String currency,

        boolean active,

        @PositiveOrZero
        int sortOrder
) {
}
