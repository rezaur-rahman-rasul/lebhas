package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.GenerationCostEstimate;
import com.lebhas.ai.application.dto.LayerCostEstimate;
import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GenerationCostEstimator {

    private final CreativePipelineRepository pipelineRepository;
    private final CreativePipelineLayerRepository layerRepository;
    private final ProviderCostComparisonService providerCostComparisonService;
    private final LayerCostOptimizationService layerCostOptimizationService;
    private final AiMonitoringEventProducer eventProducer;

    public GenerationCostEstimator(
            CreativePipelineRepository pipelineRepository,
            CreativePipelineLayerRepository layerRepository,
            ProviderCostComparisonService providerCostComparisonService,
            LayerCostOptimizationService layerCostOptimizationService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.pipelineRepository = pipelineRepository;
        this.layerRepository = layerRepository;
        this.providerCostComparisonService = providerCostComparisonService;
        this.layerCostOptimizationService = layerCostOptimizationService;
        this.eventProducer = eventProducer;
    }

    @Transactional(readOnly = true)
    public GenerationCostEstimate estimateGenerationCost(UUID pipelineId, CostEstimateInput input) {
        CreativePipeline pipeline = pipelineRepository.findById(pipelineId)
                .filter(current -> !current.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative pipeline not found"));
        List<LayerCostEstimate> layerEstimates = layerRepository
                .findAllByPipelineIdAndDeletedFalseOrderBySortOrderAsc(pipeline.getId()).stream()
                .filter(CreativePipelineLayer::isEnabled)
                .map(layer -> estimatePerLayerCost(layer.getId(), input))
                .toList();
        BigDecimal total = layerEstimates.stream()
                .map(LayerCostEstimate::estimatedCostUsd)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
        CostEstimateInput normalizedInput = input == null ? CostEstimateInput.defaultInput() : input;
        GenerationCostEstimate estimate = new GenerationCostEstimate(
                pipeline.getId(),
                normalizedInput.workspaceId(),
                normalizedInput.creativeRequestId(),
                total,
                layerEstimates);
        eventProducer.publishCostEstimated(new AiCostEvent(
                null,
                Instant.now(),
                estimate.workspaceId(),
                estimate.creativeRequestId(),
                null,
                null,
                estimate.pipelineId(),
                estimate.totalEstimatedCostUsd(),
                "USD",
                Map.of("layerCount", estimate.layerEstimates().size()),
                Map.of("source", "generation-cost-estimator")));
        return estimate;
    }

    @Transactional(readOnly = true)
    public LayerCostEstimate estimatePerLayerCost(UUID layerId, CostEstimateInput input) {
        List<ProviderCostOption> options = providerCostComparisonService.compareProviderCostEfficiency(layerId, input);
        ProviderCostOption recommended = layerCostOptimizationService.eligibleCostOptions(layerId, input).stream()
                .findFirst()
                .orElseGet(() -> options.stream().findFirst().orElse(null));
        BigDecimal estimatedCost = recommended == null ? null : recommended.estimatedCostUsd();
        return new LayerCostEstimate(layerId, estimatedCost, recommended, options);
    }
}
