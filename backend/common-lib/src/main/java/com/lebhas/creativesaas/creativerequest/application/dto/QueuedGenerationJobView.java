package com.lebhas.creativesaas.creativerequest.application.dto;

import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.application.dto.GenerationJobView;

public record QueuedGenerationJobView(
        GenerationJobView job,
        CreditReservationResult reservation
) {
}
