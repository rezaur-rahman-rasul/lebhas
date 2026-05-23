package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CreditUsageMapper {

    public CreditLedgerView toLedgerView(CreditLedger ledger) {
        return new CreditLedgerView(
                ledger.getId(),
                ledger.getWorkspaceId(),
                ledger.getCreativeRequestId(),
                ledger.getGeneratedVersionId(),
                ledger.getGenerationJobId(),
                ledger.getTransactionType(),
                ledger.getCreditsAmount(),
                ledger.getBalanceBeforeTransaction(),
                ledger.getBalanceAfterTransaction(),
                ledger.getReferenceType(),
                ledger.getReferenceId(),
                ledger.getDescription(),
                ledger.getCreatedBy(),
                ledger.getCreatedAt());
    }

    public CreditBalanceView toBalanceView(CreditWalletEntity wallet) {
        return new CreditBalanceView(
                wallet.getWorkspaceId(),
                wallet.getBalance(),
                wallet.getReservedBalance(),
                wallet.getAvailableBalance());
    }

    public CreditUsageResult toUsageResult(
            CreditLedger ledger,
            UUID creditReservationId,
            CreditWalletEntity wallet,
            BigDecimal amount,
            String referenceType,
            UUID referenceId
    ) {
        return new CreditUsageResult(
                ledger.getId(),
                creditReservationId,
                ledger.getWorkspaceId(),
                amount,
                wallet.getBalance(),
                wallet.getReservedBalance(),
                wallet.getAvailableBalance(),
                referenceType,
                referenceId);
    }
}
