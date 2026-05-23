package com.lebhas.creativesaas.redis;

import com.lebhas.creativesaas.common.exception.RedisOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitWindow increment(String key, long limit, Duration window) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == null) {
                throw new RedisOperationException("Failed to increment rate-limit key: " + key);
            }
            if (currentCount == 1) {
                redisTemplate.expire(key, window);
            }
            return new RateLimitWindow(currentCount, limit, currentCount <= limit, window);
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to update rate-limit key: " + key);
        }
    }

    public record RateLimitWindow(long currentCount, long limit, boolean allowed, Duration window) {
    }
}
