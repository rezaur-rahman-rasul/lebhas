package com.lebhas.creativesaas.redis;

import com.lebhas.creativesaas.common.exception.RedisOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> compareAndDeleteRedisScript;

    public RedisLockService(
            StringRedisTemplate redisTemplate,
            RedisScript<Long> compareAndDeleteRedisScript
    ) {
        this.redisTemplate = redisTemplate;
        this.compareAndDeleteRedisScript = compareAndDeleteRedisScript;
    }

    public Optional<RedisLockToken> acquire(String key, Duration ttl) {
        try {
            String token = UUID.randomUUID().toString();
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            if (!Boolean.TRUE.equals(acquired)) {
                return Optional.empty();
            }
            return Optional.of(new RedisLockToken(key, token, Instant.now().plus(ttl)));
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to acquire Redis lock: " + key);
        }
    }

    public boolean release(RedisLockToken token) {
        try {
            Long deleted = redisTemplate.execute(compareAndDeleteRedisScript, List.of(token.key()), token.token());
            return deleted != null && deleted > 0;
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to release Redis lock: " + token.key());
        }
    }

    public boolean releaseQuietly(RedisLockToken token) {
        if (token == null) {
            return false;
        }
        try {
            return release(token);
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage().replaceAll("\\s+", " ").trim();
            log.warn("redis_lock operation=release_quietly_failed key={} reason={}", token.key(), reason);
            return false;
        }
    }

    public record RedisLockToken(String key, String token, Instant expiresAt) {
    }
}
