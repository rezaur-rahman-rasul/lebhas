package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record MasterCreditAdjustmentRequest(
        @NotNull(message = ValidationMessages.REQUIRED)
        BigDecimal creditsAmount,
        @Size(max = 80)
        String referenceType,
        UUID referenceId,
        @Size(max = 1000)
        String description
) {
}
