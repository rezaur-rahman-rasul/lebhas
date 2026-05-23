package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.ProviderBenchmarkResult;
import com.lebhas.ai.application.dto.ProviderBenchmarkSummary;
import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.application.dto.ProviderRoutingCandidate;
import com.lebhas.ai.domain.AiFailureLog;
import com.lebhas.ai.domain.AiLayerAnalytics;
import com.lebhas.ai.infrastructure.persistence.AiFailureLogRepository;
import com.lebhas.ai.infrastructure.persistence.AiLayerAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class ProviderSelectionAdvisor {

    private final ProviderCostComparisonService providerCostComparisonService;
    private final ProviderBenchmarkService providerBenchmarkService;
    private final AiProviderHealthService providerHealthService;
    private final AiLayerAnalyticsRepository layerAnalyticsRepository;
    private final AiFailureLogRepository failureLogRepository;

    public ProviderSelectionAdvisor(
            ProviderCostComparisonService providerCostComparisonService,
            ProviderBenchmarkService providerBenchmarkService,
            AiProviderHealthService providerHealthService,
            AiLayerAnalyticsRepository layerAnalyticsRepository,
            AiFailureLogRepository failureLogRepository
    ) {
        this.providerCostComparisonService = providerCostComparisonService;
        this.providerBenchmarkService = providerBenchmarkService;
        this.providerHealthService = providerHealthService;
        this.layerAnalyticsRepository = layerAnalyticsRepository;
        this.failureLogRepository = failureLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderRoutingCandidate> candidates(UUID layerId, CostEstimateInput input) {
        ProviderBenchmarkSummary benchmarkSummary = providerBenchmarkService.benchmarkProviders();
        return providerCostComparisonService.compareProviderCostEfficiency(layerId, input).stream()
                .map(option -> toCandidate(option, benchmarkSummary.results()))
                .sorted(candidateComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderRoutingCandidate currentProvider(UUID layerId, CostEstimateInput input) {
        ProviderCostOption option = providerCostComparisonService.currentPriorityProvider(layerId, input);
        if (option == null) {
            return null;
        }
        return toCandidate(option, providerBenchmarkService.benchmarkProviders().results());
    }

    public ProviderRoutingCandidate cheapest(List<ProviderRoutingCandidate> candidates, Predicate<ProviderRoutingCandidate> eligibility) {
        return candidates.stream()
                .filter(candidate -> eligibility.test(candidate) && candidate.estimatedCostUsd() != null)
                .min(Comparator.comparing(ProviderRoutingCandidate::estimatedCostUsd))
                .orElse(null);
    }

    public ProviderRoutingCandidate highestQuality(List<ProviderRoutingCandidate> candidates, Predicate<ProviderRoutingCandidate> eligibility) {
        return candidates.stream()
                .filter(candidate -> eligibility.test(candidate) && candidate.qualityScore() != null)
                .max(Comparator.comparing(ProviderRoutingCandidate::qualityScore))
                .orElse(null);
    }

    public ProviderRoutingCandidate fastest(List<ProviderRoutingCandidate> candidates, Predicate<ProviderRoutingCandidate> eligibility) {
        return candidates.stream()
                .filter(candidate -> eligibility.test(candidate) && candidate.avgLatencyMs() != null && candidate.avgLatencyMs().signum() > 0)
                .min(Comparator.comparing(ProviderRoutingCandidate::avgLatencyMs))
                .orElse(null);
    }

    private ProviderRoutingCandidate toCandidate(ProviderCostOption option, List<ProviderBenchmarkResult> benchmarks) {
        ProviderBenchmarkResult benchmark = benchmarks.stream()
                .filter(result -> Objects.equals(result.providerId(), option.providerId()))
                .filter(result -> Objects.equals(result.modelName(), option.modelName()))
                .findFirst()
                .orElse(null);
        AiLayerAnalytics layerAnalytics = layerAnalyticsRepository
                .findByLayerIdAndProviderIdAndModelNameAndDeletedFalse(option.layerId(), option.providerId(), option.modelName())
                .orElse(null);
        ProviderHealthSnapshot health = providerHealth(option.providerId());
        long recentFailureCount = failureLogRepository.findAllByProviderIdAndDeletedFalseOrderByCreatedAtDesc(option.providerId()).stream()
                .limit(20)
                .filter(log -> Objects.equals(log.getLayerId(), option.layerId()))
                .count();
        BigDecimal failureRate = failureRate(layerAnalytics);
        return new ProviderRoutingCandidate(
                option.layerId(),
                option.mappingId(),
                option.providerId(),
                option.providerCode(),
                option.providerName(),
                option.modelId(),
                option.modelCode(),
                option.modelName(),
                option.estimatedCostUsd(),
                firstNonNull(option.qualityScore(), benchmark == null ? null : benchmark.qualityScore(), layerAnalytics == null ? null : layerAnalytics.getAvgQualityScore()),
                option.qualityToCostRatio(),
                layerAnalytics == null ? null : layerAnalytics.getAvgExecutionTimeMs(),
                firstNonNull(health == null ? null : health.reliabilityScore(), benchmark == null ? null : benchmark.reliabilityScore(), null),
                failureRate,
                health == null ? "NO_DATA" : health.healthStatus(),
                benchmark == null ? 0 : benchmark.sampleSize(),
                layerAnalytics == null ? 0 : layerAnalytics.getTotalExecutions(),
                recentFailureCount,
                option.eligible(),
                option.ineligibilityReason());
    }

    private ProviderHealthSnapshot providerHealth(UUID providerId) {
        try {
            return providerHealthService.getProviderHealth(providerId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private BigDecimal failureRate(AiLayerAnalytics analytics) {
        if (analytics == null || analytics.getTotalExecutions() <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(analytics.getFailedExecutions())
                .divide(BigDecimal.valueOf(analytics.getTotalExecutions()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second, BigDecimal third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private Comparator<ProviderRoutingCandidate> candidateComparator() {
        return Comparator
                .comparing((ProviderRoutingCandidate candidate) -> !candidate.eligible())
                .thenComparing(candidate -> candidate.estimatedCostUsd() == null ? BigDecimal.ZERO : candidate.estimatedCostUsd())
                .thenComparing((ProviderRoutingCandidate candidate) -> candidate.qualityToCostRatio() == null ? BigDecimal.ZERO : candidate.qualityToCostRatio(), Comparator.reverseOrder());
    }
}
