package com.lebhas.notification;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationCacheService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public NotificationCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<UserNotificationsCacheEntry> getUserNotifications(UUID userId) {
        return read(NotificationRedisKeys.userNotifications(userId), UserNotificationsCacheEntry.class, "notification_user_get");
    }

    public void cacheUserNotifications(UUID userId, List<NotificationView> notifications) {
        write(
                NotificationRedisKeys.userNotifications(userId),
                new UserNotificationsCacheEntry(userId, notifications == null ? List.of() : List.copyOf(notifications)),
                ttlStrategy.userNotificationsTtl(),
                "notification_user_put");
    }

    public Optional<UnreadCountCacheEntry> getUnreadCount(UUID userId) {
        return read(NotificationRedisKeys.userUnreadCount(userId), UnreadCountCacheEntry.class, "notification_unread_get");
    }

    public void cacheUnreadCount(UUID userId, long unreadCount) {
        write(
                NotificationRedisKeys.userUnreadCount(userId),
                new UnreadCountCacheEntry(userId, unreadCount),
                ttlStrategy.unreadCountTtl(),
                "notification_unread_put");
    }

    public void invalidateUser(UUID userId) {
        delete(NotificationRedisKeys.userNotifications(userId), "notification_user_delete");
        delete(NotificationRedisKeys.userUnreadCount(userId), "notification_unread_delete");
    }

    private <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object value, java.time.Duration ttl, String operation) {
        try {
            redisCacheService.set(key, value, ttl);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
        }
    }

    private void delete(String key, String operation) {
        try {
            redisCacheService.delete(key);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        log.warn("notification_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record UserNotificationsCacheEntry(UUID userId, List<NotificationView> notifications) {
    }

    public record UnreadCountCacheEntry(UUID userId, long unreadCount) {
    }
}
