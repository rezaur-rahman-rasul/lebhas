package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;
import com.lebhas.ai.domain.AiProviderMetrics;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiProviderMetricsRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AiProviderMetricsService {

    private final AiProviderMetricsRepository metricsRepository;
    private final AiToolProviderRepository providerRepository;
    private final ProviderReliabilityScorer reliabilityScorer;

    public AiProviderMetricsService(
            AiProviderMetricsRepository metricsRepository,
            AiToolProviderRepository providerRepository,
            ProviderReliabilityScorer reliabilityScorer
    ) {
        this.metricsRepository = metricsRepository;
        this.providerRepository = providerRepository;
        this.reliabilityScorer = reliabilityScorer;
    }

    @Transactional
    public AiProviderMetrics recordRequest(CostObservation observation) {
        UUID providerId = require(observation.providerId(), "providerId");
        String modelName = requireText(observation.modelName(), "modelName");
        providerRepository.findById(providerId)
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
        AiProviderMetrics metrics = metricsRepository.findByProviderIdAndModelNameAndDeletedFalse(providerId, modelName)
                .orElseGet(() -> AiProviderMetrics.create(providerId, modelName));
        long previousTotal = metrics.getTotalRequests();
        long total = previousTotal + 1;
        long successful = metrics.getSuccessfulRequests() + (observation.successful() ? 1 : 0);
        long failed = metrics.getFailedRequests() + (observation.successful() ? 0 : 1);
        Instant occurredAt = observation.occurredAt() == null ? Instant.now() : observation.occurredAt();
        metrics.updateTotals(
                total,
                successful,
                failed,
                weightedAverage(metrics.getAvgLatencyMs(), previousTotal, observation.latencyMs()),
                weightedAverage(metrics.getAvgCostUsd(), previousTotal, observation.costUsd()),
                weightedAverage(metrics.getAvgQualityScore(), previousTotal, observation.qualityScore()),
                reliabilityScorer.uptimePercentage(successful, total),
                observation.successful() ? metrics.getLastFailureAt() : occurredAt,
                observation.successful() ? occurredAt : metrics.getLastSuccessAt());
        return metricsRepository.save(metrics);
    }

    @Transactional(readOnly = true)
    public List<ProviderMetricsSnapshot> getProviderMetrics(UUID providerId) {
        AiToolProvider provider = requireProvider(providerId);
        return metricsRepository.findAllByProviderIdAndDeletedFalse(provider.getId()).stream()
                .map(metrics -> toSnapshot(provider, metrics))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderMetricsSnapshot getProviderModelMetrics(UUID providerId, String modelName) {
        AiToolProvider provider = requireProvider(providerId);
        AiProviderMetrics metrics = metricsRepository
                .findByProviderIdAndModelNameAndDeletedFalse(provider.getId(), requireText(modelName, "modelName"))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider metrics not found"));
        return toSnapshot(provider, metrics);
    }

    ProviderMetricsSnapshot toSnapshot(AiToolProvider provider, AiProviderMetrics metrics) {
        return new ProviderMetricsSnapshot(
                metrics.getId(),
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                metrics.getModelName(),
                metrics.getTotalRequests(),
                metrics.getSuccessfulRequests(),
                metrics.getFailedRequests(),
                metrics.getAvgLatencyMs(),
                metrics.getAvgCostUsd(),
                metrics.getAvgQualityScore(),
                metrics.getUptimePercentage(),
                metrics.getLastFailureAt(),
                metrics.getLastSuccessAt());
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findById(require(providerId, "providerId"))
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }

    private UUID require(UUID value, String field) {
        if (value == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " is required");
        }
        return value;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, field + " is required");
        }
        return value.trim();
    }

    private BigDecimal weightedAverage(BigDecimal previousAverage, long previousCount, BigDecimal nextValue) {
        if (nextValue == null) {
            return previousAverage == null ? BigDecimal.ZERO : previousAverage;
        }
        BigDecimal normalizedNext = nextValue.signum() < 0 ? BigDecimal.ZERO : nextValue;
        if (previousCount <= 0 || previousAverage == null) {
            return normalizedNext.setScale(6, RoundingMode.HALF_UP);
        }
        return previousAverage.multiply(BigDecimal.valueOf(previousCount))
                .add(normalizedNext)
                .divide(BigDecimal.valueOf(previousCount + 1), 6, RoundingMode.HALF_UP);
    }
}
