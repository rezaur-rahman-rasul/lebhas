package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditUsageResult(
        UUID ledgerId,
        UUID creditReservationId,
        UUID workspaceId,
        BigDecimal creditsAmount,
        BigDecimal walletBalance,
        BigDecimal walletReservedBalance,
        BigDecimal walletAvailableBalance,
        String referenceType,
        UUID referenceId
) {
}
