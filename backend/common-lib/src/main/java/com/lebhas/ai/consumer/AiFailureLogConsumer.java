package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiFailureCacheService;
import com.lebhas.ai.cache.AiFailureRecentCacheEntry;
import com.lebhas.ai.domain.AiFailureLog;
import com.lebhas.ai.domain.AiFailureType;
import com.lebhas.ai.event.AiFailureLoggedEvent;
import com.lebhas.ai.event.AiLayerLifecycleEvent;
import com.lebhas.ai.infrastructure.persistence.AiFailureLogRepository;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.Map;

public class AiFailureLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiFailureLogConsumer.class);

    private final ObjectMapper objectMapper;
    private final AiFailureLogRepository failureLogRepository;
    private final AiFailureCacheService failureCacheService;
    private final AiMonitoringEventProducer eventProducer;

    public AiFailureLogConsumer(
            ObjectMapper objectMapper,
            AiFailureLogRepository failureLogRepository,
            AiFailureCacheService failureCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.failureLogRepository = failureLogRepository;
        this.failureCacheService = failureCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiLayerFailed()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-failure-log-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeLayerFailed(Object payload) {
        AiLayerLifecycleEvent event = convert(payload, AiLayerLifecycleEvent.class);
        Map<String, Object> metadata = event.metadata();
        String modelName = AiMonitoringEventSupport.stringValue(metadata, "modelName");
        if (event.creativeRequestId() == null || event.layerId() == null || event.providerId() == null || modelName == null) {
            log.debug("Skipping AI failure log because required identifiers are missing requestId={}", event.creativeRequestId());
            return;
        }
        try {
            AiFailureLog failureLog = failureLogRepository.save(AiFailureLog.create(
                    event.creativeRequestId(),
                    event.layerId(),
                    event.providerId(),
                    modelName,
                    failureType(metadata),
                    failureReason(event, metadata),
                    event.attempt() == null ? AiMonitoringEventSupport.intValue(metadata, "retryAttempt", 0) : event.attempt(),
                    event.fallbackProviderId() != null || AiMonitoringEventSupport.booleanValue(metadata, "fallbackTriggered", false)));
            failureCacheService.storeRecent(new AiFailureRecentCacheEntry(
                    failureLog.getId(),
                    failureLog.getProviderId(),
                    failureLog.getCreativeRequestId(),
                    failureLog.getLayerId(),
                    failureLog.getModelName(),
                    failureLog.getFailureType(),
                    failureLog.getFailureReason(),
                    failureLog.getRetryAttempt(),
                    failureLog.isFallbackTriggered(),
                    event.occurredAt(),
                    Instant.now()));
            eventProducer.publishFailureLogged(new AiFailureLoggedEvent(
                    null,
                    Instant.now(),
                    failureLog.getId(),
                    failureLog.getCreativeRequestId(),
                    failureLog.getLayerId(),
                    failureLog.getProviderId(),
                    failureLog.getModelName(),
                    failureLog.getFailureType(),
                    failureLog.getFailureReason(),
                    failureLog.getRetryAttempt(),
                    failureLog.isFallbackTriggered(),
                    Map.of("sourceEventId", event.eventId())));
        } catch (RuntimeException exception) {
            log.warn("Failed to log AI failure requestId={} layerId={} providerId={} reason={}",
                    event.creativeRequestId(), event.layerId(), event.providerId(), exception.getMessage());
        }
    }

    private AiFailureType failureType(Map<String, Object> metadata) {
        String value = AiMonitoringEventSupport.stringValue(metadata, "failureType");
        if (value == null) {
            return AiFailureType.UNKNOWN;
        }
        try {
            return AiFailureType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return AiFailureType.UNKNOWN;
        }
    }

    private String failureReason(AiLayerLifecycleEvent event, Map<String, Object> metadata) {
        String metadataReason = AiMonitoringEventSupport.stringValue(metadata, "failureReason");
        if (metadataReason != null) {
            return metadataReason;
        }
        return event.reason() == null ? "AI layer failed" : event.reason();
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
