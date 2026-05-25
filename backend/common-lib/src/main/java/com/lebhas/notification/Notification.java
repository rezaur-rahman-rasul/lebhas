package com.lebhas.notification;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_source_event_id", columnNames = "source_event_id"),
        indexes = {
                @Index(name = "idx_notifications_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_notifications_recipient_status_created_at", columnList = "recipient_user_id,notification_status,created_at"),
                @Index(name = "idx_notifications_reference", columnList = "reference_type,reference_id")
        })
public class Notification extends TenantAwareEntity {

    @Column(name = "source_event_id", nullable = false, updatable = false, length = 120)
    private String sourceEventId;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private UUID recipientUserId;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, updatable = false, length = 80)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "reference_type", nullable = false, updatable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 40)
    private NotificationStatus notificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 40)
    private NotificationChannel notificationChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_priority", nullable = false, length = 30)
    private NotificationPriority notificationPriority;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public static Notification create(NotificationCreateRequest request) {
        Notification notification = new Notification();
        notification.assignWorkspace(request.workspaceId());
        notification.sourceEventId = request.sourceEventId();
        notification.recipientUserId = request.recipientUserId();
        notification.actorUserId = request.actorUserId();
        notification.notificationType = request.notificationType();
        notification.title = request.title();
        notification.message = request.message();
        notification.referenceType = request.referenceType();
        notification.referenceId = request.referenceId();
        notification.notificationStatus = NotificationStatus.UNREAD;
        notification.notificationChannel = NotificationChannel.IN_APP;
        notification.notificationPriority = NotificationPriority.NORMAL;
        notification.readAt = null;
        return notification;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public NotificationChannel getNotificationChannel() {
        return notificationChannel;
    }

    public NotificationPriority getNotificationPriority() {
        return notificationPriority;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant readAt) {
        this.notificationStatus = NotificationStatus.READ;
        this.readAt = readAt == null ? Instant.now() : readAt;
    }
}
