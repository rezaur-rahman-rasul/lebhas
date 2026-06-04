package com.lebhas.ai.credit.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderCreditPoolAdjustmentCommand(
        @NotNull BigDecimal amount,
        String referenceType,
        UUID referenceId,
        String description
) {
}
