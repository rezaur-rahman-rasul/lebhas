package com.lebhas.ai.credit.application.dto;

import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;

import java.math.BigDecimal;
import java.util.List;

public record MasterCreditOverviewView(
        BigDecimal totalProviderEquivalentCredits,
        BigDecimal totalWorkspaceAvailableCredits,
        BigDecimal totalWorkspaceReservedCredits,
        BigDecimal totalWorkspaceUsedCredits,
        BigDecimal totalFreeCreditsGranted,
        List<ProviderCreditPoolView> providerPools,
        List<ProviderCreditPoolView> lowBalanceProviders,
        List<CreditLedgerView> recentCreditLedger
) {
}
