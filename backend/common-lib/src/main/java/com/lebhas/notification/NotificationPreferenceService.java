package com.lebhas.notification;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final NotificationMapper notificationMapper;
    private NotificationPreferenceCacheService notificationPreferenceCacheService;

    public NotificationPreferenceService(
            NotificationPreferenceRepository notificationPreferenceRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            NotificationMapper notificationMapper
    ) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.notificationMapper = notificationMapper;
    }

    @Autowired(required = false)
    void setNotificationPreferenceCacheService(NotificationPreferenceCacheService notificationPreferenceCacheService) {
        this.notificationPreferenceCacheService = notificationPreferenceCacheService;
    }

    @Transactional(readOnly = true)
    public boolean isInAppEnabled(UUID workspaceId, UUID userId, NotificationType notificationType) {
        return notificationPreferenceRepository
                .findByWorkspaceIdAndUserIdAndNotificationTypeAndDeletedFalse(
                        workspaceId,
                        userId,
                        notificationType.name())
                .map(NotificationPreference::isInAppEnabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceView> listUserPreferences(UUID workspaceId, UUID userId) {
        requirePreferenceAccess(workspaceId, userId);
        return notificationPreferenceRepository.findAllByWorkspaceIdAndUserIdAndDeletedFalse(workspaceId, userId)
                .stream()
                .map(notificationMapper::toView)
                .toList();
    }

    @Transactional
    public NotificationPreferenceView upsert(NotificationPreferenceCommand command) {
        UUID workspaceId = require(command.workspaceId(), "workspaceId");
        UUID userId = require(command.userId(), "userId");
        requirePreferenceAccess(workspaceId, userId);

        String notificationType = normalize(command.notificationType(), "notificationType");
        NotificationPreference preference = notificationPreferenceRepository
                .findByWorkspaceIdAndUserIdAndNotificationTypeAndDeletedFalse(workspaceId, userId, notificationType)
                .map(existing -> {
                    existing.update(
                            command.inAppEnabled(),
                            command.emailEnabled(),
                            command.smsEnabled(),
                            command.pushEnabled());
                    return existing;
                })
                .orElseGet(() -> NotificationPreference.create(
                        workspaceId,
                        userId,
                        notificationType,
                        command.inAppEnabled(),
                        command.emailEnabled(),
                        command.smsEnabled(),
                        command.pushEnabled()));
        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        invalidatePreferenceCache(workspaceId, userId);
        return notificationMapper.toView(saved);
    }

    @Transactional
    public List<NotificationPreferenceView> upsertAll(List<NotificationPreferenceCommand> commands) {
        return commands.stream()
                .map(this::upsert)
                .toList();
    }

    private void requirePreferenceAccess(UUID workspaceId, UUID userId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        CurrentUser currentUser = access.currentUser();
        if (currentUser.isMaster() || currentUser.userId().equals(userId)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "Notification preference access denied");
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private void invalidatePreferenceCache(UUID workspaceId, UUID userId) {
        if (notificationPreferenceCacheService != null) {
            notificationPreferenceCacheService.invalidatePreferences(workspaceId, userId);
        }
    }
}
