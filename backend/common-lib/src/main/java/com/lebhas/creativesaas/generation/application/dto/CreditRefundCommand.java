package com.lebhas.creativesaas.generation.application.dto;

import java.util.UUID;

public record CreditRefundCommand(
        UUID workspaceId,
        UUID creditReservationId,
        String referenceType,
        UUID referenceId,
        String refundReason
) {
}
