package com.lebhas.ai.credit.application.dto;

import com.lebhas.ai.credit.domain.ProviderCreditTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderCreditLedgerView(
        UUID id,
        UUID providerId,
        ProviderCreditTransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String referenceType,
        UUID referenceId,
        String description,
        UUID createdBy,
        Instant createdAt
) {
}
