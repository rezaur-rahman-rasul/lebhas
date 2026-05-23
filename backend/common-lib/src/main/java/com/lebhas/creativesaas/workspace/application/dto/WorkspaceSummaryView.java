package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceSummaryView(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        WorkspaceStatus status,
        WorkspaceLanguage language,
        String timezone,
        UUID ownerId,
        Role currentUserRole,
        Set<Permission> currentUserPermissions,
        Instant createdAt,
        Instant updatedAt
) {
}
