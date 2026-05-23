package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.cache.AiDuplicateGenerationLockService;
import com.lebhas.ai.cache.AiGenerationProgressCacheEntry;
import com.lebhas.ai.cache.AiGenerationProgressRedisService;
import com.lebhas.ai.cache.AiPipelineExecutionStateCacheService;
import com.lebhas.ai.cache.AiRetryThrottleService;
import com.lebhas.ai.cache.PipelineExecutionStateCacheEntry;
import com.lebhas.ai.cache.RetryThrottleState;
import com.lebhas.ai.event.AiCostEvent;
import com.lebhas.ai.event.AiGenerationLifecycleEvent;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.job.AiJobState;
import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.ai.producer.AiCreativePipelineEventProducer;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.CreativeCreditReservationService;
import com.lebhas.creativesaas.generation.application.CreditReservationService;
import com.lebhas.creativesaas.generation.application.GenerationJobService;
import com.lebhas.creativesaas.generation.application.GenerationOrchestrator;
import com.lebhas.creativesaas.generation.application.GenerationRetryService;
import com.lebhas.creativesaas.generation.application.dto.CreditRefundCommand;
import com.lebhas.creativesaas.generation.application.dto.CreditReservationResult;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.event.CreativeGenerationEventProducer;
import com.lebhas.creativesaas.generation.event.CreativeGenerationRequestedEvent;
import com.lebhas.creativesaas.generation.event.CreativeRequestCreatedEvent;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativeGenerationOrchestrator {

    private static final String CREDIT_REFERENCE_TYPE = "creative_request_generation";

    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final CreativeCreditReservationService creativeCreditReservationService;
    private final CreditReservationService creditReservationService;
    private final GenerationJobService generationJobService;
    private final GenerationOrchestrator generationOrchestrator;
    private final GenerationRetryService generationRetryService;
    private final GenerationLockService generationLockService;
    private final AiDuplicateGenerationLockService aiDuplicateGenerationLockService;
    private final AiJobStateRedisService aiJobStateRedisService;
    private final AiGenerationProgressRedisService aiGenerationProgressRedisService;
    private final AiPipelineExecutionStateCacheService aiPipelineExecutionStateCacheService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final CreativeRequestMapper creativeRequestMapper;
    private final CreativeRequestPromptFlowService creativeRequestPromptFlowService;
    private final PipelineResolver pipelineResolver;
    private final LayerRoutingResolver layerRoutingResolver;
    private final LayerExecutionStateService layerExecutionStateService;
    private final LayerProviderExecutionGateway layerProviderExecutionGateway;
    private final CreativeGenerationEventProducer creativeGenerationEventProducer;
    private final AiCreativePipelineEventProducer aiCreativePipelineEventProducer;
    private final DomainEventPublisher domainEventPublisher;

    public CreativeGenerationOrchestrator(
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            CreativeCreditReservationService creativeCreditReservationService,
            CreditReservationService creditReservationService,
            GenerationJobService generationJobService,
            GenerationOrchestrator generationOrchestrator,
            GenerationRetryService generationRetryService,
            GenerationLockService generationLockService,
            AiDuplicateGenerationLockService aiDuplicateGenerationLockService,
            AiRetryThrottleService aiRetryThrottleService,
            AiJobStateRedisService aiJobStateRedisService,
            AiGenerationProgressRedisService aiGenerationProgressRedisService,
            AiPipelineExecutionStateCacheService aiPipelineExecutionStateCacheService,
            CreativeRequestValidationService creativeRequestValidationService,
            CreativeRequestMapper creativeRequestMapper,
            CreativeRequestPromptFlowService creativeRequestPromptFlowService,
            PipelineResolver pipelineResolver,
            LayerRoutingResolver layerRoutingResolver,
            LayerExecutionStateService layerExecutionStateService,
            LayerProviderExecutionGateway layerProviderExecutionGateway,
            CreativeGenerationEventProducer creativeGenerationEventProducer,
            AiCreativePipelineEventProducer aiCreativePipelineEventProducer,
            DomainEventPublisher domainEventPublisher
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.creativeCreditReservationService = creativeCreditReservationService;
        this.creditReservationService = creditReservationService;
        this.generationJobService = generationJobService;
        this.generationOrchestrator = generationOrchestrator;
        this.generationRetryService = generationRetryService;
        this.generationLockService = generationLockService;
        this.aiDuplicateGenerationLockService = aiDuplicateGenerationLockService;
        this.aiJobStateRedisService = aiJobStateRedisService;
        this.aiGenerationProgressRedisService = aiGenerationProgressRedisService;
        this.aiPipelineExecutionStateCacheService = aiPipelineExecutionStateCacheService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.creativeRequestMapper = creativeRequestMapper;
        this.creativeRequestPromptFlowService = creativeRequestPromptFlowService;
        this.pipelineResolver = pipelineResolver;
        this.layerRoutingResolver = layerRoutingResolver;
        this.layerExecutionStateService = layerExecutionStateService;
        this.layerProviderExecutionGateway = layerProviderExecutionGateway;
        this.creativeGenerationEventProducer = creativeGenerationEventProducer;
        this.aiCreativePipelineEventProducer = aiCreativePipelineEventProducer;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreativeRequestResponse createQueuedRequest(
            UUID workspaceId,
            UUID actorUserId,
            CreativeRequestGenerationPlan plan
    ) {
        RedisLockService.RedisLockToken duplicateLock = aiDuplicateGenerationLockService
                .acquire(workspaceId, null, plan.duplicateRequestHash())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GENERATION_STATE_CONFLICT,
                        "A matching creative request is already queued"));
        try {
            CreativeRequestEntity request = CreativeRequestEntity.create(
                    workspaceId,
                    plan.brandId(),
                    plan.productServiceId(),
                    plan.projectCampaignId(),
                    actorUserId,
                    plan.requestName(),
                    plan.sourcePrompt(),
                    plan.enhancedPrompt(),
                    plan.languagePreference(),
                    plan.platform(),
                    plan.creativeType(),
                    plan.creativeObjective(),
                    plan.campaignTone(),
                    plan.targetAudience(),
                    plan.ctaPreference(),
                    CreativeRequestStatus.DRAFT,
                    plan.requestedVersions());
            request.replaceSelectedAssetIds(plan.selectedAssetIds());
            request = creativeRequestRepository.saveAndFlush(request);

            creativeRequestPromptFlowService.recordPromptEnhanced(
                    workspaceId,
                    request.getId(),
                    request.getProjectCampaignId(),
                    actorUserId,
                    request.getSourcePrompt(),
                    request.getEnhancedPrompt(),
                    plan.promptLanguage(),
                    plan.platform(),
                    plan.creativeObjective(),
                    plan.toAiGenerationRequest(workspaceId, request.getId(), null).brandContextSnapshot());
            creativeGenerationEventProducer.publishCreativeRequestCreated(new CreativeRequestCreatedEvent(
                    null,
                    null,
                    workspaceId,
                    request.getId(),
                    request.getProjectCampaignId(),
                    actorUserId,
                    null,
                    request.getStatus().name()));
            publishCreativeRequestCreatedSafely(request, actorUserId, "creative_request_created");
            generationOrchestrator.queueGeneration(request, actorUserId, plan.estimatedCreditCost());
            creativeRequestRepository.save(request);

            return creativeRequestMapper.toResponse(request, List.of(), null, plan.estimatedCreditCost());
        } finally {
            aiDuplicateGenerationLockService.release(workspaceId, null, duplicateLock);
        }
    }

    @Transactional
    public CreativeRequestResponse orchestrateFoundationGeneration(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID actorUserId
    ) {
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        RedisLockService.RedisLockToken generationLock = generationLockService
                .acquire(workspaceId, request.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GENERATION_STATE_CONFLICT,
                        "Generation is already running for this creative request"));
        try {
            PipelineResolutionContext context = pipelineResolver.resolve(workspaceId);
            generatedVersionService.validateVersionCapacity(workspaceId, request.getId(), 1);
            BigDecimal estimatedCost = creativeRequestValidationEstimatedCost(request);
            CreditReservationResult reservation = creditReservationService.reserve(
                    workspaceId,
                    request.getId(),
                    null,
                    estimatedCost,
                    CREDIT_REFERENCE_TYPE,
                    request.getId());
            request.attachCreditReservation(reservation.reservationId());
            request.markGenerationStarted(Instant.now());
            creativeRequestRepository.save(request);

            GeneratedVersionEntity version = generatedVersionService.createQueuedPlaceholder(
                    request,
                    actorUserId,
                    generatedVersionService.nextVersionNumber(workspaceId, request.getId()),
                    null,
                    null);
            GenerationJobEntity job = generationJobService.queue(request, "dynamic-ai-pipeline");
            generationJobService.start(workspaceId, job.getId());
            version.markProcessing();
            generatedVersionService.save(version);

            storePipelineState(workspaceId, request.getId(), context, "STARTED", null);
            aiCreativePipelineEventProducer.publishGenerationRequested(generationEvent(request, version.getId(), job.getId(), context, "REQUESTED", null, Map.of()));
            aiCreativePipelineEventProducer.publishGenerationStarted(generationEvent(request, version.getId(), job.getId(), context, "STARTED", null, Map.of()));

            BigDecimal totalEstimatedCost = BigDecimal.ZERO;
            BigDecimal totalQualityScore = BigDecimal.ZERO;
            int executedLayers = 0;
            for (com.lebhas.ai.domain.CreativePipelineLayer layer : context.layers()) {
                LayerRoutingDecision decision = layerRoutingResolver.resolve(context, layer, request);
                totalEstimatedCost = totalEstimatedCost.add(decision.estimatedCost() == null ? BigDecimal.ZERO : decision.estimatedCost());
                totalQualityScore = totalQualityScore.add(decision.qualityScore() == null ? BigDecimal.ZERO : decision.qualityScore());
                layerExecutionStateService.markStarted(request, job.getId(), decision, 1);
                LayerExecutionResult result = layerProviderExecutionGateway.executeFoundationLayer(request, context, decision);
                if (!result.success()) {
                    layerExecutionStateService.markFailed(request, job.getId(), decision, 1, result.message(), result.metadata());
                    storePipelineState(workspaceId, request.getId(), context, "FAILED", layer.getLayerType().name());
                    aiCreativePipelineEventProducer.publishGenerationFailed(generationEvent(request, version.getId(), job.getId(), context, "FAILED", result.message(), result.metadata()));
                    generationJobService.fail(workspaceId, job.getId(), result.message());
                    version.markFailed(result.message());
                    generatedVersionService.save(version);
                    creditReservationService.refund(
                            workspaceId,
                            request.getId(),
                            version.getId(),
                            reservation.reservationId(),
                            CREDIT_REFERENCE_TYPE,
                            request.getId(),
                            reservation.reservedAmount(),
                            "foundation_layer_failed");
                    request.markGenerationFailed(result.message(), Instant.now());
                    creativeRequestRepository.save(request);
                    return creativeRequestMapper.toResponse(request, List.of(version), null, estimatedCost);
                }
                layerExecutionStateService.markCompleted(request, job.getId(), decision, 1, result);
                executedLayers++;
            }

            Map<String, Object> costBreakdown = new LinkedHashMap<>();
            costBreakdown.put("foundationOnly", true);
            costBreakdown.put("executedLayers", executedLayers);
            costBreakdown.put("estimatedLayerCost", totalEstimatedCost);
            costBreakdown.put("qualityScoreTotal", totalQualityScore);
            aiCreativePipelineEventProducer.publishCostEstimated(new AiCostEvent(
                    null,
                    null,
                    workspaceId,
                    request.getId(),
                    version.getId(),
                    job.getId(),
                    context.pipeline().getId(),
                    totalEstimatedCost,
                    null,
                    costBreakdown,
                    Map.of("foundationOnly", true)));
            version.markReady(null, null, null, null);
            generatedVersionService.save(version);
            generationJobService.complete(workspaceId, job.getId(), null);
            creditReservationService.finalize(
                    workspaceId,
                    request.getId(),
                    version.getId(),
                    reservation.reservationId(),
                    CREDIT_REFERENCE_TYPE,
                    request.getId(),
                    reservation.reservedAmount(),
                    "foundation_generation_completed");
            request.markGenerationCompleted(Instant.now(), (int) generatedVersionService.listByCreativeRequest(workspaceId, request.getId()).stream()
                    .filter(generatedVersion -> generatedVersion.getGenerationStatus() == GenerationStatus.READY)
                    .count());
            creativeRequestRepository.save(request);
            storePipelineState(workspaceId, request.getId(), context, "FOUNDATION_COMPLETED", null);
            aiCreativePipelineEventProducer.publishGenerationCompleted(generationEvent(request, version.getId(), job.getId(), context, "FOUNDATION_COMPLETED", null, costBreakdown));

            List<GeneratedVersionEntity> versions = generatedVersionService.listByCreativeRequest(workspaceId, request.getId());
            return creativeRequestMapper.toResponse(request, versions, null, estimatedCost);
        } finally {
            generationLockService.release(generationLock, workspaceId, request.getId());
        }
    }

    @Transactional
    public CreativeRequestResponse cancelQueuedRequest(
            CreativeRequestEntity request,
            GeneratedVersionEntity latestVersion,
            java.math.BigDecimal estimatedCreditCost
    ) {
        if (request.getStatus() != CreativeRequestStatus.QUEUED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only queued creative requests can be cancelled");
        }

        request.cancel();
        creativeRequestRepository.save(request);

        AiJobStateCacheEntry jobState = null;
        if (latestVersion != null && latestVersion.getGenerationStatus() == com.lebhas.creativesaas.generatedversion.domain.GenerationStatus.QUEUED) {
            latestVersion.markFailed();
            generatedVersionService.save(latestVersion);
            jobState = new AiJobStateCacheEntry(
                    request.getWorkspaceId(),
                    request.getId(),
                    latestVersion.getId(),
                    resolveProviderType(latestVersion.getGeneratedByProvider()),
                    latestVersion.getGeneratedByModel(),
                    AiJobState.CANCELLED,
                    0,
                    null,
                    "Creative request cancelled",
                    Instant.now());
            storeJobState(jobState);
            storeProgress(
                    request.getWorkspaceId(),
                    request.getId(),
                    latestVersion.getId(),
                    AiJobState.CANCELLED,
                    "cancelled",
                    "Creative request cancelled");
        }

        if (request.getCreditReservationId() != null) {
            creditReservationService.refund(
                    request.getWorkspaceId(),
                    request.getId(),
                    latestVersion == null ? null : latestVersion.getId(),
                    request.getCreditReservationId(),
                    CREDIT_REFERENCE_TYPE,
                    request.getId(),
                    estimatedCreditCost,
                    "creative_request_cancelled");
        }

        List<GeneratedVersionEntity> versions = generatedVersionService.listByCreativeRequest(
                request.getWorkspaceId(),
                request.getId());
        return creativeRequestMapper.toResponse(request, versions, jobState, estimatedCreditCost);
    }

    @Transactional
    public CreativeRequestResponse retryFailedRequest(
            UUID actorUserId,
            CreativeRequestEntity request,
            CreativeRequestGenerationPlan plan
    ) {
        if (request.getStatus() != CreativeRequestStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only failed creative requests can be retried");
        }

        generationRetryService.validateRetryAllowed(request.getWorkspaceId(), request.getId());

        RedisLockService.RedisLockToken duplicateLock = aiDuplicateGenerationLockService
                .acquire(request.getWorkspaceId(), request.getId(), plan.duplicateRequestHash())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GENERATION_STATE_CONFLICT,
                        "A matching creative request is already queued"));
        try {
            GenerationOrchestrator.QueuedGeneration queuedGeneration = generationOrchestrator.queueGeneration(
                    request,
                    actorUserId,
                    plan.estimatedCreditCost());
            publishCreativeRequestQueuedSafely(request, actorUserId, queuedGeneration.reservation().reservationId(), "creative_request_retried");

            List<GeneratedVersionEntity> versions = generatedVersionService.listByCreativeRequest(
                    request.getWorkspaceId(),
                    request.getId());
            return creativeRequestMapper.toResponse(request, versions, null, plan.estimatedCreditCost());
        } finally {
            aiDuplicateGenerationLockService.release(request.getWorkspaceId(), request.getId(), duplicateLock);
        }
    }

    private BigDecimal creativeRequestValidationEstimatedCost(CreativeRequestEntity request) {
        return request == null ? BigDecimal.ZERO : creativeRequestValidationService.estimateCost(request);
    }

    private void storePipelineState(
            UUID workspaceId,
            UUID creativeRequestId,
            PipelineResolutionContext context,
            String state,
            String currentLayerType
    ) {
        aiPipelineExecutionStateCacheService.store(new PipelineExecutionStateCacheEntry(
                workspaceId,
                creativeRequestId,
                context.pipeline().getId(),
                state,
                0,
                currentLayerType,
                Map.of("pipelineVersion", context.pipeline().getVersion()),
                Instant.now()));
    }

    private AiGenerationLifecycleEvent generationEvent(
            CreativeRequestEntity request,
            UUID generatedVersionId,
            UUID generationJobId,
            PipelineResolutionContext context,
            String status,
            String reason,
            Map<String, Object> metadata
    ) {
        return new AiGenerationLifecycleEvent(
                null,
                null,
                request.getWorkspaceId(),
                request.getId(),
                generatedVersionId,
                generationJobId,
                context.pipeline().getId(),
                status,
                reason,
                metadata);
    }


    private void storeJobState(AiJobStateCacheEntry jobState) {
        if (!aiJobStateRedisService.store(jobState)) {
            throw new BusinessException(ErrorCode.REDIS_OPERATION_FAILED, "AI job state could not be created");
        }
    }

    private void storeProgress(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID jobId,
            AiJobState state,
            String stage,
            String message
    ) {
        boolean stored = aiGenerationProgressRedisService.store(new AiGenerationProgressCacheEntry(
                workspaceId,
                creativeRequestId,
                jobId,
                state,
                0,
                stage,
                message,
                Instant.now()));
        if (!stored) {
            throw new BusinessException(ErrorCode.REDIS_OPERATION_FAILED, "AI generation progress could not be created");
        }
    }

    private com.lebhas.ai.provider.AiProviderType resolveProviderType(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return null;
        }
        try {
            return com.lebhas.ai.provider.AiProviderType.valueOf(providerName.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void publishCreativeRequestCreatedSafely(
            CreativeRequestEntity request,
            UUID actorUserId,
            String origin
    ) {
        try {
            domainEventPublisher.publish(
                    KafkaTopicConstants.CREATIVE_REQUEST_CREATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.CREATIVE_REQUEST_CREATED,
                            request.getWorkspaceId(),
                            request.getId(),
                            Instant.now(),
                            java.util.Map.of(
                                    "creativeRequestId", request.getId(),
                                    "projectCampaignId", request.getProjectCampaignId(),
                                    "actorUserId", actorUserId,
                                    "status", request.getStatus().name(),
                                    "origin", origin)));
        } catch (RuntimeException ignored) {
        }
    }

    private void publishCreativeRequestQueuedSafely(
            CreativeRequestEntity request,
            UUID actorUserId,
            UUID creditReservationId,
            String origin
    ) {
        try {
            domainEventPublisher.publish(
                    KafkaTopicConstants.CREATIVE_REQUEST_QUEUED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.CREATIVE_REQUEST_QUEUED,
                            request.getWorkspaceId(),
                            request.getId(),
                            Instant.now(),
                            java.util.Map.of(
                                    "creativeRequestId", request.getId(),
                                    "projectCampaignId", request.getProjectCampaignId(),
                                    "actorUserId", actorUserId,
                                    "creditReservationId", creditReservationId,
                                    "status", request.getStatus().name(),
                                    "origin", origin)));
        } catch (RuntimeException ignored) {
        }
    }
}
