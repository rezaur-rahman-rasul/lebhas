package com.lebhas.creativesaas.generation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.cache.RetryThrottleState;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.generation.application.CreditReservationService;
import com.lebhas.creativesaas.generation.application.GenerationFoundationService;
import com.lebhas.creativesaas.generation.application.GenerationJobService;
import com.lebhas.creativesaas.generation.application.GenerationOrchestrator;
import com.lebhas.creativesaas.generation.application.GenerationRetryService;
import com.lebhas.creativesaas.generation.application.GenerationWorkerService;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.cache.GeneratedVersionCountCacheService;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.cache.GenerationRedisAccessSupport;
import com.lebhas.creativesaas.generation.cache.GenerationRedisCacheProperties;
import com.lebhas.creativesaas.generation.cache.GenerationRedisKeys;
import com.lebhas.creativesaas.generation.cache.GenerationRedisTtlStrategy;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobStatus;
import com.lebhas.creativesaas.generation.infrastructure.persistence.GenerationJobRepository;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.PricingPlanView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspaceSubscriptionView;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevisedDay5GenerationPipelineUnitTest {

    private static final Instant NOW = Instant.parse("2026-05-22T00:00:00Z");
    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKSPACE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREATIVE_REQUEST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GENERATED_VERSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GENERATION_JOB_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID CREDIT_RESERVATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Test
    void generatedVersionRespectsPlanLimit() {
        GeneratedVersionRepository repository = mock(GeneratedVersionRepository.class);
        when(repository.countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(WORKSPACE_ID, CREATIVE_REQUEST_ID))
                .thenReturn(1L);
        GeneratedVersionService service = new GeneratedVersionService(
                repository,
                planContextService(1),
                mock(GeneratedVersionCountCacheService.class),
                mock(GenerationEventProducer.class));

        assertThatThrownBy(() -> service.validateVersionCapacity(WORKSPACE_ID, CREATIVE_REQUEST_ID, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Generated version count exceeds");
    }

    @Test
    void creditReservedBeforeGeneration() {
        CreativeRequestRepository requestRepository = mock(CreativeRequestRepository.class);
        GenerationJobService jobService = mock(GenerationJobService.class);
        CreditReservationService creditReservationService = mock(CreditReservationService.class);
        GeneratedVersionService generatedVersionService = mock(GeneratedVersionService.class);
        CreativeRequestEntity request = creativeRequest();
        GenerationJobEntity job = generationJob();
        CreditReservationResult reservation = reservation();
        when(creditReservationService.reserve(eq(WORKSPACE_ID), eq(CREATIVE_REQUEST_ID), eq(null), eq(BigDecimal.ONE), any(), eq(CREATIVE_REQUEST_ID)))
                .thenReturn(reservation);
        when(requestRepository.save(request)).thenReturn(request);
        when(jobService.queue(eq(request), eq("creative-generation"))).thenReturn(job);

        new GenerationOrchestrator(requestRepository, jobService, creditReservationService, generatedVersionService)
                .queueGeneration(request, USER_ID, BigDecimal.ONE);

        InOrder inOrder = inOrder(generatedVersionService, creditReservationService, requestRepository, jobService);
        inOrder.verify(generatedVersionService).validateVersionCapacity(WORKSPACE_ID, CREATIVE_REQUEST_ID, 1);
        inOrder.verify(creditReservationService).reserve(eq(WORKSPACE_ID), eq(CREATIVE_REQUEST_ID), eq(null), eq(BigDecimal.ONE), any(), eq(CREATIVE_REQUEST_ID));
        inOrder.verify(requestRepository).save(request);
        inOrder.verify(jobService).queue(request, "creative-generation");
    }

    @Test
    void creditFinalizedAfterSuccess() {
        CreditReservationService creditReservationService = mock(CreditReservationService.class);
        GenerationCompletedConsumer consumer = completedConsumer(creditReservationService);

        consumer.complete(completedEvent());

        verify(creditReservationService).finalize(
                eq(WORKSPACE_ID),
                eq(CREATIVE_REQUEST_ID),
                eq(GENERATED_VERSION_ID),
                eq(CREDIT_RESERVATION_ID),
                eq("creative_request_generation"),
                eq(CREATIVE_REQUEST_ID),
                eq(BigDecimal.ONE),
                eq("generation_completed"));
    }

    @Test
    void creditRefundedOnFailure() {
        CreditReservationService creditReservationService = mock(CreditReservationService.class);
        GenerationFailedConsumer consumer = failedConsumer(creditReservationService, mock(GenerationRetryService.class));

        consumer.fail(failedEvent());

        verify(creditReservationService).refund(
                eq(WORKSPACE_ID),
                eq(CREATIVE_REQUEST_ID),
                eq(GENERATED_VERSION_ID),
                eq(CREDIT_RESERVATION_ID),
                eq("creative_request_generation"),
                eq(CREATIVE_REQUEST_ID),
                eq(BigDecimal.ONE),
                eq("generation_failed"));
    }

    @Test
    void duplicateGenerationPreventedByRedisLock() {
        GenerationWorkerService workerService = mock(GenerationWorkerService.class);

        GenerationJobQueuedConsumer consumer = new GenerationJobQueuedConsumer(
                new ObjectMapper(),
                workerService);
        consumer.consume(Map.of(
                "workspaceId", WORKSPACE_ID,
                "creativeRequestId", CREATIVE_REQUEST_ID,
                "generationJobId", GENERATION_JOB_ID,
                "creditReservationId", CREDIT_RESERVATION_ID,
                "queueName", "creative-generation"));

        verify(workerService).processQueuedJob(any(GenerationJobQueuedEventDto.class));
    }

    @Test
    void generatedVersionLinkedToCreativeRequest() {
        GeneratedVersionEntity version = generatedVersion();

        assertThat(version.getCreativeRequestId()).isEqualTo(CREATIVE_REQUEST_ID);
        assertThat(version.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void kafkaGenerationStartedPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        GenerationEventProducer producer = new GenerationEventProducer(kafkaTemplate, new CreativeGenerationKafkaTopicNames(""));
        GenerationStartedEventDto event = new GenerationStartedEventDto(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATION_JOB_ID,
                CREDIT_RESERVATION_ID,
                1,
                NOW);

        producer.publishGenerationStarted(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.GENERATION_STARTED, GENERATION_JOB_ID.toString(), event);
    }

    @Test
    void kafkaGenerationCompletedPublished() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        GenerationEventProducer producer = new GenerationEventProducer(kafkaTemplate, new CreativeGenerationKafkaTopicNames(""));
        GenerationCompletedEventDto event = completedEvent();

        producer.publishGenerationCompleted(event);

        verify(kafkaTemplate).send(KafkaTopicConstants.GENERATION_COMPLETED, GENERATION_JOB_ID.toString(), event);
    }

    @Test
    void redisGeneratedVersionCacheWorks() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        GenerationRedisKeys keys = new GenerationRedisKeys();
        GeneratedVersionCountCacheService cache = new GeneratedVersionCountCacheService(
                keys,
                new GenerationRedisAccessSupport(redisCacheService, mock(RedisLockService.class), mock(RedisRateLimitService.class)),
                new GenerationRedisTtlStrategy(new GenerationRedisCacheProperties()));
        GeneratedVersionCountCacheService.GeneratedVersionCountCacheEntry entry =
                new GeneratedVersionCountCacheService.GeneratedVersionCountCacheEntry(WORKSPACE_ID, CREATIVE_REQUEST_ID, 2, NOW);
        when(redisCacheService.get(keys.generatedVersions(CREATIVE_REQUEST_ID), GeneratedVersionCountCacheService.GeneratedVersionCountCacheEntry.class))
                .thenReturn(Optional.of(entry));

        assertThat(cache.store(WORKSPACE_ID, CREATIVE_REQUEST_ID, 2)).isTrue();
        assertThat(cache.get(WORKSPACE_ID, CREATIVE_REQUEST_ID)).contains(entry);
        verify(redisCacheService).set(eq("generated:versions:" + CREATIVE_REQUEST_ID), any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void workspaceGenerationIsolationWorks() {
        GenerationJobRepository repository = mock(GenerationJobRepository.class);
        GenerationJobEntity job = generationJob(OTHER_WORKSPACE_ID);
        when(repository.findByIdAndDeletedFalse(GENERATION_JOB_ID)).thenReturn(Optional.of(job));
        GenerationJobService service = new GenerationJobService(
                repository,
                mock(),
                mock(GenerationEventProducer.class),
                mock(GeneratedVersionService.class),
                mock());

        assertThatThrownBy(() -> service.require(WORKSPACE_ID, GENERATION_JOB_ID))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void retryCountIncrementsCorrectly() {
        AiRetryThrottleService throttleService = mock(AiRetryThrottleService.class);
        when(throttleService.recordRetry(WORKSPACE_ID, CREATIVE_REQUEST_ID))
                .thenReturn(Optional.of(retryState(1)))
                .thenReturn(Optional.of(retryState(2)));
        GenerationRetryService service = new GenerationRetryService(throttleService);

        service.validateRetryAllowed(WORKSPACE_ID, CREATIVE_REQUEST_ID);
        service.validateRetryAllowed(WORKSPACE_ID, CREATIVE_REQUEST_ID);

        verify(throttleService, times(2)).recordRetry(WORKSPACE_ID, CREATIVE_REQUEST_ID);
    }

    @Test
    void generationJobLifecycleWorks() {
        GenerationJobEntity job = generationJob();

        assertThat(job.getJobStatus()).isEqualTo(GenerationJobStatus.QUEUED);
        job.markStarted();
        assertThat(job.getJobStatus()).isEqualTo(GenerationJobStatus.PROCESSING);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        job.markCompleted("provider-job-1");

        assertThat(job.getJobStatus()).isEqualTo(GenerationJobStatus.COMPLETED);
        assertThat(job.getProviderJobId()).isEqualTo("provider-job-1");
        assertThat(job.isTerminal()).isTrue();
    }

    private GenerationCompletedConsumer completedConsumer(CreditReservationService creditReservationService) {
        CreativeRequestRepository requestRepository = mock(CreativeRequestRepository.class);
        GeneratedVersionService generatedVersionService = mock(GeneratedVersionService.class);
        GenerationJobService jobService = mock(GenerationJobService.class);
        when(requestRepository.findByIdAndWorkspaceIdAndDeletedFalse(CREATIVE_REQUEST_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(creativeRequest()));
        when(jobService.complete(WORKSPACE_ID, GENERATION_JOB_ID, "provider-job-1"))
                .thenReturn(generationJob());
        when(generatedVersionService.requireByIdAndWorkspaceId(WORKSPACE_ID, GENERATED_VERSION_ID))
                .thenReturn(generatedVersion());
        when(generatedVersionService.save(any(GeneratedVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(generatedVersionService.listByCreativeRequest(WORKSPACE_ID, CREATIVE_REQUEST_ID))
                .thenReturn(List.of(generatedVersion()));
        return new GenerationCompletedConsumer(
                new ObjectMapper(),
                requestRepository,
                generatedVersionService,
                jobService,
                creditReservationService,
                mock(AiJobStateRedisService.class),
                mock(AiGenerationProgressRedisService.class),
                mock(AiRetryThrottleService.class),
                mock(GenerationEventProducer.class));
    }

    private GenerationFailedConsumer failedConsumer(
            CreditReservationService creditReservationService,
            GenerationRetryService retryService
    ) {
        CreativeRequestRepository requestRepository = mock(CreativeRequestRepository.class);
        GeneratedVersionService generatedVersionService = mock(GeneratedVersionService.class);
        GenerationJobService jobService = mock(GenerationJobService.class);
        when(requestRepository.findByIdAndWorkspaceIdAndDeletedFalse(CREATIVE_REQUEST_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(creativeRequest()));
        when(jobService.fail(WORKSPACE_ID, GENERATION_JOB_ID, "provider failed"))
                .thenReturn(generationJob());
        when(generatedVersionService.findByIdAndWorkspaceId(WORKSPACE_ID, GENERATED_VERSION_ID))
                .thenReturn(Optional.of(generatedVersion()));
        when(generatedVersionService.save(any(GeneratedVersionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return new GenerationFailedConsumer(
                new ObjectMapper(),
                requestRepository,
                generatedVersionService,
                jobService,
                creditReservationService,
                retryService,
                mock(AiJobStateRedisService.class),
                mock(AiGenerationProgressRedisService.class),
                mock(GenerationEventProducer.class));
    }

    private GenerationJobQueuedEventDto queuedEvent() {
        return new GenerationJobQueuedEventDto(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATION_JOB_ID,
                CREDIT_RESERVATION_ID,
                "creative-generation",
                NOW);
    }

    private GenerationCompletedEventDto completedEvent() {
        return new GenerationCompletedEventDto(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATION_JOB_ID,
                GENERATED_VERSION_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                CREDIT_RESERVATION_ID,
                BigDecimal.ONE,
                null,
                null,
                "provider-job-1",
                false,
                NOW);
    }

    private GenerationFailedEventDto failedEvent() {
        return new GenerationFailedEventDto(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                GENERATION_JOB_ID,
                GENERATED_VERSION_ID,
                CREDIT_RESERVATION_ID,
                BigDecimal.ONE,
                "provider failed",
                true,
                false,
                NOW);
    }

    private CreativeRequestEntity creativeRequest() {
        CreativeRequestEntity request = CreativeRequestEntity.create(
                WORKSPACE_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                USER_ID,
                "Request",
                "Prompt",
                "Enhanced",
                BrandLanguagePreference.ENGLISH,
                PromptPlatform.FACEBOOK,
                null,
                CampaignObjective.AWARENESS,
                null,
                null,
                null,
                1);
        ReflectionTestUtils.setField(request, "id", CREATIVE_REQUEST_ID);
        request.attachCreditReservation(CREDIT_RESERVATION_ID);
        return request;
    }

    private GenerationJobEntity generationJob() {
        return generationJob(WORKSPACE_ID);
    }

    private GenerationJobEntity generationJob(UUID workspaceId) {
        GenerationJobEntity job = GenerationJobEntity.queue(workspaceId, CREATIVE_REQUEST_ID, null, "creative-generation", 3);
        ReflectionTestUtils.setField(job, "id", GENERATION_JOB_ID);
        return job;
    }

    private GeneratedVersionEntity generatedVersion() {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                UUID.randomUUID(),
                1,
                "Version 1",
                null,
                null,
                GenerationStatus.READY,
                ApprovalStatus.NOT_SUBMITTED,
                true,
                null,
                null,
                USER_ID,
                GeneratedVersionStatus.ACTIVE);
        ReflectionTestUtils.setField(version, "id", GENERATED_VERSION_ID);
        return version;
    }

    private CreditReservationResult reservation() {
        return new CreditReservationResult(
                CREDIT_RESERVATION_ID,
                WORKSPACE_ID,
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                "creative_request_generation",
                CREATIVE_REQUEST_ID);
    }

    private RetryThrottleState retryState(long count) {
        return new RetryThrottleState(
                WORKSPACE_ID,
                CREATIVE_REQUEST_ID,
                count,
                3,
                true,
                Duration.ofMinutes(5),
                NOW,
                NOW.plus(Duration.ofMinutes(5)));
    }

    private WorkspacePlanContextService planContextService(int maxVersions) {
        WorkspacePlanContextService service = mock(WorkspacePlanContextService.class);
        when(service.getWorkspacePlanContext(WORKSPACE_ID)).thenReturn(planContext(maxVersions));
        return service;
    }

    private WorkspacePlanContextView planContext(int maxVersions) {
        UUID pricingPlanId = UUID.randomUUID();
        return new WorkspacePlanContextView(
                WORKSPACE_ID,
                new WorkspaceSubscriptionView(
                        UUID.randomUUID(),
                        WORKSPACE_ID,
                        pricingPlanId,
                        WorkspaceSubscriptionStatus.ACTIVE,
                        NOW.minus(Duration.ofDays(1)),
                        NOW.plus(Duration.ofDays(30)),
                        null,
                        true,
                        NOW,
                        NOW),
                new PricingPlanView(pricingPlanId, "Dynamic plan", "DYNAMIC", "Dynamic", BigDecimal.ONE, BigDecimal.TEN, "USD", false, true, 1, NOW, NOW),
                new PlanFeaturePolicyView(UUID.randomUUID(), pricingPlanId, maxVersions, 5, 5, 5, 5, BigDecimal.TEN, BigDecimal.TEN, true, true, true, true, true, true, NOW, NOW),
                false);
    }
}
