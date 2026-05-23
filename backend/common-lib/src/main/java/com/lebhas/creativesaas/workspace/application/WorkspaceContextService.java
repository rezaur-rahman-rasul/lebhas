package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.application.MasterSupportModeService;
import com.lebhas.creativesaas.identity.application.SessionProperties;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import com.lebhas.creativesaas.redis.RedisPermissionVersionService;
import com.lebhas.creativesaas.redis.RedisRealtimeStateService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.redis.RedisWorkspaceContextCache;
import com.lebhas.creativesaas.workspace.application.dto.SupportModeView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceContextView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkspaceContextService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final RedisWorkspaceContextCache redisWorkspaceContextCache;
    private final RedisPermissionVersionService redisPermissionVersionService;
    private final RedisRealtimeStateService redisRealtimeStateService;
    private final MasterSupportModeService masterSupportModeService;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final SessionProperties sessionProperties;

    public WorkspaceContextService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            RedisWorkspaceContextCache redisWorkspaceContextCache,
            RedisPermissionVersionService redisPermissionVersionService,
            RedisRealtimeStateService redisRealtimeStateService,
            MasterSupportModeService masterSupportModeService,
            WorkspacePlanContextService workspacePlanContextService,
            SessionProperties sessionProperties
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.redisWorkspaceContextCache = redisWorkspaceContextCache;
        this.redisPermissionVersionService = redisPermissionVersionService;
        this.redisRealtimeStateService = redisRealtimeStateService;
        this.masterSupportModeService = masterSupportModeService;
        this.workspacePlanContextService = workspacePlanContextService;
        this.sessionProperties = sessionProperties;
    }

    @Transactional(readOnly = true)
    public WorkspaceContextView getWorkspaceContext(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        String deviceId = access.currentUser().deviceId() == null ? "unknown" : access.currentUser().deviceId();
        redisRealtimeStateService.markWorkspaceSessionActive(
                access.workspace().getId(),
                access.currentUser().userId(),
                deviceId,
                sessionProperties.getActiveStateTtl());
        RedisRealtimeStateService.WorkspaceActivitySnapshot activitySnapshot =
                redisRealtimeStateService.getWorkspaceActivity(access.workspace().getId());
        long permissionVersion = redisPermissionVersionService.getVersion(access.workspace().getId());
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(access.workspace().getId());
        PlanFeaturePolicyView featurePolicy = planContext.featurePolicy();
        SupportModeView supportModeView = access.currentUser().isMaster()
                ? masterSupportModeService.currentSupportMode()
                : new SupportModeView(access.currentUser().userId(), null, deviceId, false, null, null);
        RedisWorkspaceContextCache.WorkspaceContextSnapshot snapshot = access.currentUser().isMaster()
                ? new RedisWorkspaceContextCache.WorkspaceContextSnapshot(
                        access.workspace().getId(),
                        access.currentUser().userId(),
                        Role.MASTER.name(),
                        access.permissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                        true,
                        true,
                        Instant.now())
                : redisWorkspaceContextCache.get(access.workspace().getId(), access.currentUser().userId()).orElse(
                        new RedisWorkspaceContextCache.WorkspaceContextSnapshot(
                                access.workspace().getId(),
                                access.currentUser().userId(),
                                access.effectiveRole().name(),
                                access.permissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()),
                                true,
                                true,
                                Instant.now()));
        return new WorkspaceContextView(
                access.workspace().getId(),
                access.workspace().getName(),
                access.effectiveRole(),
                access.permissions(),
                snapshot.canDownloadCreative(),
                snapshot.canEditCreative(),
                activitySnapshot.activeUserCount(),
                activitySnapshot.activeSessionCount(),
                permissionVersion,
                supportModeView.active() && access.workspace().getId().equals(supportModeView.workspaceId()),
                supportModeView.startedAt(),
                supportModeView.expiresAt(),
                planContext.pricingPlan(),
                planContext.subscription(),
                featurePolicy,
                featurePolicy == null ? null : featurePolicy.maxGeneratedVersionsPerRequest(),
                featurePolicy == null ? null : featurePolicy.maxStorageGb(),
                featurePolicy != null && featurePolicy.allowApprovalWorkflow(),
                featurePolicy != null && featurePolicy.allowPublicShareLinks(),
                featurePolicy == null ? null : featurePolicy.maxTeamMembers(),
                featurePolicy == null ? null : featurePolicy.monthlyCreditLimit(),
                snapshot.cachedAt());
    }
}
