package com.lebhas.notification;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends TenantAwareRepository<NotificationPreference> {

    List<NotificationPreference> findAllByWorkspaceIdAndUserIdAndDeletedFalse(UUID workspaceId, UUID userId);

    Optional<NotificationPreference> findByWorkspaceIdAndUserIdAndNotificationTypeAndDeletedFalse(
            UUID workspaceId,
            UUID userId,
            String notificationType);
}
