package com.lebhas.ai.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.application.WorkspaceAiUsageService;
import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.cache.WorkspaceAiUsageCacheService;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.event.AiWorkspaceUsageUpdatedEvent;
import com.lebhas.ai.producer.AiMonitoringEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class AiWorkspaceUsageConsumer {

    private static final Logger log = LoggerFactory.getLogger(AiWorkspaceUsageConsumer.class);

    private final ObjectMapper objectMapper;
    private final WorkspaceAiUsageService workspaceAiUsageService;
    private final WorkspaceAiUsageCacheService workspaceAiUsageCacheService;
    private final AiMonitoringEventProducer eventProducer;

    public AiWorkspaceUsageConsumer(
            ObjectMapper objectMapper,
            WorkspaceAiUsageService workspaceAiUsageService,
            WorkspaceAiUsageCacheService workspaceAiUsageCacheService,
            AiMonitoringEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.workspaceAiUsageService = workspaceAiUsageService;
        this.workspaceAiUsageCacheService = workspaceAiUsageCacheService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationRequested()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-workspace-usage-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationRequested(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        try {
            updateUsage(event, workspaceAiUsageService.recordGenerationRequested(event.workspaceId()));
        } catch (RuntimeException exception) {
            log.warn("Failed to record AI generation request usage workspaceId={} requestId={} reason={}",
                    event.workspaceId(), event.creativeRequestId(), exception.getMessage());
        }
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationCompleted()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-workspace-usage-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationCompleted(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        Map<String, Object> metadata = event.metadata();
        long generatedVersions = AiMonitoringEventSupport.longValue(metadata, "generatedVersions", event.generatedVersionId() == null ? 0 : 1);
        BigDecimal creditsConsumed = AiMonitoringEventSupport.decimalValue(metadata, "creditsConsumed");
        BigDecimal estimatedCostUsd = AiMonitoringEventSupport.decimalValue(metadata, "estimatedCostUsd");
        BigDecimal generationTimeMs = AiMonitoringEventSupport.decimalValue(metadata, "generationTimeMs");
        try {
            updateUsage(event, workspaceAiUsageService.recordGenerationCompleted(
                    event.workspaceId(),
                    generatedVersions,
                    creditsConsumed,
                    estimatedCostUsd,
                    generationTimeMs));
        } catch (RuntimeException exception) {
            log.warn("Failed to record AI generation completed usage workspaceId={} requestId={} reason={}",
                    event.workspaceId(), event.creativeRequestId(), exception.getMessage());
        }
    }

    @KafkaListener(
            topics = "#{@aiCreativePipelineKafkaTopicNames.aiGenerationFailed()}",
            groupId = "${spring.kafka.consumer.group-id:creative-service}-ai-workspace-usage-monitoring",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationFailed(Object payload) {
        AiGenerationLifecycleEvent event = convert(payload, AiGenerationLifecycleEvent.class);
        Map<String, Object> metadata = event.metadata();
        try {
            updateUsage(event, workspaceAiUsageService.recordGenerationFailure(
                    event.workspaceId(),
                    AiMonitoringEventSupport.decimalValue(metadata, "estimatedCostUsd"),
                    AiMonitoringEventSupport.decimalValue(metadata, "generationTimeMs")));
        } catch (RuntimeException exception) {
            log.warn("Failed to record AI generation failure usage workspaceId={} requestId={} reason={}",
                    event.workspaceId(), event.creativeRequestId(), exception.getMessage());
        }
    }

    private void updateUsage(AiGenerationLifecycleEvent source, WorkspaceAiUsageView usage) {
        try {
            workspaceAiUsageCacheService.store(usage);
            eventProducer.publishWorkspaceUsageUpdated(new AiWorkspaceUsageUpdatedEvent(
                    null,
                    Instant.now(),
                    usage.id(),
                    usage.workspaceId(),
                    usage.totalGenerationRequests(),
                    usage.totalGeneratedVersions(),
                    usage.totalCreditsConsumed(),
                    usage.totalEstimatedCostUsd(),
                    usage.totalFailures(),
                    usage.avgGenerationTimeMs(),
                    Map.of("sourceEventId", source.eventId())));
        } catch (RuntimeException exception) {
            log.warn("Failed to publish AI workspace usage update workspaceId={} reason={}", usage.workspaceId(), exception.getMessage());
        }
    }

    private <T> T convert(Object payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }
}
