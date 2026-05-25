package com.lebhas.notification;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationRecipientResolver notificationRecipientResolver;
    private final NotificationMapper notificationMapper;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private NotificationCacheService notificationCacheService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceService notificationPreferenceService,
            NotificationRecipientResolver notificationRecipientResolver,
            NotificationMapper notificationMapper,
            WorkspaceAuthorizationService workspaceAuthorizationService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceService = notificationPreferenceService;
        this.notificationRecipientResolver = notificationRecipientResolver;
        this.notificationMapper = notificationMapper;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Autowired(required = false)
    void setNotificationCacheService(NotificationCacheService notificationCacheService) {
        this.notificationCacheService = notificationCacheService;
    }

    @Transactional
    public Optional<Notification> createInternal(NotificationCreateRequest request) {
        if (notificationRepository.existsBySourceEventIdAndDeletedFalse(request.sourceEventId())) {
            return Optional.empty();
        }
        if (!notificationPreferenceService.isInAppEnabled(
                request.workspaceId(),
                request.recipientUserId(),
                request.notificationType())) {
            return Optional.empty();
        }
        Notification saved = notificationRepository.save(Notification.create(request));
        invalidateNotificationCache(saved.getRecipientUserId());
        return Optional.of(saved);
    }

    @Transactional
    public Optional<NotificationView> createInAppNotification(NotificationCreateRequest request) {
        return createInternal(request).map(notificationMapper::toView);
    }

    @Transactional
    public List<NotificationView> createInAppNotifications(
            NotificationCreateRequest template,
            NotificationRecipientRequest recipientRequest
    ) {
        Set<UUID> recipients = notificationRecipientResolver.resolveRecipients(recipientRequest);
        return recipients.stream()
                .map(recipientUserId -> copyForRecipient(template, recipientUserId))
                .map(this::createInAppNotification)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public NotificationView markAsRead(UUID workspaceId, UUID notificationId, UUID userId) {
        requireNotificationAccess(workspaceId, userId);
        Notification notification = notificationRepository
                .findByIdAndWorkspaceIdAndRecipientUserIdAndDeletedFalse(notificationId, workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
        if (notification.getNotificationStatus() == NotificationStatus.UNREAD) {
            notification.markRead(Instant.now());
        }
        Notification saved = notificationRepository.save(notification);
        invalidateNotificationCache(userId);
        return notificationMapper.toView(saved);
    }

    @Transactional
    public List<NotificationView> markAllAsRead(UUID workspaceId, UUID userId) {
        requireNotificationAccess(workspaceId, userId);
        List<Notification> unreadNotifications = notificationRepository
                .findAllByWorkspaceIdAndRecipientUserIdAndNotificationStatusAndDeletedFalse(
                        workspaceId,
                        userId,
                        NotificationStatus.UNREAD);
        Instant readAt = Instant.now();
        unreadNotifications.forEach(notification -> notification.markRead(readAt));
        List<Notification> saved = notificationRepository.saveAll(unreadNotifications);
        invalidateNotificationCache(userId);
        return saved
                .stream()
                .map(notificationMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationView> listUserNotifications(UUID workspaceId, UUID userId) {
        requireNotificationAccess(workspaceId, userId);
        return notificationRepository.findAllByWorkspaceIdAndRecipientUserIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId, userId)
                .stream()
                .map(notificationMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID workspaceId, UUID userId) {
        requireNotificationAccess(workspaceId, userId);
        return notificationRepository.countByWorkspaceIdAndRecipientUserIdAndNotificationStatusAndDeletedFalse(
                workspaceId,
                userId,
                NotificationStatus.UNREAD);
    }

    @Transactional(readOnly = true)
    public boolean canDeliverInApp(UUID workspaceId, UUID userId, NotificationType notificationType) {
        return notificationPreferenceService.isInAppEnabled(workspaceId, userId, notificationType);
    }

    private void requireNotificationAccess(UUID workspaceId, UUID userId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        CurrentUser currentUser = access.currentUser();
        if (currentUser.isMaster() || currentUser.userId().equals(userId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "Notification access denied");
    }

    private static NotificationCreateRequest copyForRecipient(NotificationCreateRequest template, UUID recipientUserId) {
        return new NotificationCreateRequest(
                template.workspaceId(),
                recipientUserId,
                template.actorUserId(),
                template.notificationType(),
                template.title(),
                template.message(),
                template.referenceType(),
                template.referenceId(),
                template.sourceEventId() + ":" + recipientUserId);
    }

    private void invalidateNotificationCache(UUID userId) {
        if (notificationCacheService != null && userId != null) {
            notificationCacheService.invalidateUser(userId);
        }
    }
}
