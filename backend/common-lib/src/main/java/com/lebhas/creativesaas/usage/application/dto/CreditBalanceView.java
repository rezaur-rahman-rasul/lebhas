package com.lebhas.creativesaas.usage.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditBalanceView(
        UUID workspaceId,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance
) {
}
