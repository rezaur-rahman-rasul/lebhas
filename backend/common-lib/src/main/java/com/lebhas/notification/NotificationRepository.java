package com.lebhas.notification;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends TenantAwareRepository<Notification> {

    boolean existsBySourceEventIdAndDeletedFalse(String sourceEventId);

    List<Notification> findAllByWorkspaceIdAndRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId, UUID recipientUserId);

    Optional<Notification> findByIdAndWorkspaceIdAndRecipientUserIdAndDeletedFalse(
            UUID id,
            UUID workspaceId,
            UUID recipientUserId);

    List<Notification> findAllByWorkspaceIdAndRecipientUserIdAndNotificationStatusAndDeletedFalse(
            UUID workspaceId,
            UUID recipientUserId,
            NotificationStatus notificationStatus);

    long countByWorkspaceIdAndRecipientUserIdAndNotificationStatusAndDeletedFalse(
            UUID workspaceId,
            UUID recipientUserId,
            NotificationStatus notificationStatus);
}
