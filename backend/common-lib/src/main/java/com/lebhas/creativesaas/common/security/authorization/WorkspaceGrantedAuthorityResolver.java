package com.lebhas.creativesaas.common.security.authorization;

import com.lebhas.creativesaas.common.security.AuthenticatedPrincipal;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.redis.RedisPermissionCache;
import com.lebhas.creativesaas.redis.RedisPermissionVersionService;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class WorkspaceGrantedAuthorityResolver {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final RolePermissionRegistry rolePermissionRegistry;
    private final RedisPermissionCache redisPermissionCache;
    private final RedisPermissionVersionService redisPermissionVersionService;

    public WorkspaceGrantedAuthorityResolver(
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            RolePermissionRegistry rolePermissionRegistry,
            RedisPermissionCache redisPermissionCache,
            RedisPermissionVersionService redisPermissionVersionService
    ) {
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.rolePermissionRegistry = rolePermissionRegistry;
        this.redisPermissionCache = redisPermissionCache;
        this.redisPermissionVersionService = redisPermissionVersionService;
    }

    public Set<Permission> resolve(AuthenticatedPrincipal principal) {
        Role effectiveRole = principal.roles().stream().findFirst().orElse(null);
        if (effectiveRole == null) {
            return Set.of();
        }
        if (effectiveRole.isMaster() || principal.workspaceId() == null) {
            return rolePermissionRegistry.resolve(effectiveRole);
        }
        long permissionVersion = redisPermissionVersionService.getVersion(principal.workspaceId());
        RedisPermissionCache.PermissionSnapshot snapshot = redisPermissionCache.getOrLoad(
                principal.workspaceId(),
                principal.userId(),
                permissionVersion,
                () -> workspaceMembershipRepository.findByUserIdAndWorkspaceIdAndDeletedFalse(principal.userId(), principal.workspaceId())
                        .filter(WorkspaceMembershipEntity::isActive)
                        .map(membership -> new RedisPermissionCache.PermissionSnapshot(
                                principal.workspaceId(),
                                principal.userId(),
                                workspacePermissionPolicy.resolveEffectivePermissions(membership.getRole(), membership.getPermissions()).stream()
                                        .map(Enum::name)
                                        .collect(java.util.stream.Collectors.toSet()),
                                permissionVersion,
                                java.time.Instant.now()))
                        .orElse(new RedisPermissionCache.PermissionSnapshot(
                                principal.workspaceId(),
                                principal.userId(),
                                Set.of(),
                                permissionVersion,
                                java.time.Instant.now())));
        return snapshot.permissions().stream().map(Permission::valueOf).collect(java.util.stream.Collectors.toSet());
    }

    public Set<Permission> resolve(Role role, UUID workspaceId, Set<Permission> storedPermissions) {
        if (role.isMaster() || workspaceId == null) {
            return rolePermissionRegistry.resolve(role);
        }
        return workspacePermissionPolicy.resolveEffectivePermissions(role, storedPermissions);
    }

    public boolean hasActiveMembership(UUID userId, UUID workspaceId) {
        return workspaceMembershipRepository.existsByUserIdAndWorkspaceIdAndStatusAndDeletedFalse(
                userId,
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE);
    }
}
