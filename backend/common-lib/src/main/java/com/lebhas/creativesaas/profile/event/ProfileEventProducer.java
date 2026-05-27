package com.lebhas.creativesaas.profile.event;

import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProfileEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventProducer.class);

    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ProfileEventProducer(DomainEventPublisher domainEventPublisher, Clock clock) {
        this.domainEventPublisher = domainEventPublisher;
        this.clock = clock;
    }

    public void profileUpdated(UUID workspaceId, UUID profileId, UUID userId, UUID actorUserId) {
        ProfileUpdatedEventDto event = new ProfileUpdatedEventDto(workspaceId, profileId, userId, actorUserId, now());
        publish(KafkaTopicConstants.PROFILE_UPDATED, profileId, event);
    }

    public void profileImageUploadRequested(
            UUID workspaceId,
            UUID uploadReferenceId,
            UUID userId,
            UUID actorUserId,
            String mimeType,
            long fileSize,
            String extension,
            Instant uploadUrlExpiresAt
    ) {
        ProfileImageUploadRequestedEventDto event = new ProfileImageUploadRequestedEventDto(
                workspaceId,
                uploadReferenceId,
                userId,
                actorUserId,
                mimeType,
                fileSize,
                extension,
                uploadUrlExpiresAt,
                now());
        publish(KafkaTopicConstants.PROFILE_IMAGE_UPLOAD_REQUESTED, uploadReferenceId, event);
    }

    public void profileImageUpdated(UUID workspaceId, UUID profileId, UUID userId, UUID actorUserId) {
        ProfileImageChangedEventDto event = new ProfileImageChangedEventDto(workspaceId, profileId, userId, actorUserId, false, now());
        publish(KafkaTopicConstants.PROFILE_IMAGE_UPDATED, profileId, event);
    }

    public void profileImageRemoved(UUID workspaceId, UUID profileId, UUID userId, UUID actorUserId) {
        ProfileImageChangedEventDto event = new ProfileImageChangedEventDto(workspaceId, profileId, userId, actorUserId, true, now());
        publish(KafkaTopicConstants.PROFILE_IMAGE_REMOVED, profileId, event);
    }

    public void profileSettingsUpdated(UUID workspaceId, UUID settingsId, UUID userId, UUID actorUserId) {
        ProfileSettingsUpdatedEventDto event = new ProfileSettingsUpdatedEventDto(workspaceId, settingsId, userId, actorUserId, now());
        publish(KafkaTopicConstants.PROFILE_SETTINGS_UPDATED, settingsId, event);
    }

    public void profilePasswordChanged(
            UUID workspaceId,
            UUID userId,
            UUID actorUserId,
            boolean otherSessionsRevoked,
            int revokedDeviceCount
    ) {
        ProfilePasswordChangedEventDto event = new ProfilePasswordChangedEventDto(
                workspaceId,
                userId,
                actorUserId,
                otherSessionsRevoked,
                revokedDeviceCount,
                now());
        publish(KafkaTopicConstants.PROFILE_PASSWORD_CHANGED, userId, event);
    }

    public void profileSessionRevoked(
            UUID workspaceId,
            UUID userId,
            UUID actorUserId,
            int revokedTokenCount,
            int revokedDeviceCount,
            boolean currentSessionIncluded
    ) {
        ProfileSessionRevokedEventDto event = new ProfileSessionRevokedEventDto(
                workspaceId,
                userId,
                actorUserId,
                revokedTokenCount,
                revokedDeviceCount,
                currentSessionIncluded,
                now());
        publish(KafkaTopicConstants.PROFILE_SESSION_REVOKED, userId, event);
    }

    public void profileSecurityActivityCreated(
            UUID workspaceId,
            UUID securityActivityId,
            UUID userId,
            UUID actorUserId,
            UserSecurityActivityType activityType,
            boolean success,
            String failureReason
    ) {
        ProfileSecurityActivityCreatedEventDto event = new ProfileSecurityActivityCreatedEventDto(
                workspaceId,
                securityActivityId,
                userId,
                actorUserId,
                activityType,
                success,
                failureReason,
                now());
        publish(KafkaTopicConstants.PROFILE_SECURITY_ACTIVITY_CREATED, securityActivityId, event);
    }

    private void publish(String topic, UUID aggregateId, Record event) {
        Runnable publisher = () -> publishNow(topic, aggregateId, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    private void publishNow(String topic, UUID aggregateId, Record event) {
        try {
            Map<String, Object> attributes = attributes(event);
            Object workspaceValue = attributes.get("workspaceId");
            UUID workspaceId = workspaceValue instanceof UUID id ? id : null;
            domainEventPublisher.publish(topic, new BaseDomainEvent(
                    topic,
                    workspaceId,
                    aggregateId == null ? UUID.randomUUID() : aggregateId,
                    now(),
                    attributes));
        } catch (RuntimeException exception) {
            log.warn("profile_event_publish_failed topic={} reason={}", topic, reason(exception));
        }
    }

    private Map<String, Object> attributes(Record event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (RecordComponent component : event.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(event);
                if (value != null) {
                    attributes.put(component.getName(), value);
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to build profile event attributes", exception);
            }
        }
        return attributes;
    }

    private Instant now() {
        return clock.instant();
    }

    private static String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
