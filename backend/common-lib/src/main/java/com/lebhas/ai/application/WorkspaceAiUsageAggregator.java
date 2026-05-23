package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.WorkspaceAiUsageRecord;
import com.lebhas.ai.domain.WorkspaceAiUsage;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class WorkspaceAiUsageAggregator {

    public void apply(WorkspaceAiUsage usage, WorkspaceAiUsageRecord record) {
        if (usage == null || record == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Workspace usage and usage record are required");
        }
        long generationRequests = nonNegative(record.generationRequests(), "generationRequests");
        long generatedVersions = nonNegative(record.generatedVersions(), "generatedVersions");
        long failures = nonNegative(record.failures(), "failures");
        long previousTimedGenerations = usage.getTotalGeneratedVersions() + usage.getTotalFailures();
        long nextGenerationRequests = usage.getTotalGenerationRequests() + generationRequests;
        long generationTimeSamples = generatedVersions + failures;
        if (record.generationTimeMs() != null && generationTimeSamples == 0) {
            generationTimeSamples = generationRequests;
        }
        BigDecimal nextAvgGenerationTimeMs = weightedAverage(
                usage.getAvgGenerationTimeMs(),
                previousTimedGenerations,
                record.generationTimeMs(),
                generationTimeSamples);
        usage.updateTotals(
                nextGenerationRequests,
                usage.getTotalGeneratedVersions() + generatedVersions,
                usage.getTotalCreditsConsumed().add(nonNegative(record.creditsConsumed())),
                usage.getTotalEstimatedCostUsd().add(nonNegative(record.estimatedCostUsd())),
                usage.getTotalFailures() + failures,
                nextAvgGenerationTimeMs);
    }

    private BigDecimal weightedAverage(
            BigDecimal previousAverage,
            long previousCount,
            BigDecimal nextValue,
            long nextCount
    ) {
        if (nextValue == null || nextCount <= 0) {
            return previousAverage == null ? BigDecimal.ZERO : previousAverage;
        }
        BigDecimal normalizedNext = nonNegative(nextValue);
        if (previousCount <= 0 || previousAverage == null) {
            return normalizedNext.setScale(4, RoundingMode.HALF_UP);
        }
        return previousAverage.multiply(BigDecimal.valueOf(previousCount))
                .add(normalizedNext.multiply(BigDecimal.valueOf(nextCount)))
                .divide(BigDecimal.valueOf(previousCount + nextCount), 4, RoundingMode.HALF_UP);
    }

    private long nonNegative(long value, String field) {
        if (value < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " must not be negative");
        }
        return value;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "usage amounts must not be negative");
        }
        return value;
    }
}
