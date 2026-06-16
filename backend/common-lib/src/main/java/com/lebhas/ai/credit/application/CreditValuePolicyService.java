package com.lebhas.ai.credit.application;

import com.lebhas.ai.credit.application.dto.CreditValuePolicyCommand;
import com.lebhas.ai.credit.application.dto.CreditValuePolicyPreviewView;
import com.lebhas.ai.credit.application.dto.CreditValuePolicyView;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.ai.credit.domain.CreditValuePolicy;
import com.lebhas.ai.credit.infrastructure.persistence.CreditValuePolicyRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreditValuePolicyService {

    private final CreditValuePolicyRepository policyRepository;
    private final ProviderCreditPoolService providerCreditPoolService;

    public CreditValuePolicyService(
            CreditValuePolicyRepository policyRepository,
            ProviderCreditPoolService providerCreditPoolService
    ) {
        this.policyRepository = policyRepository;
        this.providerCreditPoolService = providerCreditPoolService;
    }

    @Transactional(readOnly = true)
    public CreditValuePolicyView getActivePolicyView() {
        return toView(requireActivePolicy());
    }

    @Transactional(readOnly = true)
    public CreditValuePolicy requireActivePolicy() {
        return policyRepository.findFirstByActiveTrueAndDeletedFalseOrderByEffectiveFromDescUpdatedAtDesc()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Credit value policy is not configured"));
    }

    @Transactional
    public CreditValuePolicyView savePolicy(CreditValuePolicyCommand command) {
        CreditValuePolicy policy = policyRepository.findFirstByDeletedFalseOrderByUpdatedAtDesc()
                .orElseGet(() -> CreditValuePolicy.create(
                        command.currency(),
                        command.creditUsdValue(),
                        command.averageProviderCostPerCreativeUsd(),
                        command.providerCostMultiplier(),
                        command.freeSignupCreditEnabled(),
                        command.freeSignupMode(),
                        command.freeSignupCredits(),
                        command.freeSignupUsdValue(),
                        command.freeSignupPercentage(),
                        command.oneTimePerWorkspace(),
                        command.minimumWalletBalanceWarning(),
                        command.active(),
                        command.effectiveFrom()));
        if (policy.getId() != null) {
            policy.update(
                    command.currency(),
                    command.creditUsdValue(),
                    command.averageProviderCostPerCreativeUsd(),
                    command.providerCostMultiplier(),
                    command.freeSignupCreditEnabled(),
                    command.freeSignupMode(),
                    command.freeSignupCredits(),
                    command.freeSignupUsdValue(),
                    command.freeSignupPercentage(),
                    command.oneTimePerWorkspace(),
                    command.minimumWalletBalanceWarning(),
                    command.active(),
                    command.effectiveFrom());
        }
        return toView(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public CreditValuePolicyPreviewView preview(CreditValuePolicyCommand command) {
        CreditValuePolicy policy = CreditValuePolicy.create(
                command.currency(),
                command.creditUsdValue(),
                command.averageProviderCostPerCreativeUsd(),
                command.providerCostMultiplier(),
                command.freeSignupCreditEnabled(),
                command.freeSignupMode(),
                command.freeSignupCredits(),
                command.freeSignupUsdValue(),
                command.freeSignupPercentage(),
                command.oneTimePerWorkspace(),
                command.minimumWalletBalanceWarning(),
                command.active(),
                command.effectiveFrom());
        BigDecimal providerCostUsd = policy.providerCost(null);
        BigDecimal creativeCostUsd = policy.calculatedCreativeCostUsd(providerCostUsd);
        BigDecimal creativeCredits = policy.calculateCreativeCreditCost(providerCostUsd, 1);
        BigDecimal freeCredits = policy.calculateFreeSignupCredits(totalProviderAvailableCredits());
        return new CreditValuePolicyPreviewView(
                policy.getCurrency(),
                providerCostUsd,
                policy.getProviderCostMultiplier(),
                creativeCostUsd,
                policy.getCreditUsdValue(),
                creativeCredits,
                policy.getFreeSignupMode(),
                freeCredits,
                policy.freeSignupUsdEquivalent(freeCredits));
    }

    public BigDecimal calculateCreativeCreditCost(BigDecimal providerEstimatedCostUsd, int versions) {
        return requireActivePolicy().calculateCreativeCreditCost(providerEstimatedCostUsd, versions);
    }

    public BigDecimal calculateFreeSignupCredits() {
        CreditValuePolicy policy = requireActivePolicy();
        return policy.calculateFreeSignupCredits(totalProviderAvailableCredits());
    }

    public CreditValuePolicyView toView(CreditValuePolicy policy) {
        BigDecimal providerCostUsd = policy.providerCost(null);
        BigDecimal creativeCredits = policy.calculateCreativeCreditCost(providerCostUsd, 1);
        BigDecimal freeCredits = policy.calculateFreeSignupCredits(totalProviderAvailableCredits());
        return new CreditValuePolicyView(
                policy.getId(),
                policy.getCurrency(),
                policy.getCreditUsdValue(),
                policy.getAverageProviderCostPerCreativeUsd(),
                policy.getProviderCostMultiplier(),
                policy.calculatedCreativeCostUsd(providerCostUsd),
                creativeCredits,
                policy.isFreeSignupCreditEnabled(),
                policy.getFreeSignupMode(),
                policy.getFreeSignupCredits(),
                policy.getFreeSignupUsdValue(),
                policy.getFreeSignupPercentage(),
                policy.freeSignupUsdEquivalent(freeCredits),
                policy.isOneTimePerWorkspace(),
                policy.getMinimumWalletBalanceWarning(),
                policy.isActive(),
                policy.getEffectiveFrom(),
                policy.getUpdatedAt());
    }

    private BigDecimal totalProviderAvailableCredits() {
        return providerCreditPoolService.listPools().stream()
                .map(ProviderCreditPoolView::availableInternalCredits)
                .reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);
    }
}
