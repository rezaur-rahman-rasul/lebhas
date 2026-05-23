package com.lebhas.creativesaas.usage.application.dto;

import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditLedgerView(
        UUID id,
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        CreditLedgerTransactionType transactionType,
        BigDecimal creditsAmount,
        BigDecimal balanceBeforeTransaction,
        BigDecimal balanceAfterTransaction,
        String referenceType,
        UUID referenceId,
        String description,
        UUID createdBy,
        Instant createdAt
) {
}
