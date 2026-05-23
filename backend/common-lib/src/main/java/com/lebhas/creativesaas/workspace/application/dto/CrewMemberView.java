package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CrewMemberView(
        UUID userId,
        UUID workspaceId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        WorkspaceMembershipStatus status,
        Set<Permission> permissions,
        Instant joinedAt,
        UUID invitedByUserId,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
}
