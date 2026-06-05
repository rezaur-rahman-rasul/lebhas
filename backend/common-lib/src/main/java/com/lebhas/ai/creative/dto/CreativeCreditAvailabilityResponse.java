package com.lebhas.ai.creative.dto;

import java.math.BigDecimal;

public record CreativeCreditAvailabilityResponse(
        Integer requestedVersions,
        BigDecimal availableCredits,
        CreditStatus creditStatus,
        Boolean hasEnoughCredits,
        Boolean blockGeneration,
        String message,
        BigDecimal remainingCreditsAfterGeneration,
        BigDecimal actualCreditsUsed
) {
    public enum CreditStatus {
        READY,
        MAY_BE_INSUFFICIENT,
        UNAVAILABLE
    }
}
