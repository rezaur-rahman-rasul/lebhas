package com.lebhas.ai.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QualityScoreResult(
        UUID qualityScoreId,
        UUID workspaceId,
        UUID generatedVersionId,
        BigDecimal overallScore,
        BigDecimal textReadabilityScore,
        BigDecimal productPreservationScore,
        BigDecimal brandingScore,
        BigDecimal banglaTypographyScore,
        BigDecimal compositionScore,
        String qualityNotes
) {
}
