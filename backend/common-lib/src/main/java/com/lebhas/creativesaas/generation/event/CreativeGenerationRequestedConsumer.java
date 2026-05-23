package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressCacheEntry;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.dto.AiGenerationResponse;
import com.lebhas.ai.job.AiGenerationJobService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "platform.ai.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class CreativeGenerationRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationRequestedConsumer.class);

    private final ObjectMapper objectMapper;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final AiJobStateRedisService aiJobStateRedisService;
    private final AiGenerationProgressRedisService aiGenerationProgressRedisService;
    private final AiGenerationJobService aiGenerationJobService;
    private final CreativeGenerationEventProducer creativeGenerationEventProducer;

    public CreativeGenerationRequestedConsumer(
            ObjectMapper objectMapper,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            AiJobStateRedisService aiJobStateRedisService,
            AiGenerationProgressRedisService aiGenerationProgressRedisService,
            AiGenerationJobService aiGenerationJobService,
            CreativeGenerationEventProducer creativeGenerationEventProducer
    ) {
        this.objectMapper = objectMapper;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.aiJobStateRedisService = aiJobStateRedisService;
        this.aiGenerationProgressRedisService = aiGenerationProgressRedisService;
        this.aiGenerationJobService = aiGenerationJobService;
        this.creativeGenerationEventProducer = creativeGenerationEventProducer;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.creativeGenerationRequested()}",
            groupId = "${platform.ai.generation.kafka.consumer-group:${spring.application.name}-ai-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        CreativeGenerationRequestedEvent event = toEvent(payload);
        if (event == null) {
            return;
        }

        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(event.creativeRequestId(), event.workspaceId())
                .orElse(null);
        GeneratedVersionEntity version = generatedVersionService
                .findByIdAndWorkspaceId(event.workspaceId(), event.generatedVersionId())
                .orElse(null);
        if (request == null || version == null) {
            log.warn("generation_event type=requested_consumer_skipped workspaceId={} requestId={} versionId={} reason=entity_missing",
                    event.workspaceId(), event.creativeRequestId(), event.generatedVersionId());
            return;
        }
        if (request.getStatus() == CreativeRequestStatus.CANCELLED) {
            log.info("generation_event type=requested_consumer_skipped workspaceId={} requestId={} versionId={} reason=request_cancelled",
                    event.workspaceId(), event.creativeRequestId(), event.generatedVersionId());
            return;
        }

        request.markProcessing();
        version.markProcessing();
        creativeRequestRepository.save(request);
        generatedVersionService.save(version);

        updateJobState(new AiJobStateCacheEntry(
                event.workspaceId(),
                event.creativeRequestId(),
                event.generatedVersionId(),
                event.providerType(),
                event.model(),
                AiJobState.PROCESSING,
                1,
                null,
                "AI generation started",
                Instant.now()));
        updateProgress(event.workspaceId(), event.creativeRequestId(), event.generatedVersionId(), AiJobState.PROCESSING, 10, "processing", "AI generation started");
        creativeGenerationEventProducer.publishCreativeGenerationStarted(new CreativeGenerationStartedEvent(
                null,
                null,
                event.workspaceId(),
                event.creativeRequestId(),
                event.generatedVersionId(),
                event.creditReservationId(),
                event.providerType(),
                event.model(),
                1));

        try {
            AiGenerationResponse response = aiGenerationJobService.execute(event.toAiGenerationRequest());
            if (response.success()) {
                creativeGenerationEventProducer.publishCreativeGenerationCompleted(new CreativeGenerationCompletedEvent(
                        null,
                        null,
                        event.workspaceId(),
                        event.creativeRequestId(),
                        event.generatedVersionId(),
                        event.creditReservationId(),
                        response.providerType(),
                        response.providerName(),
                        response.model(),
                        response.providerJobId(),
                        extractUuid(response.metadata(), "storageFileId"),
                        extractUuid(response.metadata(), "assetId"),
                        response.mimeType(),
                        response.width(),
                        response.height(),
                        response.duration(),
                        response.metadata(),
                        response.message()));
                return;
            }

            creativeGenerationEventProducer.publishCreativeGenerationFailed(new CreativeGenerationFailedEvent(
                    null,
                    null,
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generatedVersionId(),
                    event.creditReservationId(),
                    response.providerType(),
                    response.providerName(),
                    response.model(),
                    response.message(),
                    retryable(response.metadata()),
                    response.metadata()));
        } catch (RuntimeException exception) {
            creativeGenerationEventProducer.publishCreativeGenerationFailed(new CreativeGenerationFailedEvent(
                    null,
                    null,
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generatedVersionId(),
                    event.creditReservationId(),
                    event.providerType(),
                    event.providerType() == null ? null : event.providerType().name(),
                    event.model(),
                    failureReason(exception),
                    false,
                    Map.of("foundationOnly", true, "exceptionType", exception.getClass().getSimpleName())));
        }
    }

    private CreativeGenerationRequestedEvent toEvent(Object payload) {
        try {
            CreativeGenerationRequestedEvent event = objectMapper.convertValue(payload, CreativeGenerationRequestedEvent.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generatedVersionId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void updateJobState(AiJobStateCacheEntry entry) {
        if (!aiJobStateRedisService.store(entry)) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state={}",
                    entry.workspaceId(), entry.creativeRequestId(), entry.jobId(), entry.state());
        }
    }

    private void updateProgress(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID jobId,
            AiJobState state,
            int progress,
            String stage,
            String message
    ) {
        if (!aiGenerationProgressRedisService.store(new AiGenerationProgressCacheEntry(
                workspaceId,
                creativeRequestId,
                jobId,
                state,
                progress,
                stage,
                message,
                Instant.now()))) {
            log.warn("generation_event type=redis_progress_unavailable workspaceId={} requestId={} jobId={} stage={}",
                    workspaceId, creativeRequestId, jobId, stage);
        }
    }

    private UUID extractUuid(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean retryable(Map<String, Object> metadata) {
        if (metadata == null) {
            return false;
        }
        Object value = metadata.get("retryable");
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private String failureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }
}
