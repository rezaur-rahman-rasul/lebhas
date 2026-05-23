package com.lebhas.notification;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "platform")
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

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant readAt) {
        this.notificationStatus = NotificationStatus.READ;
        this.readAt = readAt == null ? Instant.now() : readAt;
    }
}
