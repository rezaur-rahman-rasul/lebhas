package com.lebhas.notification;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceCacheService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public NotificationPreferenceCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<NotificationPreferencesCacheEntry> getPreferences(UUID workspaceId, UUID userId) {
        return read(NotificationRedisKeys.userPreferences(workspaceId, userId), NotificationPreferencesCacheEntry.class, "notification_preference_get");
    }

    public void cachePreferences(UUID workspaceId, UUID userId, List<NotificationPreferenceView> preferences) {
        write(
                NotificationRedisKeys.userPreferences(workspaceId, userId),
                new NotificationPreferencesCacheEntry(workspaceId, userId, preferences == null ? List.of() : List.copyOf(preferences)),
                "notification_preference_put");
    }

    public void invalidatePreferences(UUID workspaceId, UUID userId) {
        delete(NotificationRedisKeys.userPreferences(workspaceId, userId), "notification_preference_delete");
    }

    private <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object value, String operation) {
        try {
            redisCacheService.set(key, value, ttlStrategy.preferenceTtl());
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
        log.warn("notification_preference_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record NotificationPreferencesCacheEntry(UUID workspaceId, UUID userId, List<NotificationPreferenceView> preferences) {
    }
}
