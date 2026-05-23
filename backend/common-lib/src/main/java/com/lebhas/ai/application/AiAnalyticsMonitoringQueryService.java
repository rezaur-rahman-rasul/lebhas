package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.AiFailureLogView;
import com.lebhas.ai.application.dto.AiLayerAnalyticsView;
import com.lebhas.ai.application.dto.DynamicRoutingOptimizationResult;
import com.lebhas.ai.application.dto.ProviderHealthSnapshot;
import com.lebhas.ai.application.dto.ProviderMetricsSnapshot;
import com.lebhas.ai.application.dto.QualityScoreResult;
import com.lebhas.ai.application.dto.RoutingOptimizationRequest;
import com.lebhas.ai.application.dto.WorkspaceAiUsageView;
import com.lebhas.ai.domain.AiFailureLog;
import com.lebhas.ai.domain.AiFailureType;
import com.lebhas.ai.domain.AiLayerAnalytics;
import com.lebhas.ai.infrastructure.persistence.AiFailureLogRepository;
import com.lebhas.ai.infrastructure.persistence.AiLayerAnalyticsRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiAnalyticsMonitoringQueryService {

    private static final int DEFAULT_FAILURE_LIMIT = 100;
    private static final int MAX_FAILURE_LIMIT = 500;

    private final AiProviderHealthService providerHealthService;
    private final AiLayerAnalyticsRepository layerAnalyticsRepository;
    private final WorkspaceAiUsageQueryService workspaceAiUsageQueryService;
    private final GeneratedVersionQualityService generatedVersionQualityService;
    private final AiFailureLogRepository failureLogRepository;
    private final DynamicRoutingOptimizationService routingOptimizationService;
    private final CurrentUserContext currentUserContext;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public AiAnalyticsMonitoringQueryService(
            AiProviderHealthService providerHealthService,
            AiLayerAnalyticsRepository layerAnalyticsRepository,
            WorkspaceAiUsageQueryService workspaceAiUsageQueryService,
            GeneratedVersionQualityService generatedVersionQualityService,
            AiFailureLogRepository failureLogRepository,
            DynamicRoutingOptimizationService routingOptimizationService,
            CurrentUserContext currentUserContext,
            WorkspaceAuthorizationService workspaceAuthorizationService
    ) {
        this.providerHealthService = providerHealthService;
        this.layerAnalyticsRepository = layerAnalyticsRepository;
        this.workspaceAiUsageQueryService = workspaceAiUsageQueryService;
        this.generatedVersionQualityService = generatedVersionQualityService;
        this.failureLogRepository = failureLogRepository;
        this.routingOptimizationService = routingOptimizationService;
        this.currentUserContext = currentUserContext;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<ProviderMetricsSnapshot> listProviderMetricsForMaster() {
        requireMaster();
        return providerHealthService.listProviderHealth().stream()
                .flatMap(health -> health.modelMetrics().stream())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderHealthSnapshot getProviderHealthForMaster(UUID providerId) {
        requireMaster();
        return providerHealthService.getProviderHealth(providerId);
    }

    @Transactional(readOnly = true)
    public List<AiLayerAnalyticsView> getLayerAnalyticsForMaster(UUID layerId) {
        requireMaster();
        return layerAnalyticsRepository.findAllByLayerIdAndDeletedFalse(layerId).stream()
                .map(this::toLayerAnalyticsView)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceAiUsageView getWorkspaceUsage(UUID workspaceId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            boolean adminOwnWorkspace = currentUser.hasRole(Role.ADMIN) && workspaceId != null && workspaceId.equals(currentUser.workspaceId());
            if (!adminOwnWorkspace) {
                workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
            }
        }
        return workspaceAiUsageQueryService.getWorkspaceUsage(workspaceId);
    }

    @Transactional(readOnly = true)
    public QualityScoreResult getQualityScoreForMaster(UUID generatedVersionId) {
        requireMaster();
        return generatedVersionQualityService.getQualityScore(generatedVersionId);
    }

    @Transactional(readOnly = true)
    public List<AiFailureLogView> listFailuresForMaster(
            UUID providerId,
            UUID layerId,
            UUID creativeRequestId,
            AiFailureType failureType,
            Integer limit
    ) {
        requireMaster();
        List<AiFailureLog> failures = resolveFailures(providerId, layerId, creativeRequestId, failureType);
        int normalizedLimit = normalizeLimit(limit);
        return failures.stream()
                .sorted(Comparator.comparing(AiFailureLog::getCreatedAt).reversed())
                .limit(normalizedLimit)
                .map(this::toFailureLogView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DynamicRoutingOptimizationResult recommendRoutingForMaster(
            UUID workspaceId,
            UUID layerId,
            UUID creativeRequestId,
            BigDecimal requestedUnits
    ) {
        requireMaster();
        return routingOptimizationService.recommendRouting(new RoutingOptimizationRequest(
                workspaceId,
                layerId,
                creativeRequestId,
                requestedUnits,
                Map.of()));
    }

    private void requireMaster() {
        if (!currentUserContext.requireCurrentUser().isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private List<AiFailureLog> resolveFailures(
            UUID providerId,
            UUID layerId,
            UUID creativeRequestId,
            AiFailureType failureType
    ) {
        if (providerId != null) {
            return failureLogRepository.findAllByProviderIdAndDeletedFalseOrderByCreatedAtDesc(providerId).stream()
                    .filter(log -> layerId == null || layerId.equals(log.getLayerId()))
                    .filter(log -> creativeRequestId == null || creativeRequestId.equals(log.getCreativeRequestId()))
                    .filter(log -> failureType == null || failureType == log.getFailureType())
                    .toList();
        }
        if (layerId != null) {
            return failureLogRepository.findAllByLayerIdAndDeletedFalseOrderByCreatedAtDesc(layerId).stream()
                    .filter(log -> creativeRequestId == null || creativeRequestId.equals(log.getCreativeRequestId()))
                    .filter(log -> failureType == null || failureType == log.getFailureType())
                    .toList();
        }
        if (creativeRequestId != null) {
            return failureLogRepository.findAllByCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(creativeRequestId).stream()
                    .filter(log -> failureType == null || failureType == log.getFailureType())
                    .toList();
        }
        if (failureType != null) {
            return failureLogRepository.findAllByFailureTypeAndDeletedFalseOrderByCreatedAtDesc(failureType);
        }
        return failureLogRepository.findAll().stream()
                .filter(log -> !log.isDeleted())
                .toList();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_FAILURE_LIMIT;
        }
        return Math.min(limit, MAX_FAILURE_LIMIT);
    }

    private AiLayerAnalyticsView toLayerAnalyticsView(AiLayerAnalytics analytics) {
        return new AiLayerAnalyticsView(
                analytics.getId(),
                analytics.getLayerId(),
                analytics.getProviderId(),
                analytics.getModelName(),
                analytics.getTotalExecutions(),
                analytics.getSuccessfulExecutions(),
                analytics.getFailedExecutions(),
                analytics.getAvgExecutionTimeMs(),
                analytics.getAvgExecutionCostUsd(),
                analytics.getAvgQualityScore(),
                analytics.getCreatedAt(),
                analytics.getUpdatedAt());
    }

    private AiFailureLogView toFailureLogView(AiFailureLog failureLog) {
        return new AiFailureLogView(
                failureLog.getId(),
                failureLog.getCreativeRequestId(),
                failureLog.getLayerId(),
                failureLog.getProviderId(),
                failureLog.getModelName(),
                failureLog.getFailureType(),
                failureLog.getFailureReason(),
                failureLog.getRetryAttempt(),
                failureLog.isFallbackTriggered(),
                failureLog.getCreatedAt());
    }
}
