package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.AiCostIntelligenceService;
import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.cache.AiLayerAnalyticsCacheEntry;
import com.lebhas.ai.cache.AiLayerAnalyticsCacheService;
import com.lebhas.ai.domain.AiLayerAnalytics;
import com.lebhas.ai.event.AiLayerAnalyticsUpdatedEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.Map;

public class AiLayerAnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiLayerAnalyticsConsumer.class);

    private final ObjectMapper objectMapper;
    private final AiCostIntelligenceService costIntelligenceService;
    private final AiLayerAnalyticsCacheService layerAnalyticsCacheService;
    private final AiMonitoringEventProducer eventProducer;

    public AiLayerAnalyticsConsumer(
            ObjectMapper objectMapper,
            AiCostIntelligenceService costIntelligenceService,
            AiLayerAnalyticsCacheService layerAnalyticsCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.costIntelligenceService = costIntelligenceService;
        this.layerAnalyticsCacheService = layerAnalyticsCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerCompleted()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-layer-analytics-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerCompleted(Object payload) {
        updateLayerAnalytics(convert(payload, AiLayerLifecycleEvent.class), true);
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerFailed()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-layer-analytics-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerFailed(Object payload) {
        updateLayerAnalytics(convert(payload, AiLayerLifecycleEvent.class), false);
    }

    private void updateLayerAnalytics(AiLayerLifecycleEvent event, boolean successful) {
        Map<String, Object> metadata = event.metadata();
        String modelName = AiMonitoringEventSupport.stringValue(metadata, "modelName");
        if (event.layerId() == null || event.providerId() == null || modelName == null) {
            log.debug("Skipping layer analytics update because layerId/providerId/modelName is missing requestId={}", event.creativeRequestId());
            return;
        }
        try {
            AiLayerAnalytics analytics = costIntelligenceService.trackLayerCost(new CostObservation(
                    event.providerId(),
                    event.layerId(),
                    modelName,
                    AiMonitoringEventSupport.decimalValue(metadata, "costUsd"),
                    AiMonitoringEventSupport.decimalValue(metadata, "qualityScore"),
                    AiMonitoringEventSupport.decimalValue(metadata, "latencyMs"),
                    successful,
                    event.occurredAt()));
            layerAnalyticsCacheService.store(new AiLayerAnalyticsCacheEntry(
                    analytics.getId(),
                    analytics.getLayerId(),
                    analytics.getProviderId(),
                    analytics.getModelName(),
                    analytics.getTotalExecutions(),
                    analytics.getSuccessfulExecutions(),
                    analytics.getFailedExecutions(),
                    analytics.getAvgExecutionTimeMs(),
                    analytics.getAvgExecutionCostUsd(),
                    analytics.getAvgQualityScore(),
                    Instant.now()));
            eventProducer.publishLayerAnalyticsUpdated(new AiLayerAnalyticsUpdatedEvent(
                    null,
                    Instant.now(),
                    analytics.getId(),
                    analytics.getLayerId(),
                    analytics.getProviderId(),
                    analytics.getModelName(),
                    analytics.getTotalExecutions(),
                    analytics.getSuccessfulExecutions(),
                    analytics.getFailedExecutions(),
                    analytics.getAvgExecutionTimeMs(),
                    analytics.getAvgExecutionCostUsd(),
                    analytics.getAvgQualityScore(),
                    Map.of("sourceEventId", event.eventId())));
        } catch (RuntimeException exception) {
            log.warn("Failed to update AI layer analytics layerId={} providerId={} reason={}",
                    event.layerId(), event.providerId(), exception.getMessage());
        }
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
