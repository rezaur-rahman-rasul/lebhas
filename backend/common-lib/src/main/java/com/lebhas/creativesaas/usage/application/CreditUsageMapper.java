package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.profile.application.SafeProfileDisplayService;
import com.lebhas.creativesaas.profile.application.dto.SafeProfileDisplayView;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CreditUsageMapper {

    private SafeProfileDisplayService safeProfileDisplayService;

    @Autowired(required = false)
    public void setSafeProfileDisplayService(SafeProfileDisplayService safeProfileDisplayService) {
        this.safeProfileDisplayService = safeProfileDisplayService;
    }

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
                safeDisplay(ledger.getWorkspaceId(), ledger.getCreatedBy()),
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

    private SafeProfileDisplayView safeDisplay(UUID workspaceId, UUID userId) {
        return safeProfileDisplayService == null ? null : safeProfileDisplayService.forUserInWorkspace(workspaceId, userId);
    }
}
