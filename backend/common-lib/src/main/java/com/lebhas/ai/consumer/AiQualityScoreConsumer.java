package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.GeneratedVersionQualityService;
import com.lebhas.ai.application.dto.QualityScoreInput;
import com.lebhas.ai.application.dto.QualityScoreResult;
import com.lebhas.ai.cache.AiQualityScoreCacheService;
import com.lebhas.ai.event.AiQualityScoreCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;

public class AiQualityScoreConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiQualityScoreConsumer.class);

    private final ObjectMapper objectMapper;
    private final GeneratedVersionQualityService qualityService;
    private final AiQualityScoreCacheService qualityScoreCacheService;

    public AiQualityScoreConsumer(
            ObjectMapper objectMapper,
            GeneratedVersionQualityService qualityService,
            AiQualityScoreCacheService qualityScoreCacheService
    ) {
        this.objectMapper = objectMapper;
        this.qualityService = qualityService;
        this.qualityScoreCacheService = qualityScoreCacheService;
    }

    @KafkaListener(
            topics = "#{@aiMonitoringKafkaTopicNames.qualityScoreCreated()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-quality-score-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeQualityScoreCreated(Object payload) {
        AiQualityScoreCreatedEvent event = convert(payload, AiQualityScoreCreatedEvent.class);
        if (event.workspaceId() == null || event.generatedVersionId() == null) {
            log.debug("Skipping AI quality score event because workspaceId/generatedVersionId is missing");
            return;
        }
        try {
            Map<String, Object> metadata = event.metadata();
            QualityScoreResult result = qualityService.scoreGeneratedVersion(new QualityScoreInput(
                    event.workspaceId(),
                    event.generatedVersionId(),
                    AiMonitoringEventSupport.uuidValue(metadata, "providerId"),
                    AiMonitoringEventSupport.uuidValue(metadata, "layerId"),
                    AiMonitoringEventSupport.stringValue(metadata, "modelName"),
                    event.textReadabilityScore(),
                    event.productPreservationScore(),
                    event.brandingScore(),
                    event.banglaTypographyScore(),
                    event.compositionScore(),
                    event.overallScore(),
                    event.qualityNotes(),
                    AiMonitoringEventSupport.decimalValue(metadata, "costUsd"),
                    AiMonitoringEventSupport.decimalValue(metadata, "latencyMs"),
                    true));
            qualityScoreCacheService.store(result);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist AI quality score generatedVersionId={} reason={}",
                    event.generatedVersionId(), exception.getMessage());
        }
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
