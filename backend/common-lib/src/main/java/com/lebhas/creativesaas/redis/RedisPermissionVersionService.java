package com.lebhas.creativesaas.redis;

import com.lebhas.creativesaas.common.exception.RedisOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RedisPermissionVersionService {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisPermissionVersionService(StringRedisTemplate redisTemplate, RedisKeyBuilder redisKeyBuilder) {
        this.redisTemplate = redisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public long getVersion(UUID workspaceId) {
        try {
            String value = redisTemplate.opsForValue().get(redisKeyBuilder.permissionVersion(workspaceId));
            return value == null || value.isBlank() ? 1L : Long.parseLong(value);
        } catch (DataAccessException | NumberFormatException exception) {
            throw new RedisOperationException("Failed to read permission version for workspace " + workspaceId);
        }
    }

    public long increment(UUID workspaceId) {
        try {
            Long next = redisTemplate.opsForValue().increment(redisKeyBuilder.permissionVersion(workspaceId));
            if (next == null) {
                throw new RedisOperationException("Failed to increment permission version for workspace " + workspaceId);
            }
            return next;
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to increment permission version for workspace " + workspaceId);
        }
    }
}
