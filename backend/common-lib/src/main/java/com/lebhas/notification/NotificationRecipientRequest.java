package com.lebhas.notification;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;

import java.util.Set;
import java.util.UUID;

public record NotificationRecipientRequest(
        UUID workspaceId,
        Set<UUID> candidateUserIds,
        Set<Role> allowedRoles,
        Set<Permission> requiredPermissions,
        NotificationType notificationType
) {
}
