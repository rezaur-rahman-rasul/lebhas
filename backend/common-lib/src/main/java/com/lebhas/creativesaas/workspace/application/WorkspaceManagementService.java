package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.workspace.application.dto.CreateWorkspaceCommand;
import com.lebhas.creativesaas.workspace.application.dto.UpdateWorkspaceCommand;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceSummaryView;
import com.lebhas.creativesaas.workspace.application.dto.WorkspaceView;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkspaceManagementService {

    private final CurrentUserContext currentUserContext;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceProvisioningService workspaceProvisioningService;
    private final WorkspaceSlugService workspaceSlugService;
    private final WorkspaceViewMapper workspaceViewMapper;
    private final WorkspaceActivityLogger workspaceActivityLogger;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final RolePermissionRegistry rolePermissionRegistry;

    public WorkspaceManagementService(
            CurrentUserContext currentUserContext,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspaceProvisioningService workspaceProvisioningService,
            WorkspaceSlugService workspaceSlugService,
            WorkspaceViewMapper workspaceViewMapper,
            WorkspaceActivityLogger workspaceActivityLogger,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            RolePermissionRegistry rolePermissionRegistry
    ) {
        this.currentUserContext = currentUserContext;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspaceProvisioningService = workspaceProvisioningService;
        this.workspaceSlugService = workspaceSlugService;
        this.workspaceViewMapper = workspaceViewMapper;
        this.workspaceActivityLogger = workspaceActivityLogger;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.rolePermissionRegistry = rolePermissionRegistry;
    }

    @Transactional
    public WorkspaceView createWorkspace(CreateWorkspaceCommand command) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        WorkspaceProvisioningService.ProvisionedWorkspace provisionedWorkspace = workspaceProvisioningService.provisionOwnedWorkspace(
                currentUser.userId(),
                new WorkspaceProvisioningService.WorkspaceSeed(
                        command.name(),
                        command.slug(),
                        command.logoUrl(),
                        command.description(),
                        command.industry(),
                        command.timezone(),
                        command.language(),
                        command.currency(),
                        command.country()));
        workspaceActivityLogger.logWorkspaceCreated(
                provisionedWorkspace.workspace().getId(),
                currentUser.userId(),
                provisionedWorkspace.workspace().getSlug());
        Role currentRole = currentUser.isMaster() ? Role.MASTER : Role.ADMIN;
        Set<Permission> permissions = currentUser.isMaster()
                ? rolePermissionRegistry.resolve(Role.MASTER)
                : rolePermissionRegistry.resolve(Role.ADMIN);
        return workspaceViewMapper.toWorkspaceView(provisionedWorkspace.workspace(), currentRole, permissions);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceSummaryView> listAccessibleWorkspaces() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (currentUser.isMaster()) {
            Set<Permission> permissions = rolePermissionRegistry.resolve(Role.MASTER);
            return workspaceRepository.findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                    .map(workspace -> workspaceViewMapper.toWorkspaceSummaryView(workspace, Role.MASTER, permissions))
                    .toList();
        }

        List<WorkspaceMembershipEntity> memberships = workspaceMembershipRepository
                .findAllByUserIdAndStatusAndDeletedFalse(currentUser.userId(), WorkspaceMembershipStatus.ACTIVE);
        Map<UUID, WorkspaceMembershipEntity> membershipByWorkspaceId = memberships.stream()
                .collect(Collectors.toMap(WorkspaceMembershipEntity::getWorkspaceId, Function.identity(), (left, right) -> left));
        Map<UUID, WorkspaceEntity> workspacesById = workspaceRepository.findAllById(membershipByWorkspaceId.keySet()).stream()
                .filter(workspace -> !workspace.isDeleted())
                .collect(Collectors.toMap(WorkspaceEntity::getId, Function.identity()));

        return membershipByWorkspaceId.values().stream()
                .map(membership -> toSummaryView(workspacesById.get(membership.getWorkspaceId()), membership))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(WorkspaceSummaryView::updatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceView getWorkspace(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return workspaceViewMapper.toWorkspaceView(access.workspace(), access.effectiveRole(), access.permissions());
    }

    @Transactional
    public WorkspaceView updateWorkspace(UpdateWorkspaceCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceOwnerOrMaster(command.workspaceId());
        WorkspaceEntity workspace = access.workspace();
        String resolvedSlug = workspaceSlugService.resolveUpdateSlug(command.slug(), command.name(), workspace.getId());
        workspace.update(
                command.name(),
                resolvedSlug,
                command.logoUrl(),
                command.description(),
                command.industry(),
                command.timezone(),
                command.language(),
                command.currency(),
                command.country());
        if (command.status() != null) {
            workspace.changeStatus(command.status());
        }
        workspaceRepository.save(workspace);
        workspaceActivityLogger.logWorkspaceUpdated(
                workspace.getId(),
                access.currentUser().userId(),
                workspace.getSlug(),
                workspace.getStatus().name());
        return workspaceViewMapper.toWorkspaceView(workspace, access.effectiveRole(), access.permissions());
    }

    @Transactional(readOnly = true)
    public WorkspaceEntity requireWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    private WorkspaceSummaryView toSummaryView(WorkspaceEntity workspace, WorkspaceMembershipEntity membership) {
        if (workspace == null) {
            return null;
        }
        Set<Permission> permissions = workspacePermissionPolicy.resolveEffectivePermissions(membership.getRole(), membership.getPermissions());
        return workspaceViewMapper.toWorkspaceSummaryView(workspace, membership.getRole(), permissions);
    }
}
