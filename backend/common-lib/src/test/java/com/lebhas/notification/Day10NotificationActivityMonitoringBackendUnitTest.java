package com.lebhas.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lebhas.ai.event.AiProviderHealthChangedEvent;
import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.creativesaas.activity.application.ActivityFeedMapper;
import com.lebhas.creativesaas.activity.application.ActivityFeedService;
import com.lebhas.creativesaas.activity.application.ActivityFeedView;
import com.lebhas.creativesaas.activity.domain.ActivityCategory;
import com.lebhas.creativesaas.activity.domain.ActivityFeed;
import com.lebhas.creativesaas.activity.infrastructure.persistence.ActivityFeedRepository;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.auditlog.application.AuditEventMapper;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.application.AuditQueryService;
import com.lebhas.creativesaas.auditlog.application.AuditLogView;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditLog;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.auditlog.infrastructure.persistence.AuditLogRepository;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.generation.event.GenerationCompletedEventDto;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.monitoring.application.MasterMonitoringService;
import com.lebhas.creativesaas.monitoring.application.MonitoringAlertCommand;
import com.lebhas.creativesaas.monitoring.application.MonitoringAlertService;
import com.lebhas.creativesaas.monitoring.application.MonitoringMapper;
import com.lebhas.creativesaas.monitoring.application.SystemHealthEventService;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlert;
import com.lebhas.creativesaas.monitoring.domain.MonitoringAlertStatus;
import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthEvent;
import com.lebhas.creativesaas.monitoring.domain.SystemHealthStatus;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.MonitoringAlertRepository;
import com.lebhas.creativesaas.monitoring.infrastructure.persistence.SystemHealthEventRepository;
import com.lebhas.creativesaas.payment.application.event.PaymentTransactionEventDto;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Day10NotificationActivityMonitoringBackendUnitTest {

    private static final UUID WORKSPACE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_WORKSPACE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID REFERENCE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TRANSACTION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID PROVIDER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-05-24T10:15:30Z");

    @BeforeEach
    void resetSharedConsumerMocks() {
        reset(
                notificationService(),
                activityFeedService(),
                auditLogService(),
                systemHealthEventService(),
                monitoringAlertService());
    }

    @Test
    void notificationPersistsCorrectly() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        NotificationService service = notificationService(repository, true, currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_VIEW)));

        Optional<Notification> notification = service.createInternal(notificationRequest("notification:persisted"));

        assertThat(notification).isPresent();
        assertThat(notification.orElseThrow().getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(notification.orElseThrow().getNotificationType()).isEqualTo(NotificationType.AI_GENERATION_COMPLETED);
        assertThat(notification.orElseThrow().getNotificationStatus()).isEqualTo(NotificationStatus.UNREAD);
        verify(repository).save(any(Notification.class));
    }

    @Test
    void notificationPreferencePersistsCorrectly() {
        NotificationPreference preference = NotificationPreference.create(
                WORKSPACE_ID,
                USER_ID,
                "ai_generation_completed",
                true,
                false,
                false,
                false);

        assertThat(preference.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(preference.getUserId()).isEqualTo(USER_ID);
        assertThat(preference.getNotificationType()).isEqualTo("AI_GENERATION_COMPLETED");
        assertThat(preference.isInAppEnabled()).isTrue();
        assertThat(preference.isEmailEnabled()).isFalse();
    }

    @Test
    void activityFeedPersistsCorrectly() {
        ActivityFeedRepository repository = mock(ActivityFeedRepository.class);
        when(repository.save(any(ActivityFeed.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        ActivityFeedService service = new ActivityFeedService(repository, mock(WorkspaceAuthorizationService.class), new ActivityFeedMapper());

        Optional<ActivityFeedView> view = service.generationCompleted(
                WORKSPACE_ID,
                "activity:persisted",
                ACTOR_ID,
                REFERENCE_ID,
                "Generation completed");

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.orElseThrow().activityCategory()).isEqualTo(ActivityCategory.GENERATED_VERSION);
        verify(repository).save(any(ActivityFeed.class));
    }

    @Test
    void auditLogPersistsCorrectly() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        AuditLogService service = new AuditLogService(repository, new AuditEventMapper(new ObjectMapper()), mock(CurrentUserContext.class));

        Optional<AuditLogView> view = service.appendUserAction(
                WORKSPACE_ID,
                "audit:persisted",
                ACTOR_ID,
                AuditActionType.APPROVE,
                AuditOutcome.SUCCESS,
                "APPROVAL_REQUEST",
                REFERENCE_ID,
                "Approval approved",
                Map.of("status", "APPROVED"),
                "127.0.0.1",
                "JUnit");

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(view.orElseThrow().actionType()).isEqualTo(AuditActionType.APPROVE);
        verify(repository).save(any(AuditLog.class));
    }

    @Test
    void systemHealthEventPersistsCorrectly() {
        SystemHealthEvent event = SystemHealthEvent.create(
                WORKSPACE_ID,
                "health:persisted",
                SystemComponentType.AI,
                "openai",
                SystemHealthStatus.DEGRADED,
                MonitoringSeverity.WARNING,
                "AI provider degraded",
                "{\"failedRequests\":3}",
                NOW);

        assertThat(event.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(event.getComponentType()).isEqualTo(SystemComponentType.AI);
        assertThat(event.getHealthStatus()).isEqualTo(SystemHealthStatus.DEGRADED);
        assertThat(event.getSeverity()).isEqualTo(MonitoringSeverity.WARNING);
    }

    @Test
    void monitoringAlertPersistsCorrectly() {
        MonitoringAlert alert = MonitoringAlert.create(
                WORKSPACE_ID,
                "ai:provider:failed",
                SystemComponentType.AI,
                "openai",
                MonitoringSeverity.ERROR,
                "AI provider failed",
                "Provider is unhealthy",
                NOW);

        assertThat(alert.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(alert.getAlertKey()).isEqualTo("ai:provider:failed");
        assertThat(alert.getAlertStatus()).isEqualTo(MonitoringAlertStatus.OPEN);
    }

    @Test
    void notificationCreatedFromGenerationCompletedEvent() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        NotificationService service = notificationService(repository, true, currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_VIEW)));
        GenerationCompletedEventDto event = generationCompletedEvent();

        Optional<Notification> notification = service.createInternal(new NotificationCreateRequest(
                event.workspaceId(),
                USER_ID,
                USER_ID,
                NotificationType.AI_GENERATION_COMPLETED,
                "Generation completed",
                "Your generation is ready.",
                "GENERATED_VERSION",
                event.generatedVersionId(),
                "generation.completed:" + event.generationJobId() + ":notification"));

        assertThat(notification).isPresent();
        assertThat(notification.orElseThrow().getReferenceId()).isEqualTo(event.generatedVersionId());
        assertThat(notification.orElseThrow().getNotificationType()).isEqualTo(NotificationType.AI_GENERATION_COMPLETED);
    }

    @Test
    void activityCreatedFromPaymentSucceededEvent() {
        Day10NotificationActivityAuditConsumer consumer = consumer();
        PaymentTransactionEventDto event = paymentEvent(PaymentTransactionStatus.SUCCESS, null);

        consumer.consumePaymentSucceeded(event);

        verify(activityFeedService()).paymentCompleted(
                eq(WORKSPACE_ID),
                eq("payment.transaction.succeeded:" + TRANSACTION_ID),
                eq(USER_ID),
                eq(TRANSACTION_ID));
    }

    @Test
    void auditLogCreatedFromApprovalAction() {
        Day10NotificationActivityAuditConsumer consumer = consumer();

        consumer.consumeApprovalDecision(approvalEvent("approval:event:1", ApprovalStatus.APPROVED));

        verify(auditLogService()).appendUserAction(
                eq(WORKSPACE_ID),
                eq("approval:event:1"),
                eq(ACTOR_ID),
                eq(AuditActionType.APPROVE),
                eq(AuditOutcome.SUCCESS),
                eq("APPROVAL_REQUEST"),
                eq(REFERENCE_ID),
                eq("Approval approved"),
                any(),
                eq(null),
                eq(null));
    }

    @Test
    void unreadNotificationCountWorks() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.countByWorkspaceIdAndRecipientUserIdAndNotificationStatusAndDeletedFalse(
                WORKSPACE_ID,
                USER_ID,
                NotificationStatus.UNREAD)).thenReturn(7L);
        NotificationService service = notificationService(repository, true, currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_VIEW)));

        assertThat(service.unreadCount(WORKSPACE_ID, USER_ID)).isEqualTo(7);
    }

    @Test
    void markNotificationAsReadWorks() {
        Notification notification = Notification.create(notificationRequest("notification:read"));
        ReflectionTestUtils.setField(notification, "id", REFERENCE_ID);
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.findByIdAndWorkspaceIdAndRecipientUserIdAndDeletedFalse(REFERENCE_ID, WORKSPACE_ID, USER_ID))
                .thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationService service = notificationService(repository, true, currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_VIEW)));

        NotificationView view = service.markAsRead(WORKSPACE_ID, REFERENCE_ID, USER_ID);

        assertThat(view.notificationStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(view.readAt()).isNotNull();
    }

    @Test
    void notificationPreferenceBlocksDisabledType() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationService service = notificationService(repository, false, currentUser(Role.ADMIN, Set.of(Permission.WORKSPACE_VIEW)));

        assertThat(service.createInternal(notificationRequest("notification:blocked"))).isEmpty();
        verify(repository, times(0)).save(any(Notification.class));
    }

    @Test
    void activityFeedWorkspaceIsolationWorks() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requireWorkspaceContext(OTHER_WORKSPACE_ID))
                .thenThrow(new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        ActivityFeedService service = new ActivityFeedService(mock(ActivityFeedRepository.class), authorizationService, new ActivityFeedMapper());

        assertThatThrownBy(() -> service.listWorkspaceActivities(OTHER_WORKSPACE_ID, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    @Test
    void auditLogWorkspaceIsolationWorks() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requirePermission(OTHER_WORKSPACE_ID, Permission.WORKSPACE_VIEW))
                .thenThrow(new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED));
        AuditQueryService service = new AuditQueryService(mock(AuditLogRepository.class), authorizationService, new AuditEventMapper(new ObjectMapper()));

        assertThatThrownBy(() -> service.listWorkspaceAuditLogs(OTHER_WORKSPACE_ID, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORKSPACE_ACCESS_DENIED);
    }

    @Test
    void monitoringAlertCreatedFromProviderFailure() {
        Day10NotificationActivityAuditConsumer consumer = consumer();
        AiProviderHealthChangedEvent event = new AiProviderHealthChangedEvent(
                "ai:provider:failed:1",
                NOW,
                PROVIDER_ID,
                "FAILED",
                new BigDecimal("0.45"),
                new BigDecimal("90.00"),
                100,
                12,
                Map.of());

        consumer.consumeAiProviderHealth(event);

        verify(monitoringAlertService()).openAlert(any(MonitoringAlertCommand.class));
    }

    @Test
    void redisNotificationCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        NotificationCacheService cache = new NotificationCacheService(redis, new NotificationRedisTtlStrategy());
        NotificationView view = new NotificationMapper().toView(Notification.create(notificationRequest("notification:cache")));

        cache.cacheUserNotifications(USER_ID, List.of(view));

        verify(redis).set(
                eq(NotificationRedisKeys.userNotifications(USER_ID)),
                any(NotificationCacheService.UserNotificationsCacheEntry.class),
                eq(Duration.ofMinutes(5)));
    }

    @Test
    void redisUnreadCountCacheWorks() {
        RedisCacheService redis = mock(RedisCacheService.class);
        NotificationCacheService cache = new NotificationCacheService(redis, new NotificationRedisTtlStrategy());
        NotificationCacheService.UnreadCountCacheEntry entry = new NotificationCacheService.UnreadCountCacheEntry(USER_ID, 4);
        when(redis.get(NotificationRedisKeys.userUnreadCount(USER_ID), NotificationCacheService.UnreadCountCacheEntry.class))
                .thenReturn(Optional.of(entry));

        cache.cacheUnreadCount(USER_ID, 4);

        assertThat(cache.getUnreadCount(USER_ID)).contains(entry);
        verify(redis).set(
                eq(NotificationRedisKeys.userUnreadCount(USER_ID)),
                any(NotificationCacheService.UnreadCountCacheEntry.class),
                eq(Duration.ofMinutes(2)));
    }

    @Test
    void kafkaNotificationConsumerIsIdempotent() {
        Day10NotificationActivityAuditConsumer consumer = consumer();
        PaymentTransactionEventDto event = paymentEvent(PaymentTransactionStatus.SUCCESS, null);
        ArgumentCaptor<NotificationCreateRequest> requestCaptor = ArgumentCaptor.forClass(NotificationCreateRequest.class);

        consumer.consumePaymentSucceeded(event);
        consumer.consumePaymentSucceeded(event);

        verify(notificationService(), times(2)).createInAppNotification(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(NotificationCreateRequest::sourceEventId)
                .containsOnly("payment.transaction.succeeded:" + TRANSACTION_ID + ":notification");
    }

    @Test
    void masterCanViewMonitoring() {
        CurrentUserContext currentUserContext = mock(CurrentUserContext.class);
        when(currentUserContext.requireCurrentUser()).thenReturn(currentUser(Role.MASTER, Set.of()));
        MonitoringAlertRepository alertRepository = mock(MonitoringAlertRepository.class);
        when(alertRepository.findAllByDeletedFalseOrderByTriggeredAtDesc(any(Pageable.class))).thenReturn(List.of());
        MasterMonitoringService service = new MasterMonitoringService(
                mock(SystemHealthEventRepository.class),
                alertRepository,
                mock(MonitoringAlertService.class),
                new MonitoringMapper(new ObjectMapper()),
                currentUserContext);

        assertThat(service.recentAlerts(10)).isEmpty();
    }

    @Test
    void crewCannotViewAuditLogsWithoutPermission() {
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requirePermission(WORKSPACE_ID, Permission.WORKSPACE_VIEW))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
        AuditQueryService service = new AuditQueryService(mock(AuditLogRepository.class), authorizationService, new AuditEventMapper(new ObjectMapper()));

        assertThatThrownBy(() -> service.listWorkspaceAuditLogs(WORKSPACE_ID, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void standardApiResponseFormatWorks() {
        ApiResponse<String> response = ApiResponse.success("Monitoring loaded", "ok");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Monitoring loaded");
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    private static NotificationCreateRequest notificationRequest(String sourceEventId) {
        return new NotificationCreateRequest(
                WORKSPACE_ID,
                USER_ID,
                ACTOR_ID,
                NotificationType.AI_GENERATION_COMPLETED,
                "Generation completed",
                "Your generation is ready.",
                "GENERATED_VERSION",
                REFERENCE_ID,
                sourceEventId);
    }

    private static NotificationService notificationService(
            NotificationRepository repository,
            boolean preferenceEnabled,
            CurrentUser currentUser
    ) {
        NotificationPreferenceService preferenceService = mock(NotificationPreferenceService.class);
        when(preferenceService.isInAppEnabled(eq(WORKSPACE_ID), eq(USER_ID), any(NotificationType.class)))
                .thenReturn(preferenceEnabled);
        WorkspaceAuthorizationService authorizationService = mock(WorkspaceAuthorizationService.class);
        when(authorizationService.requireWorkspaceContext(WORKSPACE_ID))
                .thenReturn(new WorkspaceAuthorizationService.WorkspaceAccess(null, currentUser, null, Role.ADMIN, currentUser.permissions()));
        return new NotificationService(
                repository,
                preferenceService,
                mock(NotificationRecipientResolver.class),
                new NotificationMapper(),
                authorizationService);
    }

    private static CurrentUser currentUser(Role role, Set<Permission> permissions) {
        return new CurrentUser(
                USER_ID,
                WORKSPACE_ID,
                "device",
                "user@example.test",
                Set.of(role),
                permissions,
                "token",
                Instant.now().plusSeconds(3600));
    }

    private static GenerationCompletedEventDto generationCompletedEvent() {
        return new GenerationCompletedEventDto(
                WORKSPACE_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                REFERENCE_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                UUID.randomUUID(),
                new BigDecimal("3.0000"),
                "mock-ai",
                "image-model",
                "provider-job-1",
                true,
                NOW);
    }

    private static PaymentTransactionEventDto paymentEvent(PaymentTransactionStatus status, String failureReason) {
        return new PaymentTransactionEventDto(
                WORKSPACE_ID,
                TRANSACTION_ID,
                USER_ID,
                PROVIDER_ID,
                PaymentPurpose.CREDIT_PURCHASE,
                "CREDIT_PURCHASE_ORDER",
                REFERENCE_ID,
                new BigDecimal("25.00"),
                "USD",
                "provider-tx-1",
                "provider-session-1",
                status,
                failureReason);
    }

    private static ApprovalLifecycleEvent approvalEvent(String eventId, ApprovalStatus currentStatus) {
        return new ApprovalLifecycleEvent(
                eventId,
                NOW,
                WORKSPACE_ID,
                REFERENCE_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                USER_ID,
                ACTOR_ID,
                ACTOR_ID,
                null,
                ApprovalStatus.IN_REVIEW,
                currentStatus,
                NOW.plusSeconds(3600),
                "Looks good",
                false,
                1);
    }

    private static Day10NotificationActivityAuditConsumer consumer() {
        return new Day10NotificationActivityAuditConsumer(
                new ObjectMapper().registerModule(new JavaTimeModule()),
                notificationService(),
                activityFeedService(),
                auditLogService(),
                systemHealthEventService(),
                monitoringAlertService());
    }

    private static NotificationService notificationService() {
        return Holder.NOTIFICATION_SERVICE;
    }

    private static ActivityFeedService activityFeedService() {
        return Holder.ACTIVITY_FEED_SERVICE;
    }

    private static AuditLogService auditLogService() {
        return Holder.AUDIT_LOG_SERVICE;
    }

    private static SystemHealthEventService systemHealthEventService() {
        return Holder.SYSTEM_HEALTH_EVENT_SERVICE;
    }

    private static MonitoringAlertService monitoringAlertService() {
        return Holder.MONITORING_ALERT_SERVICE;
    }

    private static <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(entity, "createdAt", Instant.now());
        return entity;
    }

    private static final class Holder {
        private static final NotificationService NOTIFICATION_SERVICE = mock(NotificationService.class);
        private static final ActivityFeedService ACTIVITY_FEED_SERVICE = mock(ActivityFeedService.class);
        private static final AuditLogService AUDIT_LOG_SERVICE = mock(AuditLogService.class);
        private static final SystemHealthEventService SYSTEM_HEALTH_EVENT_SERVICE = mock(SystemHealthEventService.class);
        private static final MonitoringAlertService MONITORING_ALERT_SERVICE = mock(MonitoringAlertService.class);
    }
}
