package com.lebhas.creativesaas.identity.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserView(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean isMaster,
        UserStatus status,
        boolean emailVerified,
        Instant lastLoginAt,
        UUID workspaceId,
        Instant createdAt,
        Instant updatedAt,
        Set<Permission> permissions
) {
}
