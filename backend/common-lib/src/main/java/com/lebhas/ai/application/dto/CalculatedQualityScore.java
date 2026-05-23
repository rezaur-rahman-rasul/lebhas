package com.lebhas.ai.application.dto;

import java.math.BigDecimal;

public record CalculatedQualityScore(
        BigDecimal overallScore,
        BigDecimal textReadabilityScore,
        BigDecimal productPreservationScore,
        BigDecimal brandingScore,
        BigDecimal banglaTypographyScore,
        BigDecimal compositionScore,
        String qualityNotes
) {
}
