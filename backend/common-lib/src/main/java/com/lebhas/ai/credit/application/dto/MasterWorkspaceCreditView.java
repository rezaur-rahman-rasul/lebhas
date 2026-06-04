package com.lebhas.ai.credit.application.dto;

import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;

import java.time.Instant;
import java.util.List;

public record MasterWorkspaceCreditView(
        CreditBalanceView creditAccount,
        boolean freeCreditsGranted,
        Instant freeCreditsGrantedAt,
        List<CreditLedgerView> usageHistory
) {
}
