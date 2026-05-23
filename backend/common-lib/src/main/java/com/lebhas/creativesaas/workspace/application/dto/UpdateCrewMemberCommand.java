package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;

import java.util.Set;
import java.util.UUID;

public record UpdateCrewMemberCommand(
        UUID workspaceId,
        UUID crewUserId,
        Set<Permission> permissions,
        WorkspaceMembershipStatus status
) {
}
