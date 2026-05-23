package com.lebhas.creativesaas.usage.application.dto;

import java.util.UUID;

public record CreditUsageCommand(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generatedVersionId,
        UUID generationJobId,
        String referenceType,
        UUID referenceId,
        UUID createdBy
) {
}
