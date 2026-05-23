package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressCacheEntry;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "platform.ai.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class CreativeGenerationFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationFailedConsumer.class);

    private final ObjectMapper objectMapper;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final CreativeCreditReservationService creativeCreditReservationService;
    private final AiJobStateRedisService aiJobStateRedisService;
    private final AiGenerationProgressRedisService aiGenerationProgressRedisService;

    public CreativeGenerationFailedConsumer(
            ObjectMapper objectMapper,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            CreativeCreditReservationService creativeCreditReservationService,
            AiJobStateRedisService aiJobStateRedisService,
            AiGenerationProgressRedisService aiGenerationProgressRedisService
    ) {
        this.objectMapper = objectMapper;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.creativeCreditReservationService = creativeCreditReservationService;
        this.aiJobStateRedisService = aiJobStateRedisService;
        this.aiGenerationProgressRedisService = aiGenerationProgressRedisService;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.creativeGenerationFailed()}",
            groupId = "${platform.ai.generation.kafka.consumer-group:${spring.application.name}-ai-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        CreativeGenerationFailedEvent event = toEvent(payload);
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
            log.warn("generation_event type=failed_consumer_skipped workspaceId={} requestId={} versionId={} reason=entity_missing",
                    event.workspaceId(), event.creativeRequestId(), event.generatedVersionId());
            return;
        }

        version.markFailed();
        request.markFailed();
        generatedVersionService.save(version);
        creativeRequestRepository.save(request);

        creativeCreditReservationService.refundCredits(new CreditRefundCommand(
                request.getWorkspaceId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                null,
                null,
                "creative_generation_failed"));

        if (!aiJobStateRedisService.store(new AiJobStateCacheEntry(
                request.getWorkspaceId(),
                request.getId(),
                version.getId(),
                event.providerType(),
                event.model(),
                AiJobState.FAILED,
                1,
                null,
                event.failureReason(),
                Instant.now()))) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state=FAILED",
                    request.getWorkspaceId(), request.getId(), version.getId());
        }
        if (!aiGenerationProgressRedisService.store(new AiGenerationProgressCacheEntry(
                request.getWorkspaceId(),
                request.getId(),
                version.getId(),
                AiJobState.FAILED,
                100,
                "failed",
                event.failureReason(),
                Instant.now()))) {
            log.warn("generation_event type=redis_progress_unavailable workspaceId={} requestId={} jobId={} stage=failed",
                    request.getWorkspaceId(), request.getId(), version.getId());
        }

        log.warn("generation_event type=creative_request_failed workspaceId={} requestId={} versionId={} provider={} model={} retryable={} reason={}",
                request.getWorkspaceId(),
                request.getId(),
                version.getId(),
                event.providerName(),
                event.model(),
                event.retryable(),
                abbreviate(event.failureReason()));
    }

    private CreativeGenerationFailedEvent toEvent(Object payload) {
        try {
            CreativeGenerationFailedEvent event = objectMapper.convertValue(payload, CreativeGenerationFailedEvent.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generatedVersionId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }
}
