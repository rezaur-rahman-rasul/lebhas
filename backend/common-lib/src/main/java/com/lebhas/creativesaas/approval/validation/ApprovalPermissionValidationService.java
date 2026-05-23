package com.lebhas.creativesaas.approval.validation;

import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ApprovalPermissionValidationService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ApprovalPermissionValidator approvalPermissionValidator;

    public ApprovalPermissionValidationService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ApprovalPermissionValidator approvalPermissionValidator
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.approvalPermissionValidator = approvalPermissionValidator;
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireApprovalVisibility(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        requireApprovalVisibility(access);
        return access;
    }

    public void requireApprovalVisibility(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (!canViewApproval(access)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval visibility is not permitted for this user");
        }
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireApprovalManagement(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        requireApprovalManagement(access);
        return access;
    }

    public void requireApprovalManagement(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (!canManageApproval(access)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval management is not permitted for this user");
        }
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireApprovalCreation(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        requireApprovalCreation(access);
        return access;
    }

    public void requireApprovalCreation(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (!canCreateApproval(access)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval workflow creation is not permitted for this user");
        }
    }

    public void requireApprovalAction(WorkspaceAuthorizationService.WorkspaceAccess access, ApprovalWorkflow workflow) {
        if (!canActOnApproval(access, workflow)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Approval action is not permitted for this user");
        }
    }

    public boolean canViewApproval(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (access == null) {
            return false;
        }
        CurrentUser currentUser = access.currentUser();
        if (currentUser != null && currentUser.isMaster()) {
            return approvalPermissionValidator.hasMasterSupportVisibility(
                    currentUser,
                    access.permissions(),
                    workspaceId(access));
        }
        if (access.effectiveRole() == Role.ADMIN) {
            return true;
        }
        Set<Permission> permissions = access.permissions();
        return access.effectiveRole() == Role.CREW
                && permissions != null
                && (permissions.contains(Permission.CREATIVE_SUBMIT)
                || permissions.contains(Permission.GENERATED_VERSION_MANAGE));
    }

    public boolean canManageApproval(WorkspaceAuthorizationService.WorkspaceAccess access) {
        return access != null
                && access.effectiveRole() == Role.ADMIN
                && !isMaster(access);
    }

    public boolean canCreateApproval(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (canManageApproval(access)) {
            return true;
        }
        Set<Permission> permissions = access == null ? null : access.permissions();
        return access != null
                && access.effectiveRole() == Role.CREW
                && permissions != null
                && (permissions.contains(Permission.CREATIVE_SUBMIT)
                || permissions.contains(Permission.GENERATED_VERSION_MANAGE));
    }

    public boolean canActOnApproval(WorkspaceAuthorizationService.WorkspaceAccess access, ApprovalWorkflow workflow) {
        if (canManageApproval(access)) {
            return true;
        }
        Set<Permission> permissions = access == null ? null : access.permissions();
        CurrentUser currentUser = access == null ? null : access.currentUser();
        return access != null
                && access.effectiveRole() == Role.CREW
                && currentUser != null
                && permissions != null
                && (permissions.contains(Permission.CREATIVE_SUBMIT)
                || permissions.contains(Permission.GENERATED_VERSION_MANAGE))
                && workflow != null
                && currentUser.userId().equals(workflow.getCurrentReviewerId());
    }

    public void requireShareLinkCreation(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (!canCreateShareLink(access)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Share link creation is not permitted for this user");
        }
    }

    public boolean canCreateShareLink(WorkspaceAuthorizationService.WorkspaceAccess access) {
        if (canManageApproval(access)) {
            return true;
        }
        Set<Permission> permissions = access == null ? null : access.permissions();
        return access != null
                && access.effectiveRole() == Role.CREW
                && permissions != null
                && permissions.contains(Permission.CREATIVE_DOWNLOAD);
    }

    private boolean isMaster(WorkspaceAuthorizationService.WorkspaceAccess access) {
        return access.currentUser() != null && access.currentUser().isMaster();
    }

    private UUID workspaceId(WorkspaceAuthorizationService.WorkspaceAccess access) {
        return access == null || access.workspace() == null ? null : access.workspace().getId();
    }
}
