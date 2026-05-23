package com.lebhas.creativesaas.identity.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;

import java.util.Set;
import java.util.UUID;

public record InviteCrewCommand(
        UUID workspaceId,
        String email,
        Role role,
        Set<Permission> permissions
) {
}
