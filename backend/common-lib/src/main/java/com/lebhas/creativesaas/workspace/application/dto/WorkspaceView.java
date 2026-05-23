package com.lebhas.creativesaas.workspace.application.dto;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record WorkspaceView(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String description,
        String industry,
        String timezone,
        WorkspaceLanguage language,
        String currency,
        String country,
        WorkspaceStatus status,
        UUID ownerId,
        Role currentUserRole,
        Set<Permission> currentUserPermissions,
        Instant createdAt,
        Instant updatedAt
) {
}
