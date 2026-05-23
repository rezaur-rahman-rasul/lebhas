package com.lebhas.creativesaas.storage.cache;

import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class StorageRedisAccessSupport {

    private static final Logger log = LoggerFactory.getLogger(StorageRedisAccessSupport.class);

    private final RedisCacheService redisCacheService;

    public StorageRedisAccessSupport(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    public <T> Optional<T> read(String key, Class<T> type, UUID workspaceId) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure("read", key, workspaceId, exception);
            return Optional.empty();
        }
    }

    public boolean write(String key, Object value, Duration ttl, UUID workspaceId) {
        try {
            redisCacheService.set(key, value, ttl);
            return true;
        } catch (RuntimeException exception) {
            logFailure("write", key, workspaceId, exception);
            return false;
        }
    }

    public boolean delete(String key, UUID workspaceId) {
        try {
            redisCacheService.delete(key);
            return true;
        } catch (RuntimeException exception) {
            logFailure("delete", key, workspaceId, exception);
            return false;
        }
    }

    private void logFailure(String operation, String key, UUID workspaceId, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("storage_usage_cache operation={} workspaceId={} key={} reason={}",
                operation,
                workspaceId,
                key,
                reason);
    }
}
