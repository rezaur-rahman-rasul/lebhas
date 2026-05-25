package com.lebhas.notification;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.workspace.application.WorkspacePermissionPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class NotificationRecipientResolver {

    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspacePermissionPolicy workspacePermissionPolicy;
    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationRecipientResolver(
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspacePermissionPolicy workspacePermissionPolicy,
            NotificationPreferenceService notificationPreferenceService
    ) {
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspacePermissionPolicy = workspacePermissionPolicy;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @Transactional(readOnly = true)
    public Set<UUID> resolveRecipients(NotificationRecipientRequest request) {
        UUID workspaceId = require(request.workspaceId(), "workspaceId");
        NotificationType notificationType = require(request.notificationType(), "notificationType");
        Set<UUID> candidateUserIds = request.candidateUserIds() == null
                ? Set.of()
                : request.candidateUserIds();
        Set<Role> allowedRoles = request.allowedRoles() == null ? Set.of() : request.allowedRoles();
        Set<Permission> requiredPermissions = request.requiredPermissions() == null
                ? Set.of()
                : request.requiredPermissions();

        List<WorkspaceMembershipEntity> memberships = candidateUserIds.isEmpty()
                ? workspaceMembershipRepository.findAllByWorkspaceIdAndDeletedFalse(workspaceId)
                : candidateUserIds.stream()
                .map(userId -> workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndDeletedFalse(workspaceId, userId))
                .flatMap(java.util.Optional::stream)
                .toList();

        Set<UUID> recipients = new LinkedHashSet<>();
        for (WorkspaceMembershipEntity membership : memberships) {
            if (!membership.isActive()) {
                continue;
            }
            if (!allowedRoles.isEmpty() && !allowedRoles.contains(membership.getRole())) {
                continue;
            }
            Set<Permission> effectivePermissions = workspacePermissionPolicy.resolveEffectivePermissions(
                    membership.getRole(),
                    membership.getPermissions());
            if (!requiredPermissions.isEmpty() && !effectivePermissions.containsAll(requiredPermissions)) {
                continue;
            }
            if (!notificationPreferenceService.isInAppEnabled(workspaceId, membership.getUserId(), notificationType)) {
                continue;
            }
            recipients.add(membership.getUserId());
        }
        return Set.copyOf(recipients);
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
