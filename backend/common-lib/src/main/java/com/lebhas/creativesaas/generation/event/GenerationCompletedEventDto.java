package com.lebhas.creativesaas.generation.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GenerationCompletedEventDto(
        UUID workspaceId,
        UUID creativeRequestId,
        UUID generationJobId,
        UUID generatedVersionId,
        UUID assetId,
        UUID storageFileId,
        UUID previewAssetId,
        UUID thumbnailAssetId,
        UUID creditReservationId,
        BigDecimal finalizedCredits,
        String providerName,
        String model,
        String providerJobId,
        boolean finalized,
        Instant occurredAt
) {
    public GenerationCompletedEventDto {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
