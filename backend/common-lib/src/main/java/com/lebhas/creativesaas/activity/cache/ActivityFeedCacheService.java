package com.lebhas.creativesaas.activity.cache;

import com.lebhas.creativesaas.activity.application.ActivityFeedView;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.notification.NotificationRedisKeys;
import com.lebhas.notification.NotificationRedisTtlStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActivityFeedCacheService {

    private static final Logger log = LoggerFactory.getLogger(ActivityFeedCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public ActivityFeedCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ActivityFeedCacheEntry> getWorkspaceActivity(UUID workspaceId) {
        return read(NotificationRedisKeys.workspaceActivity(workspaceId), ActivityFeedCacheEntry.class, "activity_workspace_get");
    }

    public void cacheWorkspaceActivity(UUID workspaceId, List<ActivityFeedView> activities) {
        write(
                NotificationRedisKeys.workspaceActivity(workspaceId),
                new ActivityFeedCacheEntry(workspaceId, activities == null ? List.of() : List.copyOf(activities)),
                "activity_workspace_put");
    }

    public void invalidateWorkspaceActivity(UUID workspaceId) {
        delete(NotificationRedisKeys.workspaceActivity(workspaceId), "activity_workspace_delete");
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
            redisCacheService.set(key, value, ttlStrategy.activityFeedTtl());
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
        log.warn("activity_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record ActivityFeedCacheEntry(UUID workspaceId, List<ActivityFeedView> activities) {
    }
}
