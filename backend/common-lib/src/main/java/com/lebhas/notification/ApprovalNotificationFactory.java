package com.lebhas.notification;

import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ApprovalNotificationFactory {

    private static final String APPROVAL_REFERENCE_TYPE = "APPROVAL_REQUEST";

    public Optional<NotificationCreateRequest> create(String topic, ApprovalLifecycleEvent event) {
        NotificationType notificationType = resolveType(topic);
        if (notificationType == null) {
            return Optional.empty();
        }

        UUID recipientUserId = resolveRecipientUserId(topic, event);
        if (recipientUserId == null) {
            return Optional.empty();
        }

        return Optional.of(new NotificationCreateRequest(
                event.workspaceId(),
                recipientUserId,
                event.actorId(),
                notificationType,
                titleFor(notificationType),
                messageFor(notificationType, event),
                APPROVAL_REFERENCE_TYPE,
                event.approvalRequestId(),
                event.eventId()));
    }

    private NotificationType resolveType(String topic) {
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_ASSIGNED)) {
            return NotificationType.APPROVAL_ASSIGNED;
        }
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_REQUEST_SUBMITTED)) {
            return NotificationType.APPROVAL_SUBMITTED;
        }
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_APPROVED)) {
            return NotificationType.APPROVAL_APPROVED;
        }
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_REJECTED)) {
            return NotificationType.APPROVAL_REJECTED;
        }
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_CHANGES_REQUESTED)) {
            return NotificationType.APPROVAL_CHANGES_REQUESTED;
        }
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_RESUBMITTED)) {
            return NotificationType.APPROVAL_RESUBMITTED;
        }
        return null;
    }

    private UUID resolveRecipientUserId(String topic, ApprovalLifecycleEvent event) {
        if (topic.endsWith(KafkaTopicConstants.APPROVAL_ASSIGNED)
                || topic.endsWith(KafkaTopicConstants.APPROVAL_REQUEST_SUBMITTED)
                || topic.endsWith(KafkaTopicConstants.APPROVAL_RESUBMITTED)) {
            return event.assignedReviewerId();
        }
        return event.submittedBy();
    }

    private String titleFor(NotificationType notificationType) {
        return switch (notificationType) {
            case APPROVAL_ASSIGNED -> "Approval assigned";
            case APPROVAL_SUBMITTED -> "Approval submitted";
            case APPROVAL_APPROVED -> "Approval approved";
            case APPROVAL_REJECTED -> "Approval rejected";
            case APPROVAL_CHANGES_REQUESTED -> "Changes requested";
            case APPROVAL_RESUBMITTED -> "Approval resubmitted";
            default -> "Notification";
        };
    }

    private String messageFor(NotificationType notificationType, ApprovalLifecycleEvent event) {
        if (event.details() != null) {
            return event.details();
        }
        return switch (notificationType) {
            case APPROVAL_ASSIGNED -> "You have been assigned an approval request.";
            case APPROVAL_SUBMITTED -> "A creative approval is ready for review.";
            case APPROVAL_APPROVED -> "Your approval request has been approved.";
            case APPROVAL_REJECTED -> "Your approval request has been rejected.";
            case APPROVAL_CHANGES_REQUESTED -> "Changes were requested for your approval request.";
            case APPROVAL_RESUBMITTED -> "A creative approval has been resubmitted for review.";
            default -> "You have a new notification.";
        };
    }
}
