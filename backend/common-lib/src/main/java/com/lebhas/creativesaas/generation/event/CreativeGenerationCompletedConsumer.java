package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.dto.CreditFinalizeCommand;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "platform.ai.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class CreativeGenerationCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationCompletedConsumer.class);

    private final ObjectMapper objectMapper;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final CreativeCreditReservationService creativeCreditReservationService;
    private final AiJobStateRedisService aiJobStateRedisService;
    private final AiGenerationProgressRedisService aiGenerationProgressRedisService;
    private final AiRetryThrottleService aiRetryThrottleService;

    public CreativeGenerationCompletedConsumer(
            ObjectMapper objectMapper,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            CreativeCreditReservationService creativeCreditReservationService,
            AiJobStateRedisService aiJobStateRedisService,
            AiGenerationProgressRedisService aiGenerationProgressRedisService,
            AiRetryThrottleService aiRetryThrottleService
    ) {
        this.objectMapper = objectMapper;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.creativeCreditReservationService = creativeCreditReservationService;
        this.aiJobStateRedisService = aiJobStateRedisService;
        this.aiGenerationProgressRedisService = aiGenerationProgressRedisService;
        this.aiRetryThrottleService = aiRetryThrottleService;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.creativeGenerationCompleted()}",
            groupId = "${platform.ai.generation.kafka.consumer-group:${spring.application.name}-ai-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        CreativeGenerationCompletedEvent event = toEvent(payload);
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
            log.warn("generation_event type=completed_consumer_skipped workspaceId={} requestId={} versionId={} reason=entity_missing",
                    event.workspaceId(), event.creativeRequestId(), event.generatedVersionId());
            return;
        }

        version.markReady(event.storageFileId(), event.assetId(), event.providerName(), event.model());
        request.markCompleted();
        generatedVersionService.save(version);
        creativeRequestRepository.save(request);

        creativeCreditReservationService.finalizeCredits(new CreditFinalizeCommand(
                request.getWorkspaceId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                null,
                null,
                "creative_generation_completed"));

        if (!aiJobStateRedisService.store(new AiJobStateCacheEntry(
                request.getWorkspaceId(),
                request.getId(),
                version.getId(),
                event.providerType(),
                event.model(),
                AiJobState.COMPLETED,
                1,
                event.providerJobId(),
                event.message(),
                Instant.now()))) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state=COMPLETED",
                    request.getWorkspaceId(), request.getId(), version.getId());
        }
        if (!aiGenerationProgressRedisService.clear(request.getWorkspaceId(), request.getId(), version.getId())) {
            log.warn("generation_event type=redis_progress_clear_unavailable workspaceId={} requestId={} jobId={}",
                    request.getWorkspaceId(), request.getId(), version.getId());
        }
        aiRetryThrottleService.clear(request.getWorkspaceId(), request.getId());
    }

    private CreativeGenerationCompletedEvent toEvent(Object payload) {
        try {
            CreativeGenerationCompletedEvent event = objectMapper.convertValue(payload, CreativeGenerationCompletedEvent.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generatedVersionId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
