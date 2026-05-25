package com.lebhas.creativesaas.auditlog.cache;

import com.lebhas.creativesaas.auditlog.application.AuditLogView;
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
public class AuditRecentCacheService {

    private static final Logger log = LoggerFactory.getLogger(AuditRecentCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public AuditRecentCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<RecentAuditCacheEntry> getRecentAudit(UUID workspaceId) {
        return read(NotificationRedisKeys.recentAudit(workspaceId), RecentAuditCacheEntry.class, "audit_recent_get");
    }

    public void cacheRecentAudit(UUID workspaceId, List<AuditLogView> auditLogs) {
        write(
                NotificationRedisKeys.recentAudit(workspaceId),
                new RecentAuditCacheEntry(workspaceId, auditLogs == null ? List.of() : List.copyOf(auditLogs)),
                "audit_recent_put");
    }

    public void invalidateRecentAudit(UUID workspaceId) {
        delete(NotificationRedisKeys.recentAudit(workspaceId), "audit_recent_delete");
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
            redisCacheService.set(key, value, ttlStrategy.auditRecentTtl());
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
        log.warn("audit_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record RecentAuditCacheEntry(UUID workspaceId, List<AuditLogView> auditLogs) {
    }
}
