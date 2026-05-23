package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QualityScoreInput(
        UUID workspaceId,
        UUID generatedVersionId,
        UUID providerId,
        UUID layerId,
        String modelName,
        BigDecimal textReadabilityScore,
        BigDecimal productPreservationScore,
        BigDecimal brandingScore,
        BigDecimal banglaTypographyScore,
        BigDecimal compositionScore,
        BigDecimal overallScore,
        String qualityNotes,
        BigDecimal costUsd,
        BigDecimal latencyMs,
        Boolean successful
) {
}
