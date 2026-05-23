package com.lebhas.creativesaas.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.creativesaas.common.exception.RedisOperationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(
            StringRedisTemplate redisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String serialized = objectMapper.writeValueAsString(value);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                redisTemplate.opsForValue().set(key, serialized);
                return;
            }
            redisTemplate.opsForValue().set(key, serialized, ttl);
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new RedisOperationException("Failed to write Redis key: " + key);
        }
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, type));
        } catch (JsonProcessingException | DataAccessException exception) {
            throw new RedisOperationException("Failed to read Redis key: " + key);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to delete Redis key: " + key);
        }
    }

    public long deleteByPattern(String pattern) {
        try {
            List<String> keys = new ArrayList<>();
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (var cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(500).build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                } catch (Exception exception) {
                    throw new RedisOperationException("Failed to scan Redis keys for pattern: " + pattern);
                }
                return null;
            });
            if (keys.isEmpty()) {
                return 0;
            }
            Long deleted = redisTemplate.delete(keys);
            return deleted == null ? 0 : deleted;
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to delete Redis keys for pattern: " + pattern);
        }
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        return get(key, type).orElseGet(() -> {
            T loaded = loader.get();
            if (loaded != null) {
                set(key, loaded, ttl);
            }
            return loaded;
        });
    }
}
