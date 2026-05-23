package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.ProviderBenchmarkResult;
import com.lebhas.ai.application.dto.ProviderBenchmarkSummary;
import com.lebhas.ai.domain.AiProviderMetrics;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiProviderMetricsRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class ProviderBenchmarkService {

    private final AiToolProviderRepository providerRepository;
    private final AiProviderMetricsRepository metricsRepository;
    private final ProviderReliabilityScorer reliabilityScorer;

    public ProviderBenchmarkService(
            AiToolProviderRepository providerRepository,
            AiProviderMetricsRepository metricsRepository,
            ProviderReliabilityScorer reliabilityScorer
    ) {
        this.providerRepository = providerRepository;
        this.metricsRepository = metricsRepository;
        this.reliabilityScorer = reliabilityScorer;
    }

    @Transactional(readOnly = true)
    public ProviderBenchmarkSummary benchmarkProviders() {
        List<ProviderMetricRow> rows = providerRepository.findAllByDeletedFalseOrderByProviderNameAsc().stream()
                .flatMap(provider -> metricsRepository.findAllByProviderIdAndDeletedFalse(provider.getId()).stream()
                        .map(metrics -> new ProviderMetricRow(provider, metrics)))
                .toList();
        BigDecimal maxLatency = rows.stream()
                .map(row -> row.metrics().getAvgLatencyMs())
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal maxCost = rows.stream()
                .map(row -> row.metrics().getAvgCostUsd())
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        List<ProviderBenchmarkResult> results = rows.stream()
                .map(row -> benchmark(row.provider(), row.metrics(), maxLatency, maxCost))
                .sorted(Comparator.comparing(ProviderBenchmarkResult::overallScore).reversed())
                .toList();
        return new ProviderBenchmarkSummary(results);
    }

    private ProviderBenchmarkResult benchmark(
            AiToolProvider provider,
            AiProviderMetrics metrics,
            BigDecimal maxLatency,
            BigDecimal maxCost
    ) {
        BigDecimal speedScore = inverseScore(metrics.getAvgLatencyMs(), maxLatency);
        BigDecimal costScore = inverseScore(metrics.getAvgCostUsd(), maxCost);
        BigDecimal qualityScore = normalizeQuality(metrics.getAvgQualityScore());
        BigDecimal reliabilityScore = reliabilityScorer.reliabilityScore(metrics);
        BigDecimal overall = speedScore
                .add(costScore)
                .add(qualityScore)
                .add(reliabilityScore)
                .divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP);
        return new ProviderBenchmarkResult(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                metrics.getModelName(),
                speedScore,
                costScore,
                qualityScore,
                reliabilityScore,
                overall,
                metrics.getTotalRequests());
    }

    private BigDecimal inverseScore(BigDecimal value, BigDecimal maxValue) {
        if (value == null || maxValue == null || maxValue.signum() <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value.signum() <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal ratio = value.divide(maxValue, 6, RoundingMode.HALF_UP);
        BigDecimal score = BigDecimal.ONE.subtract(ratio);
        if (score.signum() < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return score.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuality(BigDecimal quality) {
        if (quality == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (quality.compareTo(BigDecimal.ONE) > 0 && quality.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return quality.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        if (quality.signum() < 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (quality.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return quality.setScale(4, RoundingMode.HALF_UP);
    }

    private record ProviderMetricRow(AiToolProvider provider, AiProviderMetrics metrics) {
    }
}
