package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CostObservation(
        UUID providerId,
        UUID layerId,
        String modelName,
        BigDecimal costUsd,
        BigDecimal qualityScore,
        BigDecimal latencyMs,
        boolean successful,
        Instant occurredAt
) {
}
