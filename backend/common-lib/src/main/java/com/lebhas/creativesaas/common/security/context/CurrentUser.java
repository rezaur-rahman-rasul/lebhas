package com.lebhas.creativesaas.common.security.context;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UUID workspaceId,
        String deviceId,
        String email,
        Set<Role> roles,
        Set<Permission> permissions,
        String tokenId,
        Instant accessTokenExpiresAt
) {

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean isMaster() {
        return hasRole(Role.MASTER);
    }
}
