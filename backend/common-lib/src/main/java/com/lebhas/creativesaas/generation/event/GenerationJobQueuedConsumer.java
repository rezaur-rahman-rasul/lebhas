package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressCacheEntry;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.GenerationFoundationService;
import com.lebhas.creativesaas.generation.application.GenerationJobService;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "platform.generation.kafka", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
public class GenerationJobQueuedConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenerationJobQueuedConsumer.class);

    private final ObjectMapper objectMapper;
    private final GenerationJobService generationJobService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GenerationFoundationService generationFoundationService;
    private final GenerationLockService generationLockService;
    private final AiJobStateRedisService jobStateRedisService;
    private final AiGenerationProgressRedisService progressRedisService;
    private final GenerationEventProducer eventProducer;

    public GenerationJobQueuedConsumer(
            ObjectMapper objectMapper,
            GenerationJobService generationJobService,
            CreativeRequestRepository creativeRequestRepository,
            GenerationFoundationService generationFoundationService,
            GenerationLockService generationLockService,
            AiJobStateRedisService jobStateRedisService,
            AiGenerationProgressRedisService progressRedisService,
            GenerationEventProducer eventProducer
    ) {
        this.objectMapper = objectMapper;
        this.generationJobService = generationJobService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generationFoundationService = generationFoundationService;
        this.generationLockService = generationLockService;
        this.jobStateRedisService = jobStateRedisService;
        this.progressRedisService = progressRedisService;
        this.eventProducer = eventProducer;
    }

    @KafkaListener(
            topics = "#{@creativeGenerationKafkaTopicNames.generationJobQueued()}",
            groupId = "${platform.generation.kafka.consumer-group:${spring.application.name}-generation}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload) {
        GenerationJobQueuedEventDto event = toEvent(payload);
        if (event == null) {
            return;
        }
        RedisLockService.RedisLockToken lock = generationLockService
                .acquire(event.workspaceId(), event.creativeRequestId())
                .orElse(null);
        if (lock == null) {
            eventProducer.publishGenerationFailed(new GenerationFailedEventDto(
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generationJobId(),
                    null,
                    event.creditReservationId(),
                    null,
                    "Generation is already running for this creative request",
                    true,
                    false,
                    Instant.now()));
            return;
        }
        try {
            startAndRunFoundation(event);
        } catch (RuntimeException exception) {
            eventProducer.publishGenerationFailed(new GenerationFailedEventDto(
                    event.workspaceId(),
                    event.creativeRequestId(),
                    event.generationJobId(),
                    null,
                    event.creditReservationId(),
                    null,
                    safeReason(exception),
                    true,
                    false,
                    Instant.now()));
        } finally {
            generationLockService.release(lock, event.workspaceId(), event.creativeRequestId());
        }
    }

    @Transactional
    protected void startAndRunFoundation(GenerationJobQueuedEventDto event) {
        GenerationJobEntity job = generationJobService.start(event.workspaceId(), event.generationJobId());
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(event.creativeRequestId(), event.workspaceId())
                .orElse(null);
        if (request != null) {
            request.markGenerationStarted(Instant.now());
            creativeRequestRepository.save(request);
        }
        storeRedisStarted(job);
        eventProducer.publishGenerationStarted(new GenerationStartedEventDto(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                job.getId(),
                event.creditReservationId(),
                job.getAttemptCount(),
                Instant.now()));
        generationFoundationService.runFoundation(job);
    }

    private void storeRedisStarted(GenerationJobEntity job) {
        if (!jobStateRedisService.store(new AiJobStateCacheEntry(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                job.getId(),
                null,
                job.getModel(),
                AiJobState.PROCESSING,
                job.getAttemptCount(),
                job.getProviderJobId(),
                "Generation foundation started",
                Instant.now()))) {
            log.warn("generation_event type=redis_job_state_unavailable workspaceId={} requestId={} jobId={} state=PROCESSING",
                    job.getWorkspaceId(), job.getCreativeRequestId(), job.getId());
        }
        if (!progressRedisService.store(new AiGenerationProgressCacheEntry(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                job.getId(),
                AiJobState.PROCESSING,
                10,
                "foundation",
                "Generation foundation started",
                Instant.now()))) {
            log.warn("generation_event type=redis_progress_unavailable workspaceId={} requestId={} jobId={} stage=foundation",
                    job.getWorkspaceId(), job.getCreativeRequestId(), job.getId());
        }
    }

    private GenerationJobQueuedEventDto toEvent(Object payload) {
        try {
            GenerationJobQueuedEventDto event = objectMapper.convertValue(payload, GenerationJobQueuedEventDto.class);
            if (event.workspaceId() == null || event.creativeRequestId() == null || event.generationJobId() == null) {
                return null;
            }
            return event;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String safeReason(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }
}
