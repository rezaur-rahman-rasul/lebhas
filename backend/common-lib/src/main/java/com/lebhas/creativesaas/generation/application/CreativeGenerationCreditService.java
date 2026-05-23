package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CreativeGenerationCreditService {

    private static final String REFERENCE_TYPE = "creative_generation_request";

    private final CreditEstimationService creditEstimationService;
    private final CreativeCreditReservationService creativeCreditReservationService;

    public CreativeGenerationCreditService(
            CreditEstimationService creditEstimationService,
            CreativeCreditReservationService creativeCreditReservationService
    ) {
        this.creditEstimationService = creditEstimationService;
        this.creativeCreditReservationService = creativeCreditReservationService;
    }

    public void validateSufficientCredits(UUID workspaceId, CreativeGenerationContext context) {
        BigDecimal requiredAmount = creditEstimationService.estimate(context);
        creativeCreditReservationService.assertSufficientBalance(workspaceId, requiredAmount);
    }

    public CreditReservationResult reserveCredits(CreativeGenerationRequestEntity request) {
        return creativeCreditReservationService.reserveCredits(
                request.getWorkspaceId(),
                amountFor(request),
                REFERENCE_TYPE,
                request.getId());
    }

    public void releaseReservedCredits(CreativeGenerationRequestEntity request) {
        creativeCreditReservationService.refundCredits(new CreditRefundCommand(
                request.getWorkspaceId(),
                null,
                REFERENCE_TYPE,
                request.getId(),
                "generation_request_failed"));
    }

    public void commitCreditUsage(CreativeGenerationRequestEntity request) {
        creativeCreditReservationService.finalizeCredits(new CreditFinalizeCommand(
                request.getWorkspaceId(),
                null,
                REFERENCE_TYPE,
                request.getId(),
                "generation_request_completed"));
    }

    private BigDecimal amountFor(CreativeGenerationRequestEntity request) {
        return creditEstimationService.estimate(request);
    }
}
