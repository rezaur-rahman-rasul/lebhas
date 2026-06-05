package com.lebhas.ai.creative.service;

import com.lebhas.ai.creative.dto.CreativeCreditAvailabilityRequest;
import com.lebhas.ai.creative.dto.CreativeCreditAvailabilityResponse;
import com.lebhas.ai.creative.dto.CreativeCreditAvailabilityResponse.CreditStatus;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CreativeCreditAvailabilityService {

    private final CreditWalletRepository creditWalletRepository;
    private final WorkspacePlanContextService workspacePlanContextService;

    public CreativeCreditAvailabilityService(
            CreditWalletRepository creditWalletRepository,
            WorkspacePlanContextService workspacePlanContextService
    ) {
        this.creditWalletRepository = creditWalletRepository;
        this.workspacePlanContextService = workspacePlanContextService;
    }

    @Transactional(readOnly = true)
    public CreativeCreditAvailabilityResponse check(CreativeCreditAvailabilityRequest request) {
        UUID workspaceId = request.workspaceUuid();
        request.brandUuid();
        request.productServiceUuid();
        request.campaignUuid();

        BigDecimal availableCredits = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .map(wallet -> wallet.getAvailableBalance().max(BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);

        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        PlanFeaturePolicyView policy = planContext.featurePolicy();
        if (policy == null || !policy.creativeGenerationEnabled()) {
            return response(
                    request.versions(),
                    availableCredits,
                    CreditStatus.UNAVAILABLE,
                    false,
                    true,
                    "Creative generation is not available for the current workspace package.");
        }

        Integer versionLimit = policy.maxGeneratedVersionsPerRequest();
        if (versionLimit != null && versionLimit > 0 && request.versions() > versionLimit) {
            return response(
                    request.versions(),
                    availableCredits,
                    CreditStatus.MAY_BE_INSUFFICIENT,
                    false,
                    true,
                    "Your current package allows up to " + versionLimit + " version"
                            + (versionLimit == 1 ? "" : "s") + " per generation request.");
        }

        if (availableCredits.signum() <= 0) {
            return response(
                    request.versions(),
                    availableCredits,
                    CreditStatus.MAY_BE_INSUFFICIENT,
                    false,
                    true,
                    "This generation may require more credits. Please buy credits before generating.");
        }

        if (availableCredits.compareTo(BigDecimal.valueOf(request.versions())) < 0) {
            return response(
                    request.versions(),
                    availableCredits,
                    CreditStatus.MAY_BE_INSUFFICIENT,
                    false,
                    true,
                    "This generation may require more credits. Please reduce versions or buy credits.");
        }

        return response(
                request.versions(),
                availableCredits,
                CreditStatus.READY,
                true,
                false,
                "Your current credit balance is available for this generation.");
    }

    private CreativeCreditAvailabilityResponse response(
            Integer requestedVersions,
            BigDecimal availableCredits,
            CreditStatus status,
            boolean hasEnoughCredits,
            boolean blockGeneration,
            String message
    ) {
        return new CreativeCreditAvailabilityResponse(
                requestedVersions,
                availableCredits,
                status,
                hasEnoughCredits,
                blockGeneration,
                message,
                null,
                null);
    }
}
