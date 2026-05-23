package com.lebhas.creativesaas.common.security.session;

import com.lebhas.creativesaas.redis.RedisSessionService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RedisAccessTokenRevocationStore implements AccessTokenRevocationStore {

    private final RedisSessionService redisSessionService;
    private final Clock clock;

    public RedisAccessTokenRevocationStore(
            RedisSessionService redisSessionService,
            Clock clock
    ) {
        this.redisSessionService = redisSessionService;
        this.clock = clock;
    }

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisSessionService.blacklistAccessToken(tokenId, ttl);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        return redisSessionService.isAccessTokenBlacklisted(tokenId);
    }
}
