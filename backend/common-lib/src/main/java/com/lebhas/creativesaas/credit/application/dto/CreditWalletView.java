package com.lebhas.creativesaas.credit.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditWalletView(
        UUID id,
        UUID workspaceId,
        BigDecimal balance,
        BigDecimal reservedBalance,
        Instant createdAt,
        Instant updatedAt
) {
}
