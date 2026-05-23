package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.authorization.RolePermissionRegistry;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.common.tenant.TenantContext;
import com.lebhas.creativesaas.identity.application.dto.UpdateUserCommand;
import com.lebhas.creativesaas.identity.application.dto.UpdateUserStatusCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.UserRepository;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final CurrentUserContext currentUserContext;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final IdentityViewMapper identityViewMapper;
    private final RolePermissionRegistry rolePermissionRegistry;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;

    public UserManagementService(
            UserRepository userRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            CurrentUserContext currentUserContext,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            IdentityViewMapper identityViewMapper,
            RolePermissionRegistry rolePermissionRegistry,
            WorkspacePermissionPolicy workspacePermissionPolicy
    ) {
        this.userRepository = userRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.currentUserContext = currentUserContext;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.identityViewMapper = identityViewMapper;
        this.rolePermissionRegistry = rolePermissionRegistry;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
    }

    @Transactional(readOnly = true)
    public List<UserView> listUsers(UserStatus status) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (currentUser.isMaster() && TenantContext.getWorkspaceId().isEmpty()) {
            return userRepository.findAll().stream()
                    .filter(user -> !user.isDeleted())
                    .filter(user -> status == null || user.getStatus() == status)
                    .map(user -> identityViewMapper.toUserView(user, null, user.getRole(), rolePermissionRegistry.resolve(Set.of(user.getRole()))))
                    .toList();
        }
        UUID workspaceId = workspaceAuthorizationService.requireWorkspaceAccess(TenantContext.getWorkspaceId().orElse(null));
        Map<UUID, WorkspaceMembershipEntity> membershipsByUserId = workspaceMembershipRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId).stream()
                .filter(WorkspaceMembershipEntity::isActive)
                .collect(Collectors.toMap(WorkspaceMembershipEntity::getUserId, Function.identity(), (left, right) -> left));
        return userRepository.findWorkspaceUsers(workspaceId, status).stream()
                .map(user -> toUserView(user, workspaceId, membershipsByUserId.get(user.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserView getUser(UUID userId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (currentUser.isMaster() && TenantContext.getWorkspaceId().isEmpty()) {
            UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            return identityViewMapper.toUserView(user, null, user.getRole(), rolePermissionRegistry.resolve(Set.of(user.getRole())));
        }
        UUID workspaceId = workspaceAuthorizationService.requireWorkspaceAccess(TenantContext.getWorkspaceId().orElse(null));
        UserEntity user = userRepository.findWorkspaceUserById(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        WorkspaceMembershipEntity membership = workspaceAuthorizationService.requireActiveMembership(user.getId(), workspaceId);
        return toUserView(user, workspaceId, membership);
    }

    @Transactional
    public UserView updateUser(UpdateUserCommand command) {
        UserEntity user = loadEditableUser(command.userId());
        ensureUniqueEmail(user, command.email());
        Role newRole = command.role() == null ? user.getRole() : command.role();
        guardRoleMutation(newRole, user);
        user.updateProfile(command.firstName(), command.lastName(), command.email(), command.phone());
        UUID workspaceId = TenantContext.getWorkspaceId().orElse(null);
        if (workspaceId != null && !newRole.isMaster()) {
            WorkspaceMembershipEntity membership = workspaceAuthorizationService.requireActiveMembership(user.getId(), workspaceId);
            membership.assignRole(newRole);
            if (newRole == Role.ADMIN) {
                membership.replacePermissions(Set.of());
            } else {
                membership.replacePermissions(workspacePermissionPolicy.normalizeCrewPermissions(membership.getPermissions()));
            }
            workspaceMembershipRepository.save(membership);
            user.assignRole(resolveHighestWorkspaceRole(user.getId(), newRole));
        } else {
            user.assignRole(newRole);
        }
        userRepository.save(user);
        return buildUserView(user, workspaceId);
    }

    @Transactional
    public UserView changeStatus(UpdateUserStatusCommand command) {
        UserEntity user = loadEditableUser(command.userId());
        guardMasterMutation(user);
        user.changeStatus(command.status());
        userRepository.save(user);
        UUID workspaceId = TenantContext.getWorkspaceId().orElse(null);
        if (workspaceId != null && !user.getRole().isMaster()) {
            workspaceMembershipRepository.findByUserIdAndWorkspaceIdAndDeletedFalse(user.getId(), workspaceId)
                    .ifPresent(membership -> {
                        membership.changeStatus(command.status() == UserStatus.ACTIVE
                                ? WorkspaceMembershipStatus.ACTIVE
                                : WorkspaceMembershipStatus.SUSPENDED);
                        workspaceMembershipRepository.save(membership);
                    });
        }
        return buildUserView(user, workspaceId);
    }

    private UserEntity loadEditableUser(UUID userId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (currentUser.isMaster() && TenantContext.getWorkspaceId().isEmpty()) {
            return userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        UUID workspaceId = workspaceAuthorizationService.requireWorkspaceAccess(TenantContext.getWorkspaceId().orElse(null));
        UserEntity user = userRepository.findWorkspaceUserById(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        guardMasterMutation(user);
        return user;
    }

    private void ensureUniqueEmail(UserEntity existingUser, String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalized)
                .filter(user -> !user.getId().equals(existingUser.getId()))
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
                });
    }

    private void guardRoleMutation(Role targetRole, UserEntity targetUser) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (targetRole == Role.MASTER && !currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only master users can assign the master role");
        }
        if (targetUser.getRole().isMaster() && !currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Master users can only be managed by another master user");
        }
    }

    private void guardMasterMutation(UserEntity targetUser) {
        if (targetUser.getRole().isMaster() && !currentUserContext.requireCurrentUser().isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Master users can only be managed by another master user");
        }
    }

    private UserView buildUserView(UserEntity user, UUID workspaceId) {
        if (workspaceId == null) {
            return identityViewMapper.toUserView(user, null, user.getRole(), rolePermissionRegistry.resolve(Set.of(user.getRole())));
        }
        WorkspaceMembershipEntity membership = workspaceAuthorizationService.requireActiveMembership(user.getId(), workspaceId);
        return toUserView(user, workspaceId, membership);
    }

    private UserView toUserView(UserEntity user, UUID workspaceId, WorkspaceMembershipEntity membership) {
        if (membership == null) {
            return identityViewMapper.toUserView(user, workspaceId, user.getRole(), rolePermissionRegistry.resolve(Set.of(user.getRole())));
        }
        Set<Permission> permissions = workspacePermissionPolicy.resolveEffectivePermissions(membership.getRole(), membership.getPermissions());
        return identityViewMapper.toUserView(user, workspaceId, membership.getRole(), permissions);
    }

    private Role resolveHighestWorkspaceRole(UUID userId, Role fallbackRole) {
        return workspaceMembershipRepository.findAllByUserIdAndStatusAndDeletedFalse(userId, WorkspaceMembershipStatus.ACTIVE).stream()
                .map(WorkspaceMembershipEntity::getRole)
                .reduce((left, right) -> left == Role.ADMIN || right == Role.ADMIN ? Role.ADMIN : Role.CREW)
                .orElse(fallbackRole);
    }
}
