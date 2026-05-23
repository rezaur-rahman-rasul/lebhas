package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressCacheEntry;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreditReservationService;
import com.lebhas.creativesaas.generation.application.GenerationJobService;
import com.lebhas.creativesaas.generation.application.GenerationRetryService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "platform.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class GenerationFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationFailedConsumer.class);
    private static final String CREDIT_REFERENCE_TYPE = "creative_request_generation";

    private final ObjectMapper objectMapper;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final GenerationJobService generationJobService;
    private final CreditReservationService creditReservationService;
    private final GenerationRetryService generationRetryService;
    private final AiJobStateRedisService jobStateRedisService;
    private final AiGenerationProgressRedisService progressRedisService;
    private final GenerationEventProducer eventProducer;

    public GenerationFailedConsumer(
            ObjectMapper objectMapper,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            GenerationJobService generationJobService,
            CreditReservationService creditReservationService,
            GenerationRetryService generationRetryService,
            AiJobStateRedisService jobStateRedisService,
            AiGenerationProgressRedisService progressRedisService,
            GenerationEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.generationJobService = generationJobService;
        this.creditReservationService = creditReservationService;
        this.generationRetryService = generationRetryService;
        this.jobStateRedisService = jobStateRedisService;
        this.progressRedisService = progressRedisService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.generationFailed()}",
            groupId = "${platform.generation.kafka.consumer-group:${spring.application.name}-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        GenerationFailedEventDto event = toEvent(payload);
        if (event == null || event.finalized()) {
            return;
        }
        fail(event);
    }

    @Transactional
    protected void fail(GenerationFailedEventDto event) {
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(event.creativeRequestId(), event.workspaceId())
                .orElse(null);
        if (request == null) {
            log.warn("generation_event type=failed_skipped workspaceId={} requestId={} reason=request_missing",
                    event.workspaceId(), event.creativeRequestId());
            return;
        }

        GenerationJobEntity job = generationJobService.fail(event.workspaceId(), event.generationJobId(), event.failureReason());
        GeneratedVersionEntity version = event.generatedVersionId() == null
                ? generatedVersionService.latestByCreativeRequest(event.workspaceId(), request.getId()).orElse(null)
                : generatedVersionService.findByIdAndWorkspaceId(event.workspaceId(), event.generatedVersionId()).orElse(null);
        if (version != null) {
            version.markFailed(event.failureReason());
            generatedVersionService.save(version);
        }
        request.markGenerationFailed(event.failureReason(), Instant.now());
        creativeRequestRepository.save(request);
        creditReservationService.refund(
                event.workspaceId(),
                request.getId(),
                version == null ? null : version.getId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                CREDIT_REFERENCE_TYPE,
                request.getId(),
                event.refundedCredits(),
                "generation_failed");
        try {
            generationRetryService.validateRetryAllowed(event.workspaceId(), request.getId());
        } catch (RuntimeException exception) {
            log.warn("generation_event type=retry_state_increment_failed workspaceId={} requestId={} reason={}",
                    event.workspaceId(), request.getId(), exception.getMessage());
        }
        storeRedisFailed(job, version, event);
        eventProducer.publishGenerationFailed(new GenerationFailedEventDto(
                event.workspaceId(),
                request.getId(),
                job.getId(),
                version == null ? null : version.getId(),
                event.creditReservationId() == null ? request.getCreditReservationId() : event.creditReservationId(),
                event.refundedCredits(),
                event.failureReason(),
                job.canRetry(),
                true,
                Instant.now()));
    }

    private void storeRedisFailed(GenerationJobEntity job, GeneratedVersionEntity version, GenerationFailedEventDto event) {
        java.util.UUID redisJobId = version == null ? job.getId() : version.getId();
        if (!jobStateRedisService.store(new AiJobStateCacheEntry(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                redisJobId,
                null,
                job.getModel(),
                AiJobState.FAILED,
                job.getAttemptCount(),
                job.getProviderJobId(),
                event.failureReason(),
                Instant.now()))) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state=FAILED",
                    job.getWorkspaceId(), job.getCreativeRequestId(), redisJobId);
        }
        if (!progressRedisService.store(new AiGenerationProgressCacheEntry(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                redisJobId,
                AiJobState.FAILED,
                100,
                "failed",
                event.failureReason(),
                Instant.now()))) {
            log.warn("generation_event type=redis_progress_unavailable workspaceId={} requestId={} jobId={} stage=failed",
                    job.getWorkspaceId(), job.getCreativeRequestId(), redisJobId);
        }
    }

    private GenerationFailedEventDto toEvent(Object payload) {
        try {
            GenerationFailedEventDto event = objectMapper.convertValue(payload, GenerationFailedEventDto.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generationJobId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
