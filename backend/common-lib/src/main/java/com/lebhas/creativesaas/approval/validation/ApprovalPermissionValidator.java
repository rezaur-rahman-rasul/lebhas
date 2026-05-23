package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.redis.RedisSessionService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

@Component
public class ApprovalPermissionValidator {

    private final RedisSessionService redisSessionService;
    private final Clock clock;

    public ApprovalPermissionValidator(RedisSessionService redisSessionService, Clock clock) {
        this.redisSessionService = redisSessionService;
        this.clock = clock;
    }

    public boolean hasMasterSupportVisibility(CurrentUser currentUser, Set<Permission> permissions, UUID workspaceId) {
        if (currentUser == null || !currentUser.isMaster()) {
            return false;
        }
        if (permissions == null || !permissions.contains(Permission.SUPPORT_WORKSPACE_ACCESS)) {
            return false;
        }
        return redisSessionService.getSupportSession(currentUser.userId())
                .filter(session -> workspaceId != null && workspaceId.equals(session.workspaceId()))
                .filter(session -> session.expiresAt() == null || session.expiresAt().isAfter(clock.instant()))
                .isPresent();
    }

    public void requireMasterSupportVisibility(CurrentUser currentUser, Set<Permission> permissions, UUID workspaceId) {
        if (currentUser != null && currentUser.isMaster() && !hasMasterSupportVisibility(currentUser, permissions, workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Master approval access requires active support mode for the workspace");
        }
    }

    public boolean canViewApproval(CurrentUser currentUser, Role effectiveRole, Set<Permission> permissions, UUID workspaceId) {
        if (currentUser != null && currentUser.isMaster()) {
            return hasMasterSupportVisibility(currentUser, permissions, workspaceId);
        }
        return permissions != null && (permissions.contains(Permission.CREATIVE_SUBMIT) || permissions.contains(Permission.GENERATED_VERSION_MANAGE));
    }

    public void requireApprovalVisibility(CurrentUser currentUser, Role effectiveRole, Set<Permission> permissions, UUID workspaceId) {
        if (!canViewApproval(currentUser, effectiveRole, permissions, workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval visibility is not permitted for this user");
        }
    }

    public boolean canAssignReviewer(CurrentUser currentUser, Role effectiveRole, Set<Permission> permissions, UUID workspaceId) {
        if (currentUser != null && currentUser.isMaster()) {
            return hasMasterSupportVisibility(currentUser, permissions, workspaceId);
        }
        return effectiveRole == Role.ADMIN && permissions != null && permissions.contains(Permission.CREATIVE_SUBMIT);
    }

    public void requireAssignmentPermission(CurrentUser currentUser, Role effectiveRole, Set<Permission> permissions, UUID workspaceId) {
        if (!canAssignReviewer(currentUser, effectiveRole, permissions, workspaceId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Reviewer assignment is not permitted for this user");
        }
    }

    public boolean canReviewApproval(
            CurrentUser currentUser,
            Role effectiveRole,
            Set<Permission> permissions,
            UUID workspaceId,
            ApprovalRequest approvalRequest
    ) {
        if (currentUser == null) {
            return false;
        }
        if (currentUser.isMaster()) {
            return hasMasterSupportVisibility(currentUser, permissions, workspaceId);
        }
        if (effectiveRole == Role.ADMIN) {
            return permissions != null && permissions.contains(Permission.CREATIVE_SUBMIT);
        }
        return effectiveRole == Role.CREW
                && permissions != null
                && permissions.contains(Permission.CREATIVE_SUBMIT)
                && approvalRequest != null
                && currentUser.userId().equals(approvalRequest.getAssignedReviewerId());
    }

    public void requireReviewerActionPermission(
            CurrentUser currentUser,
            Role effectiveRole,
            Set<Permission> permissions,
            UUID workspaceId,
            ApprovalRequest approvalRequest
    ) {
        if (!canReviewApproval(currentUser, effectiveRole, permissions, workspaceId, approvalRequest)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval review action is not permitted for this user");
        }
    }
}
