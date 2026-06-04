package com.lebhas.ai.credit.application;

import com.lebhas.ai.credit.application.dto.ProviderCreditExchangePolicyView;
import com.lebhas.ai.credit.application.dto.ProviderCreditLedgerView;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.ai.credit.domain.ProviderCreditExchangePolicy;
import com.lebhas.ai.credit.domain.ProviderCreditLedger;
import com.lebhas.ai.credit.domain.ProviderCreditPool;
import org.springframework.stereotype.Component;

@Component
public class ProviderCreditMapper {

    public ProviderCreditPoolView toPoolView(ProviderCreditPool pool) {
        return new ProviderCreditPoolView(
                pool.getId(),
                pool.getProviderId(),
                pool.getCurrency(),
                pool.getProviderBalanceAmount(),
                pool.getInternalCreditEquivalent(),
                pool.getReservedInternalCredits(),
                pool.getUsedInternalCredits(),
                pool.availableInternalCredits(),
                pool.getLowBalanceThreshold(),
                pool.isLowBalance(),
                pool.getCreatedAt(),
                pool.getUpdatedAt());
    }

    public ProviderCreditExchangePolicyView toPolicyView(ProviderCreditExchangePolicy policy) {
        return new ProviderCreditExchangePolicyView(
                policy.getId(),
                policy.getProviderId(),
                policy.getInternalCreditPerProviderUnit(),
                policy.getFreeSignupCreditPercentage(),
                policy.isFreeSignupCreditEnabled(),
                policy.getMaxFreeSignupCredits(),
                policy.getMinProviderBalanceRequired(),
                policy.getFallbackFreeCredits(),
                policy.isActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    public ProviderCreditLedgerView toLedgerView(ProviderCreditLedger ledger) {
        return new ProviderCreditLedgerView(
                ledger.getId(),
                ledger.getProviderId(),
                ledger.getTransactionType(),
                ledger.getAmount(),
                ledger.getBalanceBefore(),
                ledger.getBalanceAfter(),
                ledger.getReferenceType(),
                ledger.getReferenceId(),
                ledger.getDescription(),
                ledger.getCreatedBy(),
                ledger.getCreatedAt());
    }
}
