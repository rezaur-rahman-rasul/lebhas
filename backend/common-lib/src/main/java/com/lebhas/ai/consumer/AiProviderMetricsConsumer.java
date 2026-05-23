package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.AiProviderHealthService;
import com.lebhas.ai.application.AiProviderMetricsService;
import com.lebhas.ai.application.dto.CostObservation;
import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;
import com.lebhas.ai.cache.AiProviderHealthCacheService;
import com.lebhas.ai.cache.AiProviderMetricsCacheService;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.event.AiProviderHealthChangedEvent;
import com.lebhas.ai.event.AiProviderMetricsUpdatedEvent;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AiProviderMetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiProviderMetricsConsumer.class);

    private final ObjectMapper objectMapper;
    private final AiProviderMetricsService providerMetricsService;
    private final AiProviderHealthService providerHealthService;
    private final AiProviderMetricsCacheService providerMetricsCacheService;
    private final AiProviderHealthCacheService providerHealthCacheService;
    private final AiMonitoringEventProducer eventProducer;

    public AiProviderMetricsConsumer(
            ObjectMapper objectMapper,
            AiProviderMetricsService providerMetricsService,
            AiProviderHealthService providerHealthService,
            AiProviderMetricsCacheService providerMetricsCacheService,
            AiProviderHealthCacheService providerHealthCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.providerMetricsService = providerMetricsService;
        this.providerHealthService = providerHealthService;
        this.providerMetricsCacheService = providerMetricsCacheService;
        this.providerHealthCacheService = providerHealthCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerCompleted()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-provider-metrics-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerCompleted(Object payload) {
        updateProviderMetrics(convert(payload, AiLayerLifecycleEvent.class), true);
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerFailed()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-provider-metrics-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerFailed(Object payload) {
        updateProviderMetrics(convert(payload, AiLayerLifecycleEvent.class), false);
    }

    private void updateProviderMetrics(AiLayerLifecycleEvent event, boolean successful) {
        Map<String, Object> metadata = event.metadata();
        String modelName = AiMonitoringEventSupport.stringValue(metadata, "modelName");
        if (event.providerId() == null || modelName == null) {
            log.debug("Skipping provider metrics update because providerId/modelName is missing layerId={}", event.layerId());
            return;
        }
        try {
            providerMetricsService.recordRequest(new CostObservation(
                    event.providerId(),
                    event.layerId(),
                    modelName,
                    AiMonitoringEventSupport.decimalValue(metadata, "costUsd"),
                    AiMonitoringEventSupport.decimalValue(metadata, "qualityScore"),
                    AiMonitoringEventSupport.decimalValue(metadata, "latencyMs"),
                    successful,
                    event.occurredAt()));
            ProviderMetricsSnapshot metrics = providerMetricsService.getProviderModelMetrics(event.providerId(), modelName);
            providerMetricsCacheService.store(metrics);
            eventProducer.publishProviderMetricsUpdated(new AiProviderMetricsUpdatedEvent(
                    null,
                    Instant.now(),
                    metrics.providerId(),
                    metrics.modelName(),
                    metrics.totalRequests(),
                    metrics.successfulRequests(),
                    metrics.failedRequests(),
                    metrics.avgLatencyMs(),
                    metrics.avgCostUsd(),
                    metrics.avgQualityScore(),
                    metrics.uptimePercentage(),
                    Map.of("sourceEventId", event.eventId())));
            ProviderHealthSnapshot health = providerHealthService.getProviderHealth(event.providerId());
            providerHealthCacheService.store(health);
            eventProducer.publishProviderHealthChanged(new AiProviderHealthChangedEvent(
                    null,
                    Instant.now(),
                    health.providerId(),
                    health.healthStatus(),
                    health.reliabilityScore(),
                    health.uptimePercentage(),
                    health.totalRequests(),
                    health.failedRequests(),
                    Map.of("sourceEventId", event.eventId())));
        } catch (RuntimeException exception) {
            log.warn("Failed to update AI provider metrics providerId={} layerId={} reason={}",
                    event.providerId(), event.layerId(), exception.getMessage());
        }
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
