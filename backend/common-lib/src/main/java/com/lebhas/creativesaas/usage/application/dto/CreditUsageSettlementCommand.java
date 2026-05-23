package com.lebhas.creativesaas.usage.application.dto;

import java.util.UUID;

public record CreditUsageSettlementCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        UUID creditReservationId,
        String referenceType,
        UUID referenceId,
        String reason,
        UUID createdBy
) {
}
