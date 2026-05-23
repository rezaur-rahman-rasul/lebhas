package com.lebhas.creativesaas.identity.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.InvitationStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record InvitationView(
        String invitationToken,
        UUID workspaceId,
        String email,
        Role role,
        Set<Permission> permissions,
        Instant expiresAt,
        InvitationStatus status
) {
}
