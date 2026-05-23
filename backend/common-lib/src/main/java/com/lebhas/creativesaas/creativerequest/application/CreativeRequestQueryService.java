package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.job.AiJobStateCacheEntry;
import com.lebhas.ai.job.AiJobStateRedisService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.creativerequest.cache.CreativeRequestCacheService;
import com.lebhas.creativesaas.creativerequest.cache.dto.CreativeRequestCacheEntry;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestResponse;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreativeRequestQueryService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectCampaignService projectCampaignService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final GeneratedVersionService generatedVersionService;
    private final CreativeRequestValidationService creativeRequestValidationService;
    private final AiJobStateRedisService aiJobStateRedisService;
    private final CreativeRequestCacheService creativeRequestCacheService;
    private final CreativeRequestMapper creativeRequestMapper;

    public CreativeRequestQueryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectCampaignService projectCampaignService,
            CreativeRequestRepository creativeRequestRepository,
            GeneratedVersionService generatedVersionService,
            CreativeRequestValidationService creativeRequestValidationService,
            AiJobStateRedisService aiJobStateRedisService,
            CreativeRequestCacheService creativeRequestCacheService,
            CreativeRequestMapper creativeRequestMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectCampaignService = projectCampaignService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.generatedVersionService = generatedVersionService;
        this.creativeRequestValidationService = creativeRequestValidationService;
        this.aiJobStateRedisService = aiJobStateRedisService;
        this.creativeRequestCacheService = creativeRequestCacheService;
        this.creativeRequestMapper = creativeRequestMapper;
    }

    @Transactional(readOnly = true)
    public CreativeRequestResponse getRequest(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        CreativeRequestCacheEntry request = requireAccessibleCachedRequest(workspaceId, creativeRequestId, access);
        return buildResponse(request);
    }

    @Transactional(readOnly = true)
    public List<CreativeRequestResponse> listCreativeRequests(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        List<CreativeRequestEntity> requests = creativeRequestRepository
                .findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId);
        if (!isPrivileged(access)) {
            requests = requests.stream()
                    .filter(request -> request.getRequestedBy().equals(access.currentUser().userId()))
                    .toList();
        }
        return requests.stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CreativeRequestResponse> listProjectCreativeRequests(UUID workspaceId, UUID projectCampaignId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        projectCampaignService.requireProjectCampaign(workspaceId, projectCampaignId);
        List<CreativeRequestEntity> requests = creativeRequestRepository
                .findAllByWorkspaceIdAndProjectCampaignIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, projectCampaignId);
        if (!isPrivileged(access)) {
            requests = requests.stream()
                    .filter(request -> request.getRequestedBy().equals(access.currentUser().userId()))
                    .toList();
        }
        return requests.stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreativeRequestEntity requireAccessibleRequest(
            UUID workspaceId,
            UUID creativeRequestId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        CreativeRequestEntity request = creativeRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        if (!isPrivileged(access) && !request.getRequestedBy().equals(access.currentUser().userId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return request;
    }

    private CreativeRequestCacheEntry requireAccessibleCachedRequest(
            UUID workspaceId,
            UUID creativeRequestId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        CreativeRequestCacheEntry request = creativeRequestCacheService.getOrLoad(workspaceId, creativeRequestId);
        if (request == null || !workspaceId.equals(request.workspaceId())) {
            throw new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND);
        }
        if (!isPrivileged(access) && !request.createdByUserId().equals(access.currentUser().userId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return request;
    }

    CreativeRequestResponse buildResponse(CreativeRequestEntity request) {
        List<GeneratedVersionEntity> versions = generatedVersionService.listByCreativeRequest(
                request.getWorkspaceId(),
                request.getId());
        GeneratedVersionEntity latestVersion = versions.isEmpty() ? null : versions.get(0);
        AiJobStateCacheEntry jobState = latestVersion == null
                ? null
                : aiJobStateRedisService
                        .get(request.getWorkspaceId(), request.getId(), latestVersion.getId(), latestVersion.getGeneratedByProvider())
                        .orElse(null);
        BigDecimal estimatedCost = creativeRequestValidationService.estimateCost(request);
        return creativeRequestMapper.toResponse(request, versions, jobState, estimatedCost);
    }

    CreativeRequestResponse buildResponse(CreativeRequestCacheEntry request) {
        List<GeneratedVersionEntity> versions = generatedVersionService.listByCreativeRequest(
                request.workspaceId(),
                request.id());
        GeneratedVersionEntity latestVersion = versions.isEmpty() ? null : versions.get(0);
        AiJobStateCacheEntry jobState = latestVersion == null
                ? null
                : aiJobStateRedisService
                        .get(request.workspaceId(), request.id(), latestVersion.getId(), latestVersion.getGeneratedByProvider())
                        .orElse(null);
        BigDecimal estimatedCost = creativeRequestValidationService.estimateCost(
                request.requestedFormat(),
                request.requestedVersions());
        return creativeRequestMapper.toResponse(request, versions, jobState, estimatedCost);
    }

    private boolean isPrivileged(WorkspaceAuthorizationService.WorkspaceAccess access) {
        return access.effectiveRole().isMaster() || access.effectiveRole() == Role.ADMIN;
    }
}
