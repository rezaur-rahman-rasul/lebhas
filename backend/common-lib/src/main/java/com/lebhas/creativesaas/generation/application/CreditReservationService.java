package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.event.CreditLifecycleEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreditReservationService {

    private final CreativeCreditReservationService creditReservationService;
    private final GenerationEventProducer eventProducer;

    public CreditReservationService(
            CreativeCreditReservationService creditReservationService,
            GenerationEventProducer eventProducer
    ) {
        this.creditReservationService = creditReservationService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public CreditReservationResult reserve(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            BigDecimal estimatedCost,
            String referenceType,
            UUID referenceId
    ) {
        CreditReservationResult result = creditReservationService.reserveCredits(
                workspaceId,
                estimatedCost,
                referenceType,
                referenceId);
        eventProducer.publishCreditsReserved(creditEvent(
                result.workspaceId(),
                creativeRequestId,
                generatedVersionId,
                result.reservationId(),
                result.reservedAmount(),
                "RESERVED",
                null));
        return result;
    }

    @Transactional
    public void finalize(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID creditReservationId,
            String referenceType,
            UUID referenceId,
            BigDecimal amount,
            String reason
    ) {
        creditReservationService.finalizeCredits(new CreditFinalizeCommand(
                workspaceId,
                creditReservationId,
                referenceType,
                referenceId,
                reason));
        eventProducer.publishCreditsFinalized(creditEvent(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                creditReservationId,
                amount,
                "FINALIZED",
                reason));
    }

    @Transactional
    public void refund(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID creditReservationId,
            String referenceType,
            UUID referenceId,
            BigDecimal amount,
            String reason
    ) {
        creditReservationService.refundCredits(new CreditRefundCommand(
                workspaceId,
                creditReservationId,
                referenceType,
                referenceId,
                reason));
        eventProducer.publishCreditsRefunded(creditEvent(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                creditReservationId,
                amount,
                "REFUNDED",
                reason));
    }

    private CreditLifecycleEventDto creditEvent(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID creditReservationId,
            BigDecimal amount,
            String status,
            String reason
    ) {
        return new CreditLifecycleEventDto(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                creditReservationId,
                amount,
                status,
                reason,
                Instant.now());
    }
}
