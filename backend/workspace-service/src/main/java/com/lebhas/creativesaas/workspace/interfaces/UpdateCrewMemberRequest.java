package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateCrewMemberRequest(
        @NotNull(message = ValidationMessages.REQUIRED)
        Set<Permission> permissions,
        @NotNull(message = ValidationMessages.REQUIRED)
        WorkspaceMembershipStatus status
) {
}
