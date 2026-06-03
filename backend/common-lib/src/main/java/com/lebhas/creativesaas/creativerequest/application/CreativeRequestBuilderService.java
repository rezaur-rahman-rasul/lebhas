package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.brand.domain.BrandLanguagePreference;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestReadinessView;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.GenerationPreviewView;
import com.lebhas.creativesaas.creativerequest.application.dto.QueuedGenerationJobView;
import com.lebhas.creativesaas.creativerequest.cache.CreativeRequestCacheService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.application.GenerationOrchestrator;
import com.lebhas.creativesaas.generation.application.dto.GenerationJobView;
import com.lebhas.creativesaas.generation.cache.GenerationLockService;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.prompt.domain.PromptDraftEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptDraftRepository;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativeRequestBuilderService {

    private static final String DEFAULT_QUEUE_NAME = "creative-generation";

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final CreativeRequestQueryService creativeRequestQueryService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final CreativeRequestCacheService creativeRequestCacheService;
    private final PromptDraftRepository promptDraftRepository;
    private final GenerationOrchestrator generationOrchestrator;
    private final GenerationLockService generationLockService;
    private final CreativeRequestMapper creativeRequestMapper;
    private final DomainEventPublisher domainEventPublisher;

    public CreativeRequestBuilderService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestValidationService creativeRequestValidationService,
            CreativeRequestQueryService creativeRequestQueryService,
            CreativeRequestRepository creativeRequestRepository,
            CreativeRequestCacheService creativeRequestCacheService,
            PromptDraftRepository promptDraftRepository,
            GenerationOrchestrator generationOrchestrator,
            GenerationLockService generationLockService,
            CreativeRequestMapper creativeRequestMapper,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.creativeRequestQueryService = creativeRequestQueryService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.creativeRequestCacheService = creativeRequestCacheService;
        this.promptDraftRepository = promptDraftRepository;
        this.generationOrchestrator = generationOrchestrator;
        this.generationLockService = generationLockService;
        this.creativeRequestMapper = creativeRequestMapper;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreativeRequestResponse createManual(CreateCreativeRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestGenerationPlan plan = validateForCreate(command, access);
        CreativeRequestEntity request = createDraft(command.workspaceId(), access.currentUser().userId(), plan);
        publishSafely(KafkaTopicConstants.CREATIVE_REQUEST_CREATED, request, access.currentUser().userId(), Map.of(
                "origin", "manual",
                "status", request.getStatus().name()));
        return creativeRequestMapper.toResponse(request, List.of(), null, plan.estimatedCreditCost());
    }

    @Transactional
    public CreativeRequestResponse createFromPrompt(
            UUID workspaceId,
            UUID projectId,
            UUID promptDraftId,
            String requestName,
            String enhancedPrompt,
            String requestedFormat,
            Integer requestedVersions,
            List<UUID> selectedAssetIds
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE);
        PromptDraftEntity draft = promptDraftRepository.findByIdAndWorkspaceIdAndDeletedFalse(promptDraftId, workspaceId)
                .filter(promptDraft -> projectId.equals(promptDraft.getProjectId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_DRAFT_NOT_FOUND));

        CreateCreativeRequestCommand command = new CreateCreativeRequestCommand(
                workspaceId,
                null,
                null,
                projectId,
                requestName == null || requestName.isBlank() ? draft.getTitle() : requestName,
                draft.getPromptText(),
                enhancedPrompt,
                toLanguagePreference(draft.getLanguage()),
                draft.getCampaignObjective() == null ? null : draft.getCampaignObjective().name(),
                draft.getPlatform() == null ? null : draft.getPlatform().name(),
                requestedFormat,
                requestedVersions,
                selectedAssetIds);
        CreativeRequestGenerationPlan plan = validateForCreate(command, access);
        CreativeRequestEntity request = createDraft(workspaceId, access.currentUser().userId(), plan);
        publishSafely(KafkaTopicConstants.CREATIVE_REQUEST_CREATED, request, access.currentUser().userId(), Map.of(
                "origin", "prompt_draft",
                "promptDraftId", draft.getId(),
                "status", request.getStatus().name()));
        return creativeRequestMapper.toResponse(request, List.of(), null, plan.estimatedCreditCost());
    }

    @Transactional(readOnly = true)
    public CreativeRequestReadinessView validate(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access);
        try {
            validateRequestState(request);
            creativeRequestValidationService.validateForRetry(request, access);
            return CreativeRequestReadinessView.ready(request.getId());
        } catch (BusinessException exception) {
            publishValidationFailedSafely(request, exception.getMessage());
            throw readinessBlocked(request.getId(), List.of(exception.getMessage()));
        }
    }

    @Transactional
    public GenerationPreviewView preview(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access);
        CreativeRequestGenerationPlan plan = assertReady(request, access);
        GenerationPreviewView preview = new GenerationPreviewView(
                request.getId(),
                request.getRequestedVersions(),
                plan.estimatedCreditCost(),
                false);
        publishSafely(KafkaTopicConstants.GENERATION_PREVIEW_CREATED, request, access.currentUser().userId(), Map.of(
                "estimatedCreditCost", preview.estimatedCreditCost(),
                "requestedVersionCount", preview.requestedVersionCount()));
        return preview;
    }

    @Transactional
    public QueuedGenerationJobView queue(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access);
        CreativeRequestGenerationPlan plan = assertReady(request, access);
        RedisLockService.RedisLockToken lock = generationLockService.acquire(workspaceId, creativeRequestId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GENERATION_STATE_CONFLICT,
                        "Generation is already queued for this creative request"));
        try {
            GenerationOrchestrator.QueuedGeneration queued = generationOrchestrator.queueGeneration(
                    request,
                    access.currentUser().userId(),
                    plan.estimatedCreditCost());
            creativeRequestCacheService.store(queued.request());
            publishSafely(KafkaTopicConstants.CREATIVE_REQUEST_QUEUED, queued.request(), access.currentUser().userId(), Map.of(
                    "creditReservationId", queued.reservation().reservationId(),
                    "generationJobId", queued.job().getId()));
            return new QueuedGenerationJobView(toJobView(queued.job()), queued.reservation());
        } finally {
            generationLockService.release(lock, workspaceId, creativeRequestId);
        }
    }

    private CreativeRequestEntity createDraft(
            UUID workspaceId,
            UUID actorUserId,
            CreativeRequestGenerationPlan plan
    ) {
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
        request = creativeRequestRepository.save(request);
        creativeRequestCacheService.store(request);
        return request;
    }

    private CreativeRequestGenerationPlan validateForCreate(
            CreateCreativeRequestCommand command,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        try {
            return creativeRequestValidationService.validateForCreate(command, access);
        } catch (BusinessException exception) {
            publishValidationFailedSafely(command.workspaceId(), command.projectCampaignId(), exception.getMessage());
            throw readinessBlocked(command.projectCampaignId(), List.of(exception.getMessage()));
        }
    }

    private CreativeRequestGenerationPlan assertReady(
            CreativeRequestEntity request,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        try {
            validateRequestState(request);
            return creativeRequestValidationService.validateForRetry(request, access);
        } catch (BusinessException exception) {
            publishValidationFailedSafely(request, exception.getMessage());
            throw readinessBlocked(request.getId(), List.of(exception.getMessage()));
        }
    }

    private void validateRequestState(CreativeRequestEntity request) {
        if (request.getStatus() != CreativeRequestStatus.DRAFT && request.getStatus() != CreativeRequestStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only draft or failed creative requests can be queued for generation");
        }
    }

    private BusinessException readinessBlocked(UUID entityId, List<String> reasons) {
        List<ApiError> errors = reasons.stream()
                .map(reason -> ApiError.of(ErrorCode.GENERATION_READINESS_BLOCKED.code(), reason))
                .toList();
        return new BusinessException(
                ErrorCode.GENERATION_READINESS_BLOCKED,
                "Creative request is not ready for generation",
                errors.isEmpty()
                        ? List.of(ApiError.of(ErrorCode.GENERATION_READINESS_BLOCKED.code(), "Creative request is not ready for generation"))
                        : errors);
    }

    private BrandLanguagePreference toLanguagePreference(PromptLanguage language) {
        if (language == PromptLanguage.BANGLA) {
            return BrandLanguagePreference.BANGLA;
        }
        if (language == PromptLanguage.ENGLISH) {
            return BrandLanguagePreference.ENGLISH;
        }
        return BrandLanguagePreference.BOTH;
    }

    private GenerationJobView toJobView(GenerationJobEntity job) {
        return new GenerationJobView(
                job.getId(),
                job.getWorkspaceId(),
                job.getRequestId(),
                job.getJobType(),
                job.getStatus(),
                job.getProviderJobId(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getQueueName() == null ? DEFAULT_QUEUE_NAME : job.getQueueName(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getFailedAt(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private void publishValidationFailedSafely(CreativeRequestEntity request, String reason) {
        publishValidationFailedSafely(request.getWorkspaceId(), request.getProjectCampaignId(), reason);
    }

    private void publishValidationFailedSafely(UUID workspaceId, UUID projectId, String reason) {
        try {
            domainEventPublisher.publish(
                    KafkaTopicConstants.CREATIVE_REQUEST_VALIDATION_FAILED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.CREATIVE_REQUEST_VALIDATION_FAILED,
                            workspaceId,
                            projectId == null ? workspaceId : projectId,
                            Instant.now(),
                            Map.of("reason", reason == null ? "" : reason)));
        } catch (RuntimeException ignored) {
        }
    }

    private void publishSafely(
            String topic,
            CreativeRequestEntity request,
            UUID actorUserId,
            Map<String, Object> attributes
    ) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("creativeRequestId", request.getId());
            payload.put("projectCampaignId", request.getProjectCampaignId());
            payload.put("actorUserId", actorUserId);
            payload.putAll(attributes);
            domainEventPublisher.publish(
                    topic,
                    new BaseDomainEvent(topic, request.getWorkspaceId(), request.getId(), Instant.now(), payload));
        } catch (RuntimeException ignored) {
        }
    }
}
