package com.lebhas.creativesaas.generation.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditReservationResult(
        UUID reservationId,
        UUID workspaceId,
        BigDecimal reservedAmount,
        BigDecimal walletBalance,
        BigDecimal walletReservedBalance,
        BigDecimal walletAvailableBalance,
        String referenceType,
        UUID referenceId
) {
}
