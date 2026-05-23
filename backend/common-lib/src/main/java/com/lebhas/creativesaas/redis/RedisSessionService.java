package com.lebhas.creativesaas.redis;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisSessionService {

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisSessionService(RedisCacheService redisCacheService, RedisKeyBuilder redisKeyBuilder) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void storeRefreshToken(String tokenId, RefreshTokenSession session, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.authRefresh(tokenId), session, ttl);
    }

    public Optional<RefreshTokenSession> getRefreshToken(String tokenId) {
        return redisCacheService.get(redisKeyBuilder.authRefresh(tokenId), RefreshTokenSession.class);
    }

    public void deleteRefreshToken(String tokenId) {
        redisCacheService.delete(redisKeyBuilder.authRefresh(tokenId));
    }

    public void blacklistAccessToken(String tokenId, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.authBlacklist(tokenId), new TokenBlacklistEntry(tokenId, Instant.now()), ttl);
    }

    public boolean isAccessTokenBlacklisted(String tokenId) {
        return redisCacheService.get(redisKeyBuilder.authBlacklist(tokenId), TokenBlacklistEntry.class).isPresent();
    }

    public void storeUserSession(UUID userId, String deviceId, UserSession session, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.authSession(userId, deviceId), session, ttl);
    }

    public Optional<UserSession> getUserSession(UUID userId, String deviceId) {
        return redisCacheService.get(redisKeyBuilder.authSession(userId, deviceId), UserSession.class);
    }

    public void deleteUserSession(UUID userId, String deviceId) {
        redisCacheService.delete(redisKeyBuilder.authSession(userId, deviceId));
    }

    public void storeRefreshFamily(UUID userId, RefreshTokenFamily family, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.refreshFamily(userId), family, ttl);
    }

    public Optional<RefreshTokenFamily> getRefreshFamily(UUID userId) {
        return redisCacheService.get(redisKeyBuilder.refreshFamily(userId), RefreshTokenFamily.class);
    }

    public void addTokenToRefreshFamily(UUID userId, String tokenId, String tokenFamilyId, Duration ttl) {
        RefreshTokenFamily family = getRefreshFamily(userId)
                .map(existing -> existing.withToken(tokenId, Instant.now()))
                .orElseGet(() -> RefreshTokenFamily.initialize(userId, tokenFamilyId, tokenId, Instant.now()));
        storeRefreshFamily(userId, family, ttl);
    }

    public void removeRefreshFamily(UUID userId) {
        redisCacheService.delete(redisKeyBuilder.refreshFamily(userId));
    }

    public void storeSupportSession(UUID masterUserId, SupportSession supportSession, Duration ttl) {
        redisCacheService.set(redisKeyBuilder.supportSession(masterUserId), supportSession, ttl);
    }

    public Optional<SupportSession> getSupportSession(UUID masterUserId) {
        return redisCacheService.get(redisKeyBuilder.supportSession(masterUserId), SupportSession.class);
    }

    public void deleteSupportSession(UUID masterUserId) {
        redisCacheService.delete(redisKeyBuilder.supportSession(masterUserId));
    }

    public record RefreshTokenSession(
            UUID userId,
            UUID workspaceId,
            String deviceId,
            String tokenFamilyId,
            String tokenHash,
            String clientIp,
            String userAgent,
            Instant expiresAt
    ) {
    }

    public record UserSession(
            UUID userId,
            String deviceId,
            UUID workspaceId,
            String email,
            String role,
            String clientIp,
            String userAgent,
            Instant loginAt,
            Instant lastActivityAt,
            boolean supportModeActive,
            Instant expiresAt
    ) {
    }

    public record SupportSession(
            UUID masterUserId,
            UUID workspaceId,
            String deviceId,
            Instant startedAt,
            Instant expiresAt
    ) {
    }

    public record RefreshTokenFamily(
            UUID userId,
            String tokenFamilyId,
            Set<String> tokenIds,
            Instant updatedAt
    ) {

        public static RefreshTokenFamily initialize(UUID userId, String tokenFamilyId, String tokenId, Instant updatedAt) {
            return new RefreshTokenFamily(userId, tokenFamilyId, Set.of(tokenId), updatedAt);
        }

        public RefreshTokenFamily withToken(String tokenId, Instant updatedAt) {
            LinkedHashSet<String> updatedTokenIds = new LinkedHashSet<>(tokenIds);
            updatedTokenIds.add(tokenId);
            return new RefreshTokenFamily(userId, tokenFamilyId, Set.copyOf(updatedTokenIds), updatedAt);
        }
    }

    public record TokenBlacklistEntry(String tokenId, Instant createdAt) {
    }
}
