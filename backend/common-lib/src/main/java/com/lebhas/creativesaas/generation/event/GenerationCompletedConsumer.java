package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreditReservationService;
import com.lebhas.creativesaas.generation.application.GenerationJobService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "platform.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class GenerationCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationCompletedConsumer.class);
    private static final String CREDIT_REFERENCE_TYPE = "creative_request_generation";

    private final ObjectMapper objectMapper;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final GenerationJobService generationJobService;
    private final CreditReservationService creditReservationService;
    private final AiJobStateRedisService jobStateRedisService;
    private final AiGenerationProgressRedisService progressRedisService;
    private final AiRetryThrottleService retryThrottleService;
    private final GenerationEventProducer eventProducer;

    public GenerationCompletedConsumer(
            ObjectMapper objectMapper,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            GenerationJobService generationJobService,
            CreditReservationService creditReservationService,
            AiJobStateRedisService jobStateRedisService,
            AiGenerationProgressRedisService progressRedisService,
            AiRetryThrottleService retryThrottleService,
            GenerationEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.generationJobService = generationJobService;
        this.creditReservationService = creditReservationService;
        this.jobStateRedisService = jobStateRedisService;
        this.progressRedisService = progressRedisService;
        this.retryThrottleService = retryThrottleService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.generationCompleted()}",
            groupId = "${platform.generation.kafka.consumer-group:${spring.application.name}-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        GenerationCompletedEventDto event = toEvent(payload);
        if (event == null || event.finalized()) {
            return;
        }
        complete(event);
    }

    @Transactional
    protected void complete(GenerationCompletedEventDto event) {
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(event.creativeRequestId(), event.workspaceId())
                .orElse(null);
        if (request == null) {
            log.warn("generation_event type=completed_skipped workspaceId={} requestId={} reason=request_missing",
                    event.workspaceId(), event.creativeRequestId());
            return;
        }

        GenerationJobEntity job = generationJobService.complete(event.workspaceId(), event.generationJobId(), event.providerJobId());
        boolean createdVersion = event.generatedVersionId() == null;
        GeneratedVersionEntity version = createdVersion
                ? generatedVersionService.createCompleted(
                        request,
                        request.getCreatedByUserId(),
                        generatedVersionService.nextVersionNumber(event.workspaceId(), request.getId()),
                        event.storageFileId(),
                        event.assetId(),
                        event.previewAssetId(),
                        event.thumbnailAssetId(),
                        event.providerName(),
                        event.model())
                : updateExistingVersion(event, request);
        if (!createdVersion) {
            generatedVersionService.publishGeneratedVersionCreated(version);
        }

        creditReservationService.finalize(
                event.workspaceId(),
                request.getId(),
                version.getId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                CREDIT_REFERENCE_TYPE,
                request.getId(),
                event.finalizedCredits(),
                "generation_completed");

        request.markGenerationCompleted(Instant.now(), readyVersionCount(event.workspaceId(), request.getId()));
        creativeRequestRepository.save(request);
        storeRedisCompleted(job, version, event);
        retryThrottleService.clear(event.workspaceId(), request.getId());
        eventProducer.publishGenerationCompleted(new GenerationCompletedEventDto(
                event.workspaceId(),
                request.getId(),
                job.getId(),
                version.getId(),
                version.getAssetId(),
                version.getStorageFileId(),
                event.previewAssetId(),
                event.thumbnailAssetId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                event.finalizedCredits(),
                event.providerName(),
                event.model(),
                event.providerJobId(),
                true,
                Instant.now()));
    }

    private GeneratedVersionEntity updateExistingVersion(GenerationCompletedEventDto event, CreativeRequestEntity request) {
        GeneratedVersionEntity version = generatedVersionService.requireByIdAndWorkspaceId(event.workspaceId(), event.generatedVersionId());
        if (event.assetId() != null || event.storageFileId() != null) {
            version.recordGeneratedAsset(event.assetId(), event.previewAssetId(), event.thumbnailAssetId(), null, null, null, null, null, null);
        }
        version.markReady(event.storageFileId(), event.assetId(), event.providerName(), event.model());
        return generatedVersionService.save(version);
    }

    private void storeRedisCompleted(GenerationJobEntity job, GeneratedVersionEntity version, GenerationCompletedEventDto event) {
        if (!jobStateRedisService.store(new AiJobStateCacheEntry(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                version.getId(),
                null,
                event.model(),
                AiJobState.COMPLETED,
                job.getAttemptCount(),
                event.providerJobId(),
                "Generation completed",
                Instant.now()))) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state=COMPLETED",
                    job.getWorkspaceId(), job.getCreativeRequestId(), job.getId());
        }
        if (!progressRedisService.clear(job.getWorkspaceId(), job.getCreativeRequestId(), version.getId())) {
            log.warn("generation_event type=redis_progress_clear_unavailable workspaceId={} requestId={} versionId={}",
                    job.getWorkspaceId(), job.getCreativeRequestId(), version.getId());
        }
    }

    private int readyVersionCount(java.util.UUID workspaceId, java.util.UUID creativeRequestId) {
        return (int) generatedVersionService.listByCreativeRequest(workspaceId, creativeRequestId).stream()
                .filter(version -> version.getGenerationStatus() == GenerationStatus.READY)
                .count();
    }

    private GenerationCompletedEventDto toEvent(Object payload) {
        try {
            GenerationCompletedEventDto event = objectMapper.convertValue(payload, GenerationCompletedEventDto.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generationJobId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
