package com.lebhas.creativesaas.redis;

import com.lebhas.creativesaas.common.exception.RedisOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RedisRealtimeStateService {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisRealtimeStateService(StringRedisTemplate redisTemplate, RedisKeyBuilder redisKeyBuilder) {
        this.redisTemplate = redisTemplate;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void markWorkspaceSessionActive(UUID workspaceId, UUID userId, String deviceId, Duration ttl) {
        try {
            String key = redisKeyBuilder.workspaceActive(workspaceId);
            redisTemplate.opsForHash().put(key, member(userId, deviceId), Instant.now().toString());
            redisTemplate.expire(key, ttl);
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to mark active workspace session for workspace " + workspaceId);
        }
    }

    public void clearWorkspaceSession(UUID workspaceId, UUID userId, String deviceId) {
        try {
            redisTemplate.opsForHash().delete(redisKeyBuilder.workspaceActive(workspaceId), member(userId, deviceId));
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to clear active workspace session for workspace " + workspaceId);
        }
    }

    public WorkspaceActivitySnapshot getWorkspaceActivity(UUID workspaceId) {
        try {
            Map<Object, Object> sessions = redisTemplate.opsForHash().entries(redisKeyBuilder.workspaceActive(workspaceId));
            Set<String> activeUsers = sessions.keySet().stream()
                    .map(Object::toString)
                    .map(this::userPart)
                    .collect(Collectors.toSet());
            return new WorkspaceActivitySnapshot(activeUsers.size(), sessions.size(), Instant.now());
        } catch (DataAccessException exception) {
            throw new RedisOperationException("Failed to read active workspace state for workspace " + workspaceId);
        }
    }

    private String member(UUID userId, String deviceId) {
        return userId + ":" + (deviceId == null || deviceId.isBlank() ? "unknown" : deviceId.trim());
    }

    private String userPart(String member) {
        int separatorIndex = member.indexOf(':');
        return separatorIndex < 0 ? member : member.substring(0, separatorIndex);
    }

    public record WorkspaceActivitySnapshot(
            long activeUserCount,
            long activeSessionCount,
            Instant observedAt
    ) {
    }
}
