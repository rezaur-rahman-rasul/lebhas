package com.lebhas.creativesaas.generation.application.dto;

import java.util.UUID;

public record CreditFinalizeCommand(
        UUID workspaceId,
        UUID creditReservationId,
        String referenceType,
        UUID referenceId,
        String settlementReason
) {
}
