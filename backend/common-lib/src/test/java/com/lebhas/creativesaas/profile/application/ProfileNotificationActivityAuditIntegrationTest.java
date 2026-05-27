package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.activity.application.ActivityFeedCommand;
import com.lebhas.creativesaas.activity.application.ActivityFeedService;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.notification.NotificationCreateRequest;
import com.lebhas.notification.NotificationService;
import com.lebhas.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProfileNotificationActivityAuditIntegrationTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROFILE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void passwordChangedCreatesSafeNotificationActivityAndAudit() {
        NotificationService notificationService = mock(NotificationService.class);
        ActivityFeedService activityFeedService = mock(ActivityFeedService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ProfileNotificationActivityAuditIntegration integration = integration(
                notificationService,
                activityFeedService,
                auditLogService);

        integration.passwordChanged(
                currentUser(),
                new SessionRevocationService.RevocationResult(2, Set.of("device-1", "device-2")),
                "127.0.0.1",
                "JUnit");

        ArgumentCaptor<NotificationCreateRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(notificationService).createInAppNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().notificationType()).isEqualTo(NotificationType.PASSWORD_CHANGED);
        assertThat(notificationCaptor.getValue().message()).doesNotContain("raw-token", "secret", "hash");

        ArgumentCaptor<ActivityFeedCommand> activityCaptor = ArgumentCaptor.forClass(ActivityFeedCommand.class);
        verify(activityFeedService).create(activityCaptor.capture());
        assertThat(activityCaptor.getValue().activityType()).isEqualTo(ActivityFeedService.TYPE_PASSWORD_CHANGED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).appendUserAction(
                eq(WORKSPACE_ID),
                any(String.class),
                eq(USER_ID),
                eq(AuditActionType.PASSWORD_CHANGED),
                eq(AuditOutcome.SUCCESS),
                eq("USER_PASSWORD"),
                eq(USER_ID),
                eq("User password changed"),
                metadataCaptor.capture(),
                eq("127.0.0.1"),
                eq("JUnit"));
        assertThat(metadataCaptor.getValue().get("revokedDeviceCount")).isEqualTo(2);
        assertThat(metadataCaptor.getValue().toString()).doesNotContain("passwordHash", "token", "objectKey");
    }

    @Test
    void profileImageUpdatedCreatesProfileImageActivityAndAuditWithoutObjectKey() {
        NotificationService notificationService = mock(NotificationService.class);
        ActivityFeedService activityFeedService = mock(ActivityFeedService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ProfileNotificationActivityAuditIntegration integration = integration(
                notificationService,
                activityFeedService,
                auditLogService);

        integration.profileImageUpdated(currentUser(), PROFILE_ID, "127.0.0.1", "JUnit");

        ArgumentCaptor<NotificationCreateRequest> notificationCaptor = ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(notificationService).createInAppNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().notificationType()).isEqualTo(NotificationType.PROFILE_IMAGE_UPDATED);

        ArgumentCaptor<ActivityFeedCommand> activityCaptor = ArgumentCaptor.forClass(ActivityFeedCommand.class);
        verify(activityFeedService).create(activityCaptor.capture());
        assertThat(activityCaptor.getValue().activityType()).isEqualTo(ActivityFeedService.TYPE_PROFILE_IMAGE_UPDATED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).appendUserAction(
                eq(WORKSPACE_ID),
                any(String.class),
                eq(USER_ID),
                eq(AuditActionType.PROFILE_IMAGE_UPDATED),
                eq(AuditOutcome.SUCCESS),
                eq("USER_PROFILE_IMAGE"),
                eq(PROFILE_ID),
                eq("Profile image updated"),
                metadataCaptor.capture(),
                eq("127.0.0.1"),
                eq("JUnit"));
        assertThat(metadataCaptor.getValue().toString()).doesNotContain("objectKey", "bucket", "credential");
    }

    private static ProfileNotificationActivityAuditIntegration integration(
            NotificationService notificationService,
            ActivityFeedService activityFeedService,
            AuditLogService auditLogService
    ) {
        ProfileNotificationActivityAuditIntegration integration = new ProfileNotificationActivityAuditIntegration();
        integration.setNotificationService(notificationService);
        integration.setActivityFeedService(activityFeedService);
        integration.setAuditLogService(auditLogService);
        return integration;
    }

    private static CurrentUser currentUser() {
        return new CurrentUser(
                USER_ID,
                WORKSPACE_ID,
                "device",
                "user@example.test",
                Set.of(Role.ADMIN),
                Set.of(Permission.WORKSPACE_VIEW),
                "token-id",
                Instant.now().plusSeconds(900));
    }
}
