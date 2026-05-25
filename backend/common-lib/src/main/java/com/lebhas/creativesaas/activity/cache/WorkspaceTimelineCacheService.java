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
public class WorkspaceTimelineCacheService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceTimelineCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public WorkspaceTimelineCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<WorkspaceTimelineCacheEntry> getWorkspaceTimeline(UUID workspaceId) {
        return read(NotificationRedisKeys.workspaceTimeline(workspaceId), WorkspaceTimelineCacheEntry.class, "timeline_workspace_get");
    }

    public void cacheWorkspaceTimeline(UUID workspaceId, List<ActivityFeedView> timeline) {
        write(
                NotificationRedisKeys.workspaceTimeline(workspaceId),
                new WorkspaceTimelineCacheEntry(workspaceId, timeline == null ? List.of() : List.copyOf(timeline)),
                "timeline_workspace_put");
    }

    public void invalidateWorkspaceTimeline(UUID workspaceId) {
        delete(NotificationRedisKeys.workspaceTimeline(workspaceId), "timeline_workspace_delete");
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
            redisCacheService.set(key, value, ttlStrategy.workspaceTimelineTtl());
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
        log.warn("timeline_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record WorkspaceTimelineCacheEntry(UUID workspaceId, List<ActivityFeedView> timeline) {
    }
}
