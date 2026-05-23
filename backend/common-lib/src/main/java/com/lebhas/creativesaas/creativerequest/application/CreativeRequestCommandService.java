package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.dto.CancelCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreateCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.application.dto.RetryCreativeRequestCommand;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestStatus;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreativeRequestCommandService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final CreativeRequestQueryService creativeRequestQueryService;
    private final GeneratedVersionService generatedVersionService;
    private final CreativeGenerationOrchestrator creativeGenerationOrchestrator;

    public CreativeRequestCommandService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestValidationService creativeRequestValidationService,
            CreativeRequestQueryService creativeRequestQueryService,
            GeneratedVersionService generatedVersionService,
            CreativeGenerationOrchestrator creativeGenerationOrchestrator
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.creativeRequestQueryService = creativeRequestQueryService;
        this.generatedVersionService = generatedVersionService;
        this.creativeGenerationOrchestrator = creativeGenerationOrchestrator;
    }

    @Transactional
    public CreativeRequestResponse createCreativeRequest(CreateCreativeRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestGenerationPlan plan = creativeRequestValidationService.validateForCreate(command, access);
        return creativeGenerationOrchestrator.createQueuedRequest(
                command.workspaceId(),
                access.currentUser().userId(),
                plan);
    }

    @Transactional
    public CreativeRequestResponse cancelQueuedRequest(CancelCreativeRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(
                command.workspaceId(),
                command.creativeRequestId(),
                access);
        if (request.getStatus() != CreativeRequestStatus.QUEUED) {
            throw new BusinessException(ErrorCode.GENERATION_STATE_CONFLICT, "Only queued creative requests can be cancelled");
        }
        GeneratedVersionEntity latestVersion = generatedVersionService.latestByCreativeRequest(
                        request.getWorkspaceId(),
                        request.getId())
                .orElse(null);
        return creativeGenerationOrchestrator.cancelQueuedRequest(
                request,
                latestVersion,
                creativeRequestValidationService.estimateCost(request));
    }

    @Transactional
    public CreativeRequestResponse retryFailedRequest(RetryCreativeRequestCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_REQUEST_CREATE);
        CreativeRequestEntity request = creativeRequestQueryService.requireAccessibleRequest(
                command.workspaceId(),
                command.creativeRequestId(),
                access);
        if (request.getStatus() != CreativeRequestStatus.FAILED) {
            throw new BusinessException(ErrorCode.GENERATION_STATE_CONFLICT, "Only failed creative requests can be retried");
        }
        CreativeRequestGenerationPlan plan = creativeRequestValidationService.validateForRetry(request, access);
        return creativeGenerationOrchestrator.retryFailedRequest(access.currentUser().userId(), request, plan);
    }
}
