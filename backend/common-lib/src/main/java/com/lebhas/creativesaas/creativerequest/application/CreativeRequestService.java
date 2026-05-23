package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.dto.CancelCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.RetryCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.UpdateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.cache.CreativeRequestCacheService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreativeRequestService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestCommandService creativeRequestCommandService;
    private final CreativeRequestQueryService creativeRequestQueryService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final CreativeRequestCacheService creativeRequestCacheService;
    private final DomainEventPublisher domainEventPublisher;

    public CreativeRequestService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestCommandService creativeRequestCommandService,
            CreativeRequestQueryService creativeRequestQueryService,
            CreativeRequestValidationService creativeRequestValidationService,
            CreativeRequestRepository creativeRequestRepository,
            CreativeRequestCacheService creativeRequestCacheService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestCommandService = creativeRequestCommandService;
        this.creativeRequestQueryService = creativeRequestQueryService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.creativeRequestCacheService = creativeRequestCacheService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreativeRequestResponse createCreativeRequest(CreateCreativeRequestCommand command) {
        CreativeRequestResponse response = creativeRequestCommandService.createCreativeRequest(command);
        creativeRequestCacheService.store(response.request());
        return response;
    }

    @Transactional
    public CreativeRequestResponse updateCreativeRequest(UpdateCreativeRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(
                command.workspaceId(),
                command.creativeRequestId(),
                access);
        if (!isMutable(request)) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Only draft, failed, or cancelled creative requests can be updated");
        }

        CreativeRequestGenerationPlan plan = creativeRequestValidationService.validateForCreate(
                new CreateCreativeRequestCommand(
                        command.workspaceId(),
                        command.brandId(),
                        command.productServiceId(),
                        command.projectCampaignId(),
                        command.requestName(),
                        command.sourcePrompt(),
                        command.enhancedPrompt(),
                        command.languagePreference(),
                        command.creativeObjective(),
                        command.targetPlatform(),
                        command.requestedFormat(),
                        command.requestedVersions(),
                        command.selectedAssetIds()),
                access);

        request.revise(
                plan.brandId(),
                plan.productServiceId(),
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
                plan.requestedVersions(),
                plan.requestedFormatText(),
                plan.selectedAssetIds());

        CreativeRequestEntity saved = creativeRequestRepository.save(request);
        creativeRequestCacheService.store(saved);
        return creativeRequestQueryService.buildResponse(saved);
    }

    @Transactional
    public void deleteCreativeRequest(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(
                workspaceId,
                creativeRequestId,
                access);
        if (!isDeletable(request)) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Queued or processing creative requests cannot be deleted");
        }
        request.markDeleted();
        creativeRequestRepository.save(request);
        creativeRequestCacheService.invalidate(workspaceId, creativeRequestId);
    }

    @Transactional
    public CreativeRequestResponse cancelQueuedRequest(CancelCreativeRequestCommand command) {
        CreativeRequestResponse response = creativeRequestCommandService.cancelQueuedRequest(command);
        creativeRequestCacheService.store(response.request());
        return response;
    }

    @Transactional
    public CreativeRequestResponse retryFailedRequest(RetryCreativeRequestCommand command) {
        CreativeRequestResponse response = creativeRequestCommandService.retryFailedRequest(command);
        creativeRequestCacheService.store(response.request());
        return response;
    }

    @Transactional
    public CreativeRequestResponse failCreativeRequest(UUID workspaceId, UUID creativeRequestId, String failureReason) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(
                workspaceId,
                creativeRequestId,
                access);
        if (request.getStatus() == CreativeRequestStatus.COMPLETED
                || request.getStatus() == CreativeRequestStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.GENERATION_STATE_CONFLICT,
                    "Completed or cancelled creative requests cannot be marked as failed");
        }
        request.markFailed(failureReason);
        CreativeRequestEntity saved = creativeRequestRepository.save(request);
        creativeRequestCacheService.store(saved);
        publishCreativeRequestFailedSafely(saved, access.currentUser().userId(), failureReason);
        return creativeRequestQueryService.buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public CreativeRequestResponse getRequest(UUID workspaceId, UUID creativeRequestId) {
        return creativeRequestQueryService.getRequest(workspaceId, creativeRequestId);
    }

    @Transactional(readOnly = true)
    public List<CreativeRequestResponse> listCreativeRequests(UUID workspaceId) {
        return creativeRequestQueryService.listCreativeRequests(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<CreativeRequestResponse> listProjectCreativeRequests(UUID workspaceId, UUID projectCampaignId) {
        return creativeRequestQueryService.listProjectCreativeRequests(workspaceId, projectCampaignId);
    }

    private boolean isMutable(CreativeRequestEntity request) {
        return request.getStatus() == CreativeRequestStatus.DRAFT
                || request.getStatus() == CreativeRequestStatus.FAILED
                || request.getStatus() == CreativeRequestStatus.CANCELLED;
    }

    private boolean isDeletable(CreativeRequestEntity request) {
        return request.getStatus() != CreativeRequestStatus.QUEUED
                && request.getStatus() != CreativeRequestStatus.PROCESSING;
    }

    private void publishCreativeRequestFailedSafely(
            CreativeRequestEntity request,
            UUID actorUserId,
            String failureReason
    ) {
        try {
            domainEventPublisher.publish(
                    KafkaTopicConstants.CREATIVE_REQUEST_FAILED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.CREATIVE_REQUEST_FAILED,
                            request.getWorkspaceId(),
                            request.getId(),
                            Instant.now(),
                            Map.of(
                                    "creativeRequestId", request.getId(),
                                    "projectCampaignId", request.getProjectCampaignId(),
                                    "actorUserId", actorUserId,
                                    "status", request.getStatus().name(),
                                    "reason", failureReason == null ? "" : failureReason)));
        } catch (RuntimeException ignored) {
        }
    }
}
