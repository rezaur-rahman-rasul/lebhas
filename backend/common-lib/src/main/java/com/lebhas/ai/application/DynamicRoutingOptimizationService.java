package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.DynamicRoutingOptimizationResult;
import com.lebhas.ai.application.dto.RoutingRecommendationView;
import com.lebhas.ai.application.dto.RoutingOptimizationRequest;
import com.lebhas.ai.event.AiRoutingOptimizationRecommendedEvent;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class DynamicRoutingOptimizationService {

    private final RoutingRecommendationService routingRecommendationService;
    private final AiMonitoringEventProducer eventProducer;

    public DynamicRoutingOptimizationService(
            RoutingRecommendationService routingRecommendationService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.routingRecommendationService = routingRecommendationService;
        this.eventProducer = eventProducer;
    }

    @Transactional(readOnly = true)
    public DynamicRoutingOptimizationResult recommendRouting(RoutingOptimizationRequest request) {
        if (request == null || request.layerId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "layerId is required for routing optimization");
        }
        DynamicRoutingOptimizationResult result = routingRecommendationService.recommend(request);
        publishRecommendation(result);
        return result;
    }

    @Transactional(readOnly = true)
    public DynamicRoutingOptimizationResult recommendRouting(
            UUID workspaceId,
            UUID layerId,
            UUID creativeRequestId,
            BigDecimal requestedUnits,
            Map<String, Object> metadata
    ) {
        return recommendRouting(new RoutingOptimizationRequest(workspaceId, layerId, creativeRequestId, requestedUnits, metadata));
    }

    private void publishRecommendation(DynamicRoutingOptimizationResult result) {
        result.recommendations().stream()
                .filter(RoutingRecommendationView::recommended)
                .findFirst()
                .ifPresent(recommendation -> eventProducer.publishRoutingOptimizationRecommended(
                        new AiRoutingOptimizationRecommendedEvent(
                                null,
                                Instant.now(),
                                result.workspaceId(),
                                result.layerId(),
                                recommendation.currentProvider() == null ? null : recommendation.currentProvider().providerId(),
                                recommendation.recommendedProvider() == null ? null : recommendation.recommendedProvider().providerId(),
                                recommendation.estimatedSavingsUsd(),
                                recommendation.reason(),
                                Map.of(
                                        "recommendationType", recommendation.type().name(),
                                        "candidateCount", result.candidates().size()))));
    }
}
