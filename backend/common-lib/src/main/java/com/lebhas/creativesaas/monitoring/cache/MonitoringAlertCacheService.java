package com.lebhas.creativesaas.monitoring.cache;

import com.lebhas.creativesaas.monitoring.application.MonitoringAlertView;
import com.lebhas.creativesaas.monitoring.application.SystemHealthEventView;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.notification.NotificationRedisKeys;
import com.lebhas.notification.NotificationRedisTtlStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MonitoringAlertCacheService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringAlertCacheService.class);

    private final RedisCacheService redisCacheService;
    private final NotificationRedisTtlStrategy ttlStrategy;

    public MonitoringAlertCacheService(RedisCacheService redisCacheService, NotificationRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ActiveMonitoringAlertsCacheEntry> getActiveAlerts() {
        return read(NotificationRedisKeys.activeMonitoringAlerts(), ActiveMonitoringAlertsCacheEntry.class, "monitoring_alerts_get");
    }

    public void cacheActiveAlerts(List<MonitoringAlertView> alerts) {
        write(
                NotificationRedisKeys.activeMonitoringAlerts(),
                new ActiveMonitoringAlertsCacheEntry(alerts == null ? List.of() : List.copyOf(alerts)),
                ttlStrategy.monitoringAlertsTtl(),
                "monitoring_alerts_put");
    }

    public Optional<SystemHealthCacheEntry> getSystemHealth() {
        return read(NotificationRedisKeys.systemHealth(), SystemHealthCacheEntry.class, "monitoring_health_get");
    }

    public void cacheSystemHealth(List<SystemHealthEventView> healthEvents) {
        write(
                NotificationRedisKeys.systemHealth(),
                new SystemHealthCacheEntry(healthEvents == null ? List.of() : List.copyOf(healthEvents)),
                ttlStrategy.systemHealthTtl(),
                "monitoring_health_put");
    }

    public void invalidateActiveAlerts() {
        delete(NotificationRedisKeys.activeMonitoringAlerts(), "monitoring_alerts_delete");
    }

    public void invalidateSystemHealth() {
        delete(NotificationRedisKeys.systemHealth(), "monitoring_health_delete");
    }

    public void invalidateAll() {
        invalidateActiveAlerts();
        invalidateSystemHealth();
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
        log.warn("monitoring_redis_failure operation={} key={} reason={}", operation, key, reason(exception));
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record ActiveMonitoringAlertsCacheEntry(List<MonitoringAlertView> alerts) {
    }

    public record SystemHealthCacheEntry(List<SystemHealthEventView> healthEvents) {
    }
}
