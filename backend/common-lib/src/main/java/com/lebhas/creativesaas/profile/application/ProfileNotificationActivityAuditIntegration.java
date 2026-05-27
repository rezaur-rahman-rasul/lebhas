package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.activity.application.ActivityFeedCommand;
import com.lebhas.creativesaas.activity.application.ActivityFeedService;
import com.lebhas.creativesaas.activity.domain.ActivityCategory;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.profile.domain.UserSecurityActivityType;
import com.lebhas.creativesaas.profile.event.ProfileEventConsumerHooks;
import com.lebhas.creativesaas.profile.event.ProfileImageChangedEventDto;
import com.lebhas.creativesaas.profile.event.ProfileImageUploadRequestedEventDto;
import com.lebhas.creativesaas.profile.event.ProfilePasswordChangedEventDto;
import com.lebhas.creativesaas.profile.event.ProfileSecurityActivityCreatedEventDto;
import com.lebhas.creativesaas.profile.event.ProfileSessionRevokedEventDto;
import com.lebhas.creativesaas.profile.event.ProfileSettingsUpdatedEventDto;
import com.lebhas.creativesaas.profile.event.ProfileUpdatedEventDto;
import com.lebhas.notification.NotificationCreateRequest;
import com.lebhas.notification.NotificationService;
import com.lebhas.notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ProfileNotificationActivityAuditIntegration implements ProfileEventConsumerHooks {

    private static final Logger log = LoggerFactory.getLogger(ProfileNotificationActivityAuditIntegration.class);
    private static final String USER_PROFILE = "USER_PROFILE";
    private static final String USER_ACCOUNT_SETTINGS = "USER_ACCOUNT_SETTINGS";
    private static final String USER_PROFILE_IMAGE = "USER_PROFILE_IMAGE";
    private static final String USER_PASSWORD = "USER_PASSWORD";
    private static final String USER_SESSION = "USER_SESSION";
    private static final String USER_SECURITY_ACTIVITY = "USER_SECURITY_ACTIVITY";

    private NotificationService notificationService;
    private ActivityFeedService activityFeedService;
    private AuditLogService auditLogService;
    private UserSecurityActivityService userSecurityActivityService;

    @Autowired(required = false)
    void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Autowired(required = false)
    void setActivityFeedService(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @Autowired(required = false)
    void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Autowired(required = false)
    void setUserSecurityActivityService(UserSecurityActivityService userSecurityActivityService) {
        this.userSecurityActivityService = userSecurityActivityService;
    }

    public void profileUpdated(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (currentUser == null) {
            return;
        }
        UUID workspaceId = currentUser.workspaceId();
        UUID actorUserId = currentUser.userId();
        notification(workspaceId, actorUserId, actorUserId, NotificationType.PROFILE_UPDATED,
                "Profile updated", "Your profile was updated.", USER_PROFILE, profileId,
                source("profile.updated.notification", profileId));
        activity(workspaceId, actorUserId, ActivityFeedService.TYPE_PROFILE_UPDATED,
                "Profile updated", "A user profile was updated.", USER_PROFILE, profileId,
                source("profile.updated.activity", profileId));
        audit(workspaceId, actorUserId, AuditActionType.PROFILE_UPDATED, AuditOutcome.SUCCESS,
                USER_PROFILE, profileId, "User profile updated",
                details("userId", actorUserId, "profileId", profileId), ipAddress, userAgent,
                source("profile.updated.audit", profileId));
        securityActivity(actorUserId, UserSecurityActivityType.PROFILE_UPDATED, ipAddress, userAgent, true, null);
    }

    public void settingsUpdated(CurrentUser currentUser, UUID settingsId, String ipAddress, String userAgent) {
        if (currentUser == null) {
            return;
        }
        UUID actorUserId = currentUser.userId();
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.SETTINGS_UPDATED, AuditOutcome.SUCCESS,
                USER_ACCOUNT_SETTINGS, settingsId, "User account settings updated",
                details("userId", actorUserId, "settingsId", settingsId), ipAddress, userAgent,
                source("profile.settings.updated.audit", settingsId));
        securityActivity(actorUserId, UserSecurityActivityType.ACCOUNT_SETTINGS_UPDATED, ipAddress, userAgent, true, null);
    }

    public void profileImageUploadRequested(
            CurrentUser currentUser,
            UUID uploadReferenceId,
            String mimeType,
            long fileSize,
            String extension
    ) {
        if (currentUser == null) {
            return;
        }
        audit(currentUser.workspaceId(), currentUser.userId(), AuditActionType.PROFILE_IMAGE_UPLOAD_REQUESTED, AuditOutcome.SUCCESS,
                USER_PROFILE_IMAGE, uploadReferenceId, "Profile image upload requested",
                details("userId", currentUser.userId(), "mimeType", mimeType, "fileSize", fileSize, "extension", extension),
                null, null, source("profile.image.upload.requested.audit", uploadReferenceId));
    }

    public void profileImageUpdated(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (currentUser == null) {
            return;
        }
        UUID actorUserId = currentUser.userId();
        notification(currentUser.workspaceId(), actorUserId, actorUserId, NotificationType.PROFILE_IMAGE_UPDATED,
                "Profile image updated", "Your profile image was updated.", USER_PROFILE_IMAGE, profileId,
                source("profile.image.updated.notification", profileId));
        activity(currentUser.workspaceId(), actorUserId, ActivityFeedService.TYPE_PROFILE_IMAGE_UPDATED,
                "Profile image updated", "A user profile image was updated.", USER_PROFILE_IMAGE, profileId,
                source("profile.image.updated.activity", profileId));
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.PROFILE_IMAGE_UPDATED, AuditOutcome.SUCCESS,
                USER_PROFILE_IMAGE, profileId, "Profile image updated",
                details("userId", actorUserId, "profileId", profileId), ipAddress, userAgent,
                source("profile.image.updated.audit", profileId));
        securityActivity(actorUserId, UserSecurityActivityType.PROFILE_IMAGE_UPDATED, ipAddress, userAgent, true, null);
    }

    public void profileImageRemoved(CurrentUser currentUser, UUID profileId, String ipAddress, String userAgent) {
        if (currentUser == null) {
            return;
        }
        UUID actorUserId = currentUser.userId();
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.PROFILE_IMAGE_REMOVED, AuditOutcome.SUCCESS,
                USER_PROFILE_IMAGE, profileId, "Profile image removed",
                details("userId", actorUserId, "profileId", profileId), ipAddress, userAgent,
                source("profile.image.removed.audit", profileId));
        securityActivity(actorUserId, UserSecurityActivityType.PROFILE_IMAGE_REMOVED, ipAddress, userAgent, true, null);
    }

    public void passwordChanged(
            CurrentUser currentUser,
            SessionRevocationService.RevocationResult revocationResult,
            String ipAddress,
            String userAgent
    ) {
        if (currentUser == null) {
            return;
        }
        int revokedDeviceCount = revocationResult == null ? 0 : revocationResult.revokedDeviceIds().size();
        boolean otherSessionsRevoked = revokedDeviceCount > 0;
        UUID actorUserId = currentUser.userId();
        notification(currentUser.workspaceId(), actorUserId, actorUserId, NotificationType.PASSWORD_CHANGED,
                "Password changed", "Your password was changed.", USER_PASSWORD, actorUserId,
                source("profile.password.changed.notification", actorUserId));
        activity(currentUser.workspaceId(), actorUserId, ActivityFeedService.TYPE_PASSWORD_CHANGED,
                "Password changed", "A user password was changed.", USER_PASSWORD, actorUserId,
                source("profile.password.changed.activity", actorUserId));
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.PASSWORD_CHANGED, AuditOutcome.SUCCESS,
                USER_PASSWORD, actorUserId, "User password changed",
                details("userId", actorUserId, "otherSessionsRevoked", otherSessionsRevoked, "revokedDeviceCount", revokedDeviceCount),
                ipAddress, userAgent, source("profile.password.changed.audit", actorUserId));
    }

    public void passwordChangeFailed(CurrentUser currentUser, String failureReason, String ipAddress, String userAgent) {
        if (currentUser == null) {
            return;
        }
        String sanitizedReason = sanitizeFailureReason(failureReason);
        UUID actorUserId = currentUser.userId();
        notification(currentUser.workspaceId(), actorUserId, actorUserId, NotificationType.SECURITY_ACTIVITY_DETECTED,
                "Security activity detected", "A password change attempt was blocked.", USER_SECURITY_ACTIVITY, actorUserId,
                source("profile.password.change.failed.notification", actorUserId));
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.PASSWORD_CHANGE_FAILED, AuditOutcome.FAILURE,
                USER_PASSWORD, actorUserId, "Password change failed",
                details("userId", actorUserId, "failureReason", sanitizedReason), ipAddress, userAgent,
                source("profile.password.change.failed.audit", actorUserId));
    }

    public void sessionRevoked(CurrentUser currentUser, SessionRevocationService.RevocationResult result, boolean currentSessionIncluded) {
        if (currentUser == null || result == null || (result.revokedTokenCount() <= 0 && result.revokedDeviceIds().isEmpty())) {
            return;
        }
        UUID actorUserId = currentUser.userId();
        notification(currentUser.workspaceId(), actorUserId, actorUserId, NotificationType.SESSION_REVOKED,
                "Session revoked", "One or more sessions were revoked.", USER_SESSION, actorUserId,
                source("profile.session.revoked.notification", actorUserId));
        audit(currentUser.workspaceId(), actorUserId, AuditActionType.SESSION_REVOKED, AuditOutcome.SUCCESS,
                USER_SESSION, actorUserId, "User sessions revoked",
                details(
                        "userId", actorUserId,
                        "revokedTokenCount", result.revokedTokenCount(),
                        "revokedDeviceCount", result.revokedDeviceIds().size(),
                        "currentSessionIncluded", currentSessionIncluded),
                null, null, source("profile.session.revoked.audit", actorUserId));
        securityActivity(actorUserId,
                currentSessionIncluded ? UserSecurityActivityType.SESSION_REVOKED : UserSecurityActivityType.SESSIONS_REVOKED,
                null, null, true, null);
    }

    @Override
    public void onProfileUpdated(ProfileUpdatedEventDto event) {
        if (event == null) {
            return;
        }
        notification(event.workspaceId(), event.userId(), actor(event.actorUserId(), event.userId()), NotificationType.PROFILE_UPDATED,
                "Profile updated", "Your profile was updated.", USER_PROFILE, event.profileId(),
                eventSource("profile.updated.notification", event.profileId(), event.occurredAt()));
        activity(event.workspaceId(), actor(event.actorUserId(), event.userId()), ActivityFeedService.TYPE_PROFILE_UPDATED,
                "Profile updated", "A user profile was updated.", USER_PROFILE, event.profileId(),
                eventSource("profile.updated.activity", event.profileId(), event.occurredAt()));
        audit(event.workspaceId(), actor(event.actorUserId(), event.userId()), AuditActionType.PROFILE_UPDATED, AuditOutcome.SUCCESS,
                USER_PROFILE, event.profileId(), "User profile updated",
                details("userId", event.userId(), "profileId", event.profileId()), null, null,
                eventSource("profile.updated.audit", event.profileId(), event.occurredAt()));
    }

    @Override
    public void onProfileImageUploadRequested(ProfileImageUploadRequestedEventDto event) {
        if (event == null) {
            return;
        }
        audit(event.workspaceId(), actor(event.actorUserId(), event.userId()), AuditActionType.PROFILE_IMAGE_UPLOAD_REQUESTED,
                AuditOutcome.SUCCESS, USER_PROFILE_IMAGE, event.uploadReferenceId(), "Profile image upload requested",
                details("userId", event.userId(), "mimeType", event.mimeType(), "fileSize", event.fileSize(), "extension", event.extension()),
                null, null, eventSource("profile.image.upload.requested.audit", event.uploadReferenceId(), event.occurredAt()));
    }

    @Override
    public void onProfileImageUpdated(ProfileImageChangedEventDto event) {
        if (event == null) {
            return;
        }
        notification(event.workspaceId(), event.userId(), actor(event.actorUserId(), event.userId()), NotificationType.PROFILE_IMAGE_UPDATED,
                "Profile image updated", "Your profile image was updated.", USER_PROFILE_IMAGE, event.profileId(),
                eventSource("profile.image.updated.notification", event.profileId(), event.occurredAt()));
        activity(event.workspaceId(), actor(event.actorUserId(), event.userId()), ActivityFeedService.TYPE_PROFILE_IMAGE_UPDATED,
                "Profile image updated", "A user profile image was updated.", USER_PROFILE_IMAGE, event.profileId(),
                eventSource("profile.image.updated.activity", event.profileId(), event.occurredAt()));
        audit(event.workspaceId(), actor(event.actorUserId(), event.userId()), AuditActionType.PROFILE_IMAGE_UPDATED, AuditOutcome.SUCCESS,
                USER_PROFILE_IMAGE, event.profileId(), "Profile image updated",
                details("userId", event.userId(), "profileId", event.profileId()), null, null,
                eventSource("profile.image.updated.audit", event.profileId(), event.occurredAt()));
    }

    @Override
    public void onProfileImageRemoved(ProfileImageChangedEventDto event) {
        if (event == null) {
            return;
        }
        audit(event.workspaceId(), actor(event.actorUserId(), event.userId()), AuditActionType.PROFILE_IMAGE_REMOVED, AuditOutcome.SUCCESS,
                USER_PROFILE_IMAGE, event.profileId(), "Profile image removed",
                details("userId", event.userId(), "profileId", event.profileId()), null, null,
                eventSource("profile.image.removed.audit", event.profileId(), event.occurredAt()));
    }

    @Override
    public void onProfileSettingsUpdated(ProfileSettingsUpdatedEventDto event) {
        if (event == null) {
            return;
        }
        audit(event.workspaceId(), actor(event.actorUserId(), event.userId()), AuditActionType.SETTINGS_UPDATED, AuditOutcome.SUCCESS,
                USER_ACCOUNT_SETTINGS, event.settingsId(), "User account settings updated",
                details("userId", event.userId(), "settingsId", event.settingsId()), null, null,
                eventSource("profile.settings.updated.audit", event.settingsId(), event.occurredAt()));
    }

    @Override
    public void onProfilePasswordChanged(ProfilePasswordChangedEventDto event) {
        if (event == null) {
            return;
        }
        UUID actorUserId = actor(event.actorUserId(), event.userId());
        notification(event.workspaceId(), event.userId(), actorUserId, NotificationType.PASSWORD_CHANGED,
                "Password changed", "Your password was changed.", USER_PASSWORD, event.userId(),
                eventSource("profile.password.changed.notification", event.userId(), event.occurredAt()));
        activity(event.workspaceId(), actorUserId, ActivityFeedService.TYPE_PASSWORD_CHANGED,
                "Password changed", "A user password was changed.", USER_PASSWORD, event.userId(),
                eventSource("profile.password.changed.activity", event.userId(), event.occurredAt()));
        audit(event.workspaceId(), actorUserId, AuditActionType.PASSWORD_CHANGED, AuditOutcome.SUCCESS,
                USER_PASSWORD, event.userId(), "User password changed",
                details("userId", event.userId(), "otherSessionsRevoked", event.otherSessionsRevoked(), "revokedDeviceCount", event.revokedDeviceCount()),
                null, null, eventSource("profile.password.changed.audit", event.userId(), event.occurredAt()));
    }

    @Override
    public void onProfileSessionRevoked(ProfileSessionRevokedEventDto event) {
        if (event == null || (event.revokedTokenCount() <= 0 && event.revokedDeviceCount() <= 0)) {
            return;
        }
        UUID actorUserId = actor(event.actorUserId(), event.userId());
        notification(event.workspaceId(), event.userId(), actorUserId, NotificationType.SESSION_REVOKED,
                "Session revoked", "One or more sessions were revoked.", USER_SESSION, event.userId(),
                eventSource("profile.session.revoked.notification", event.userId(), event.occurredAt()));
        audit(event.workspaceId(), actorUserId, AuditActionType.SESSION_REVOKED, AuditOutcome.SUCCESS,
                USER_SESSION, event.userId(), "User sessions revoked",
                details("userId", event.userId(), "revokedTokenCount", event.revokedTokenCount(), "revokedDeviceCount", event.revokedDeviceCount(), "currentSessionIncluded", event.currentSessionIncluded()),
                null, null, eventSource("profile.session.revoked.audit", event.userId(), event.occurredAt()));
    }

    @Override
    public void onProfileSecurityActivityCreated(ProfileSecurityActivityCreatedEventDto event) {
        if (event == null || event.success()) {
            return;
        }
        UUID actorUserId = actor(event.actorUserId(), event.userId());
        String sanitizedReason = sanitizeFailureReason(event.failureReason());
        notification(event.workspaceId(), event.userId(), actorUserId, NotificationType.SECURITY_ACTIVITY_DETECTED,
                "Security activity detected", "A security-sensitive profile action was blocked.", USER_SECURITY_ACTIVITY,
                event.securityActivityId(), eventSource("profile.security.activity.notification", event.securityActivityId(), event.occurredAt()));
        audit(event.workspaceId(), actorUserId, AuditActionType.UNAUTHORIZED_PROFILE_ACCESS_ATTEMPT, AuditOutcome.DENIED,
                USER_SECURITY_ACTIVITY, event.securityActivityId(), "Security activity detected",
                details("userId", event.userId(), "activityType", event.activityType(), "failureReason", sanitizedReason),
                null, null, eventSource("profile.security.activity.audit", event.securityActivityId(), event.occurredAt()));
    }

    private void notification(
            UUID workspaceId,
            UUID recipientUserId,
            UUID actorUserId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            String sourceEventId
    ) {
        if (notificationService == null || workspaceId == null || recipientUserId == null || referenceId == null) {
            return;
        }
        try {
            notificationService.createInAppNotification(new NotificationCreateRequest(
                    workspaceId,
                    recipientUserId,
                    actor(actorUserId, recipientUserId),
                    type,
                    title,
                    message,
                    referenceType,
                    referenceId,
                    sourceEventId));
        } catch (RuntimeException exception) {
            log.warn("profile_notification_hook_failed type={} userId={} reason={}", type, recipientUserId, reason(exception));
        }
    }

    private void activity(
            UUID workspaceId,
            UUID actorUserId,
            String activityType,
            String title,
            String description,
            String referenceType,
            UUID referenceId,
            String sourceEventId
    ) {
        if (activityFeedService == null || workspaceId == null || referenceId == null) {
            return;
        }
        try {
            activityFeedService.create(new ActivityFeedCommand(
                    workspaceId,
                    sourceEventId,
                    actorUserId,
                    ActivityCategory.WORKSPACE,
                    activityType,
                    title,
                    description,
                    referenceType,
                    referenceId,
                    Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("profile_activity_hook_failed type={} actorUserId={} reason={}", activityType, actorUserId, reason(exception));
        }
    }

    private void audit(
            UUID workspaceId,
            UUID actorUserId,
            AuditActionType actionType,
            AuditOutcome outcome,
            String entityType,
            UUID entityId,
            String summary,
            Map<String, ?> metadata,
            String ipAddress,
            String userAgent,
            String sourceEventId
    ) {
        if (auditLogService == null || workspaceId == null) {
            return;
        }
        try {
            auditLogService.appendUserAction(
                    workspaceId,
                    sourceEventId,
                    actorUserId,
                    actionType,
                    outcome,
                    entityType,
                    entityId,
                    summary,
                    metadata,
                    ipAddress,
                    userAgent);
        } catch (RuntimeException exception) {
            log.warn("profile_audit_hook_failed action={} actorUserId={} reason={}", actionType, actorUserId, reason(exception));
        }
    }

    private void securityActivity(
            UUID userId,
            UserSecurityActivityType activityType,
            String ipAddress,
            String userAgent,
            boolean success,
            String failureReason
    ) {
        if (userSecurityActivityService == null || userId == null) {
            return;
        }
        try {
            userSecurityActivityService.record(userId, activityType, ipAddress, userAgent, null, success, failureReason);
        } catch (RuntimeException exception) {
            log.warn("profile_security_activity_hook_failed activityType={} userId={} reason={}", activityType, userId, reason(exception));
        }
    }

    private static UUID actor(UUID actorUserId, UUID fallbackUserId) {
        return actorUserId == null ? fallbackUserId : actorUserId;
    }

    private static Map<String, Object> details(Object... values) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (values[i] != null && values[i + 1] != null) {
                details.put(values[i].toString(), values[i + 1]);
            }
        }
        return details;
    }

    private static String source(String prefix, UUID referenceId) {
        return prefix + "." + (referenceId == null ? UUID.randomUUID() : referenceId) + "." + UUID.randomUUID();
    }

    private static String eventSource(String prefix, UUID referenceId, Instant occurredAt) {
        long eventTime = occurredAt == null ? 0L : occurredAt.toEpochMilli();
        return prefix + "." + (referenceId == null ? "unknown" : referenceId) + "." + eventTime;
    }

    private static String sanitizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "security_activity_detected";
        }
        String normalized = failureReason.replaceAll("[^a-zA-Z0-9_.-]", "_");
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private static String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }
}
