package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class GenerationOrchestrator {

    private static final String CREDIT_REFERENCE_TYPE = "creative_request_generation";
    private static final String QUEUE_NAME = "creative-generation";

    private final CreativeRequestRepository creativeRequestRepository;
    private final GenerationJobService generationJobService;
    private final CreditReservationService creditReservationService;
    private final GeneratedVersionService generatedVersionService;

    public GenerationOrchestrator(
            CreativeRequestRepository creativeRequestRepository,
            GenerationJobService generationJobService,
            CreditReservationService creditReservationService,
            GeneratedVersionService generatedVersionService
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.generationJobService = generationJobService;
        this.creditReservationService = creditReservationService;
        this.generatedVersionService = generatedVersionService;
    }

    @Transactional
    public QueuedGeneration queueGeneration(
            CreativeRequestEntity request,
            UUID actorUserId,
            BigDecimal estimatedCreditCost
    ) {
        if (request.getStatus() != CreativeRequestStatus.DRAFT && request.getStatus() != CreativeRequestStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only draft or failed creative requests can be queued for generation");
        }
        generatedVersionService.validateVersionCapacity(
                request.getWorkspaceId(),
                request.getId(),
                request.getRequestedVersions());

        CreditReservationResult reservation = creditReservationService.reserve(
                request.getWorkspaceId(),
                request.getId(),
                null,
                estimatedCreditCost,
                CREDIT_REFERENCE_TYPE,
                request.getId());
        request.attachCreditReservation(reservation.reservationId());
        request.queue();
        CreativeRequestEntity savedRequest = creativeRequestRepository.save(request);
        GenerationJobEntity job = generationJobService.queue(savedRequest, QUEUE_NAME);
        return new QueuedGeneration(savedRequest, job, reservation, actorUserId);
    }

    public record QueuedGeneration(
            CreativeRequestEntity request,
            GenerationJobEntity job,
            CreditReservationResult reservation,
            UUID actorUserId
    ) {
    }
}
