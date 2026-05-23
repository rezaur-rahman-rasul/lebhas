package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CalculatedQualityScore;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class QualityScoreCalculator {

    private final ProductPreservationQualityService productPreservationQualityService;
    private final BrandingQualityService brandingQualityService;
    private final BanglaTypographyQualityService banglaTypographyQualityService;
    private final CompositionQualityService compositionQualityService;

    public QualityScoreCalculator(
            ProductPreservationQualityService productPreservationQualityService,
            BrandingQualityService brandingQualityService,
            BanglaTypographyQualityService banglaTypographyQualityService,
            CompositionQualityService compositionQualityService
    ) {
        this.productPreservationQualityService = productPreservationQualityService;
        this.brandingQualityService = brandingQualityService;
        this.banglaTypographyQualityService = banglaTypographyQualityService;
        this.compositionQualityService = compositionQualityService;
    }

    public CalculatedQualityScore calculate(QualityScoreInput input) {
        if (input == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Quality score input is required");
        }
        BigDecimal text = normalizeScore(input.textReadabilityScore());
        BigDecimal product = productPreservationQualityService.score(input.productPreservationScore());
        BigDecimal branding = brandingQualityService.score(input.brandingScore());
        BigDecimal bangla = banglaTypographyQualityService.score(input.banglaTypographyScore());
        BigDecimal composition = compositionQualityService.score(input.compositionScore());
        BigDecimal overall = input.overallScore() == null
                ? averageMeasured(text, product, branding, bangla, composition)
                : normalizeScore(input.overallScore());
        return new CalculatedQualityScore(
                overall,
                zeroIfNull(text),
                zeroIfNull(product),
                zeroIfNull(branding),
                zeroIfNull(bangla),
                zeroIfNull(composition),
                normalizeNotes(input.qualityNotes(), overall));
    }

    public static BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return null;
        }
        BigDecimal normalized = score;
        if (normalized.compareTo(BigDecimal.ONE) > 0 && normalized.compareTo(BigDecimal.valueOf(100)) <= 0) {
            normalized = normalized.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        if (normalized.signum() < 0) {
            normalized = BigDecimal.ZERO;
        }
        if (normalized.compareTo(BigDecimal.ONE) > 0) {
            normalized = BigDecimal.ONE;
        }
        return normalized.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMeasured(BigDecimal... scores) {
        List<BigDecimal> measured = new ArrayList<>();
        for (BigDecimal score : scores) {
            if (score != null) {
                measured.add(score);
            }
        }
        if (measured.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = measured.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(measured.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal score) {
        return score == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : score;
    }

    private String normalizeNotes(String notes, BigDecimal overall) {
        if (notes != null && !notes.isBlank()) {
            return notes.trim();
        }
        if (overall == null || overall.signum() == 0) {
            return "No measured quality inputs were provided";
        }
        return null;
    }
}
