package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestValidationService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobStatus;
import com.lebhas.creativesaas.generation.event.GenerationCompletedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.generation.event.GenerationFailedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationJobQueuedEventDto;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.storage.application.StorageFileService;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerationWorkerServiceTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRAND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID REQUEST_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID JOB_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID RESERVATION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID STORAGE_FILE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID VERSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private GenerationJobService generationJobService;
    private CreativeRequestRepository creativeRequestRepository;
    private GenerationExecutionContextFactory executionContextFactory;
    private CreativeLayerPipelineExecutor creativeLayerPipelineExecutor;
    private MockCreativeGenerationProvider mockCreativeGenerationProvider;
    private StorageFileService storageFileService;
    private AssetRepository assetRepository;
    private GeneratedVersionService generatedVersionService;
    private CreditReservationService creditReservationService;
    private CreativeRequestValidationService creativeRequestValidationService;
    private GenerationLockService generationLockService;
    private GenerationEventProducer generationEventProducer;
    private GenerationWorkerService workerService;

    @BeforeEach
    void setUp() {
        generationJobService = mock(GenerationJobService.class);
        creativeRequestRepository = mock(CreativeRequestRepository.class);
        executionContextFactory = mock(GenerationExecutionContextFactory.class);
        creativeLayerPipelineExecutor = mock(CreativeLayerPipelineExecutor.class);
        mockCreativeGenerationProvider = mock(MockCreativeGenerationProvider.class);
        storageFileService = mock(StorageFileService.class);
        assetRepository = mock(AssetRepository.class);
        generatedVersionService = mock(GeneratedVersionService.class);
        creditReservationService = mock(CreditReservationService.class);
        creativeRequestValidationService = mock(CreativeRequestValidationService.class);
        generationLockService = mock(GenerationLockService.class);
        generationEventProducer = mock(GenerationEventProducer.class);
        workerService = new GenerationWorkerService(
                generationJobService,
                creativeRequestRepository,
                executionContextFactory,
                creativeLayerPipelineExecutor,
                mockCreativeGenerationProvider,
                storageFileService,
                assetRepository,
                generatedVersionService,
                creditReservationService,
                creativeRequestValidationService,
                generationLockService,
                generationEventProducer);
    }

    @Test
    void duplicateProcessingIsPreventedByRedisLock() {
        when(generationLockService.acquire(WORKSPACE_ID, REQUEST_ID)).thenReturn(Optional.empty());

        workerService.processQueuedJob(queuedEvent());

        verify(generationJobService, never()).require(any(), any());
        verify(generationEventProducer).publishGenerationFailed(any(GenerationFailedEventDto.class));
    }

    @Test
    void completedDuplicateEventDoesNotCreateVersionsAgain() {
        GenerationJobEntity job = generationJob(GenerationJobStatus.COMPLETED);
        when(generationJobService.require(WORKSPACE_ID, JOB_ID)).thenReturn(job);

        workerService.run(queuedEvent());

        verify(generationJobService, never()).start(any(), any());
        verify(generatedVersionService, never()).createCompleted(any(), any(), any(Integer.class), any(), any(), any(), any(), any(), any());
    }

    @Test
    void queuedJobCompletesMockPipelineAndSettlesCredits() {
        CreativeRequestEntity request = creativeRequest();
        GenerationJobEntity queuedJob = generationJob(GenerationJobStatus.QUEUED);
        GenerationJobEntity startedJob = generationJob(GenerationJobStatus.PROCESSING);
        GenerationExecutionContext context = new GenerationExecutionContext(startedJob, request, mock());
        MockCreativeGenerationResult result = mockResult();
        StorageFileEntity file = storageFile();
        GeneratedVersionEntity version = generatedVersion();

        when(generationJobService.require(WORKSPACE_ID, JOB_ID)).thenReturn(queuedJob);
        when(generationJobService.start(WORKSPACE_ID, JOB_ID)).thenReturn(startedJob);
        when(creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(REQUEST_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(request));
        when(creativeRequestValidationService.estimateCost(request)).thenReturn(BigDecimal.valueOf(2));
        when(executionContextFactory.create(startedJob)).thenReturn(context);
        when(creativeLayerPipelineExecutor.execute(context)).thenReturn(Map.of("OUTPUT_PREPARATION", "ready"));
        when(generatedVersionService.nextVersionNumber(WORKSPACE_ID, REQUEST_ID)).thenReturn(1);
        when(mockCreativeGenerationProvider.generate(eq(context), any(), eq(1))).thenReturn(result);
        when(storageFileService.registerGeneratedOutput(any(), any(), any(), any(), any(), any(), any(), any(), any(Long.class), any(), any(), any(), any()))
                .thenReturn(file);
        when(assetRepository.save(any(AssetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(generatedVersionService.createCompleted(eq(request), eq(USER_ID), eq(1), eq(STORAGE_FILE_ID), any(), eq(null), eq(null), eq("MOCK_CREATIVE_PROVIDER"), eq("mock-creative-v1")))
                .thenReturn(version);
        when(generatedVersionService.save(version)).thenReturn(version);
        when(generatedVersionService.listByCreativeRequest(WORKSPACE_ID, REQUEST_ID)).thenReturn(List.of(version));

        workerService.run(queuedEvent());

        verify(creativeLayerPipelineExecutor).execute(context);
        verify(storageFileService).registerGeneratedOutput(eq(WORKSPACE_ID), eq(PROJECT_ID), eq(StorageProvider.R2), eq("mock-generated"), eq(result.objectKey()), eq(null), eq(result.mimeType()), eq(result.fileExtension()), eq((long) result.content().length), eq(result.width()), eq(result.height()), eq(result.duration()), eq(result.content()));
        verify(generatedVersionService).createCompleted(eq(request), eq(USER_ID), eq(1), eq(STORAGE_FILE_ID), any(), eq(null), eq(null), eq("MOCK_CREATIVE_PROVIDER"), eq("mock-creative-v1"));
        verify(creditReservationService).finalize(eq(WORKSPACE_ID), eq(REQUEST_ID), eq(VERSION_ID), eq(RESERVATION_ID), eq("creative_request_generation"), eq(REQUEST_ID), eq(BigDecimal.valueOf(2)), eq("mock_generation_completed"));
        verify(generationJobService).complete(eq(WORKSPACE_ID), eq(JOB_ID), eq("mock-completed-" + JOB_ID));
        verify(generationEventProducer).publishGenerationCompleted(any(GenerationCompletedEventDto.class));
        assertThat(request.getStatus()).isEqualTo(CreativeRequestStatus.COMPLETED);
    }

    @Test
    void layerFailureFailsJobAndRefundsCredits() {
        CreativeRequestEntity request = creativeRequest();
        GenerationJobEntity queuedJob = generationJob(GenerationJobStatus.QUEUED);
        GenerationJobEntity startedJob = generationJob(GenerationJobStatus.PROCESSING);
        GenerationExecutionContext context = new GenerationExecutionContext(startedJob, request, mock());
        RuntimeException failure = new BusinessException(com.lebhas.creativesaas.common.exception.ErrorCode.GENERATION_PROVIDER_REQUEST_FAILED, "layer failed");

        when(generationJobService.require(WORKSPACE_ID, JOB_ID)).thenReturn(queuedJob);
        when(generationJobService.start(WORKSPACE_ID, JOB_ID)).thenReturn(startedJob);
        when(creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(REQUEST_ID, WORKSPACE_ID))
                .thenReturn(Optional.of(request));
        when(creativeRequestValidationService.estimateCost(request)).thenReturn(BigDecimal.valueOf(2));
        when(executionContextFactory.create(startedJob)).thenReturn(context);
        when(creativeLayerPipelineExecutor.execute(context)).thenThrow(failure);

        assertThatThrownBy(() -> workerService.run(queuedEvent())).isSameAs(failure);

        verify(generationJobService).fail(eq(WORKSPACE_ID), eq(JOB_ID), any());
        verify(creditReservationService).refund(eq(WORKSPACE_ID), eq(REQUEST_ID), eq(null), eq(RESERVATION_ID), eq("creative_request_generation"), eq(REQUEST_ID), eq(BigDecimal.valueOf(2)), eq("mock_generation_failed"));
        verify(generationEventProducer).publishGenerationFailed(any(GenerationFailedEventDto.class));
        assertThat(request.getStatus()).isEqualTo(CreativeRequestStatus.FAILED);
    }

    @Test
    void mockProviderReturnsDeterministicMetadataWithoutAiCall() {
        CreativeRequestEntity request = creativeRequest();
        GenerationJobEntity job = generationJob(GenerationJobStatus.PROCESSING);
        GenerationExecutionContext context = new GenerationExecutionContext(job, request, mock());
        MockCreativeGenerationProvider provider = new MockCreativeGenerationProvider();

        MockCreativeGenerationResult first = provider.generate(context, Map.of("layer", "output"), 1);
        MockCreativeGenerationResult second = provider.generate(context, Map.of("layer", "output"), 1);

        assertThat(first.providerName()).isEqualTo("MOCK_CREATIVE_PROVIDER");
        assertThat(first.model()).isEqualTo("mock-creative-v1");
        assertThat(first.objectKey()).contains(JOB_ID.toString());
        assertThat(first.objectKey()).isEqualTo(second.objectKey());
        assertThat(first.content()).isEqualTo(second.content());
        assertThat(first.metadata()).containsEntry("mock", true);
    }

    private GenerationJobQueuedEventDto queuedEvent() {
        return new GenerationJobQueuedEventDto(
                WORKSPACE_ID,
                REQUEST_ID,
                JOB_ID,
                RESERVATION_ID,
                "creative-generation",
                Instant.parse("2026-05-31T00:00:00Z"));
    }

    private CreativeRequestEntity creativeRequest() {
        CreativeRequestEntity request = CreativeRequestEntity.create(
                WORKSPACE_ID,
                BRAND_ID,
                PRODUCT_ID,
                PROJECT_ID,
                USER_ID,
                "Creative request",
                "Create a premium launch ad",
                "Create a premium launch ad with sharp product context",
                BrandLanguagePreference.ENGLISH,
                PromptPlatform.FACEBOOK,
                null,
                CampaignObjective.AWARENESS,
                null,
                null,
                null,
                CreativeRequestStatus.QUEUED,
                1);
        ReflectionTestUtils.setField(request, "id", REQUEST_ID);
        request.attachCreditReservation(RESERVATION_ID);
        return request;
    }

    private GenerationJobEntity generationJob(GenerationJobStatus status) {
        GenerationJobEntity job = GenerationJobEntity.queue(WORKSPACE_ID, REQUEST_ID, null, "creative-generation", 3);
        ReflectionTestUtils.setField(job, "id", JOB_ID);
        if (status == GenerationJobStatus.PROCESSING) {
            job.markStarted();
        } else if (status == GenerationJobStatus.COMPLETED) {
            job.markCompleted("provider-job");
        }
        return job;
    }

    private MockCreativeGenerationResult mockResult() {
        return new MockCreativeGenerationResult(
                "MOCK_CREATIVE_PROVIDER",
                "mock-creative-v1",
                "mock-job",
                "generated/mock/output.png",
                "image/png",
                "png",
                1080,
                1080,
                null,
                "mock-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Map.of("mock", true));
    }

    private StorageFileEntity storageFile() {
        StorageFileEntity file = StorageFileEntity.create(
                WORKSPACE_ID,
                PROJECT_ID,
                StorageProvider.R2,
                "mock-generated",
                "generated/mock/output.png",
                null,
                "image/png",
                "png",
                10,
                "hash",
                1080,
                1080,
                null,
                StorageClass.STANDARD,
                StorageFilePurpose.GENERATED);
        ReflectionTestUtils.setField(file, "id", STORAGE_FILE_ID);
        return file;
    }

    private GeneratedVersionEntity generatedVersion() {
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                WORKSPACE_ID,
                REQUEST_ID,
                PROJECT_ID,
                1,
                "Version 1",
                STORAGE_FILE_ID,
                UUID.randomUUID(),
                GenerationStatus.READY,
                ApprovalStatus.NOT_SUBMITTED,
                true,
                "MOCK_CREATIVE_PROVIDER",
                "mock-creative-v1",
                USER_ID,
                GeneratedVersionStatus.ACTIVE);
        ReflectionTestUtils.setField(version, "id", VERSION_ID);
        return version;
    }
}
