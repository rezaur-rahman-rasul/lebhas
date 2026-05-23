package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.domain.AiProviderMetrics;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiProviderMetricsRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AiProviderHealthService {

    private final AiToolProviderRepository providerRepository;
    private final AiProviderMetricsRepository metricsRepository;
    private final AiProviderMetricsService metricsService;
    private final ProviderReliabilityScorer reliabilityScorer;
    private final ProviderHealthStatusService healthStatusService;

    public AiProviderHealthService(
            AiToolProviderRepository providerRepository,
            AiProviderMetricsRepository metricsRepository,
            AiProviderMetricsService metricsService,
            ProviderReliabilityScorer reliabilityScorer,
            ProviderHealthStatusService healthStatusService
    ) {
        this.providerRepository = providerRepository;
        this.metricsRepository = metricsRepository;
        this.metricsService = metricsService;
        this.reliabilityScorer = reliabilityScorer;
        this.healthStatusService = healthStatusService;
    }

    @Transactional(readOnly = true)
    public ProviderHealthSnapshot getProviderHealth(UUID providerId) {
        AiToolProvider provider = providerRepository.findById(providerId)
                .filter(current -> !current.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
        List<AiProviderMetrics> metrics = metricsRepository.findAllByProviderIdAndDeletedFalse(provider.getId());
        return toHealthSnapshot(provider, metrics);
    }

    @Transactional(readOnly = true)
    public List<ProviderHealthSnapshot> listProviderHealth() {
        return providerRepository.findAllByDeletedFalseOrderByProviderNameAsc().stream()
                .map(provider -> toHealthSnapshot(provider, metricsRepository.findAllByProviderIdAndDeletedFalse(provider.getId())))
                .toList();
    }

    private ProviderHealthSnapshot toHealthSnapshot(AiToolProvider provider, List<AiProviderMetrics> metrics) {
        long totalRequests = metrics.stream().mapToLong(AiProviderMetrics::getTotalRequests).sum();
        long successfulRequests = metrics.stream().mapToLong(AiProviderMetrics::getSuccessfulRequests).sum();
        long failedRequests = metrics.stream().mapToLong(AiProviderMetrics::getFailedRequests).sum();
        Instant lastFailureAt = metrics.stream()
                .map(AiProviderMetrics::getLastFailureAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        Instant lastSuccessAt = metrics.stream()
                .map(AiProviderMetrics::getLastSuccessAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal reliability = reliabilityScorer.aggregateReliabilityScore(metrics);
        return new ProviderHealthSnapshot(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                healthStatusService.status(metrics),
                reliability,
                reliabilityScorer.uptimePercentage(successfulRequests, totalRequests),
                totalRequests,
                successfulRequests,
                failedRequests,
                lastFailureAt,
                lastSuccessAt,
                metrics.stream().map(metric -> metricsService.toSnapshot(provider, metric)).toList());
    }
}
