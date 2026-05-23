package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.application.dto.GenerationCostEstimate;
import com.lebhas.ai.application.dto.LayerCostEstimate;
import com.lebhas.ai.application.dto.LayerCostRecommendation;
import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.domain.AiLayerAnalytics;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiProviderMetrics;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiLayerAnalyticsRepository;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
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
public class AiCostIntelligenceService {

    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiLayerAnalyticsRepository layerAnalyticsRepository;
    private final AiProviderMetricsService providerMetricsService;
    private final CostEfficiencyCalculator costEfficiencyCalculator;
    private final ProviderCostComparisonService providerCostComparisonService;
    private final LayerCostOptimizationService layerCostOptimizationService;
    private final GenerationCostEstimator generationCostEstimator;

    public AiCostIntelligenceService(
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiLayerAnalyticsRepository layerAnalyticsRepository,
            AiProviderMetricsService providerMetricsService,
            CostEfficiencyCalculator costEfficiencyCalculator,
            ProviderCostComparisonService providerCostComparisonService,
            LayerCostOptimizationService layerCostOptimizationService,
            GenerationCostEstimator generationCostEstimator
    ) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.layerAnalyticsRepository = layerAnalyticsRepository;
        this.providerMetricsService = providerMetricsService;
        this.costEfficiencyCalculator = costEfficiencyCalculator;
        this.providerCostComparisonService = providerCostComparisonService;
        this.layerCostOptimizationService = layerCostOptimizationService;
        this.generationCostEstimator = generationCostEstimator;
    }

    @Transactional(readOnly = true)
    public ProviderCostOption estimateProviderCost(UUID providerId, UUID modelId, CostEstimateInput input) {
        AiToolProvider provider = requireProvider(providerId);
        AiModel model = modelId == null ? null : requireModel(provider.getId(), modelId);
        CostEstimateInput normalizedInput = input == null ? CostEstimateInput.defaultInput() : input;
        return costEfficiencyCalculator.toOption(
                null,
                null,
                provider,
                model,
                normalizedInput.requestedUnits(),
                provider.isEnabled(),
                provider.isEnabled() ? null : "Provider is disabled");
    }

    @Transactional(readOnly = true)
    public LayerCostEstimate estimatePerLayerCost(UUID layerId, CostEstimateInput input) {
        return generationCostEstimator.estimatePerLayerCost(layerId, input);
    }

    @Transactional(readOnly = true)
    public GenerationCostEstimate estimateGenerationCost(UUID pipelineId, CostEstimateInput input) {
        return generationCostEstimator.estimateGenerationCost(pipelineId, input);
    }

    @Transactional(readOnly = true)
    public List<ProviderCostOption> compareProviderCostEfficiency(UUID layerId, CostEstimateInput input) {
        return providerCostComparisonService.compareProviderCostEfficiency(layerId, input);
    }

    @Transactional(readOnly = true)
    public LayerCostRecommendation recommendCheaperRouting(UUID layerId, CostEstimateInput input) {
        return layerCostOptimizationService.recommendCheaperRouting(layerId, input);
    }

    @Transactional
    public AiProviderMetrics trackProviderCost(CostObservation observation) {
        return providerMetricsService.recordRequest(observation);
    }

    @Transactional
    public AiLayerAnalytics trackLayerCost(CostObservation observation) {
        UUID layerId = require(observation.layerId(), "layerId");
        UUID providerId = require(observation.providerId(), "providerId");
        String modelName = requireText(observation.modelName(), "modelName");
        AiLayerAnalytics analytics = layerAnalyticsRepository
                .findByLayerIdAndProviderIdAndModelNameAndDeletedFalse(layerId, providerId, modelName)
                .orElseGet(() -> AiLayerAnalytics.create(layerId, providerId, modelName));
        long previousTotal = analytics.getTotalExecutions();
        long total = previousTotal + 1;
        analytics.updateTotals(
                total,
                analytics.getSuccessfulExecutions() + (observation.successful() ? 1 : 0),
                analytics.getFailedExecutions() + (observation.successful() ? 0 : 1),
                weightedAverage(analytics.getAvgExecutionTimeMs(), previousTotal, observation.latencyMs()),
                weightedAverage(analytics.getAvgExecutionCostUsd(), previousTotal, observation.costUsd()),
                weightedAverage(analytics.getAvgQualityScore(), previousTotal, observation.qualityScore()));
        return layerAnalyticsRepository.save(analytics);
    }

    @Transactional
    public AiProviderMetrics trackModelCost(CostObservation observation) {
        return trackProviderCost(observation);
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findById(providerId)
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }

    private AiModel requireModel(UUID providerId, UUID modelId) {
        return modelRepository.findById(modelId)
                .filter(model -> !model.isDeleted())
                .filter(model -> model.getProviderId().equals(providerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI model not found"));
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
        if (previousCount <= 0 || previousAverage == null) {
            return nonNegative(nextValue);
        }
        BigDecimal previousWeighted = previousAverage.multiply(BigDecimal.valueOf(previousCount));
        return previousWeighted.add(nonNegative(nextValue))
                .divide(BigDecimal.valueOf(previousCount + 1), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
