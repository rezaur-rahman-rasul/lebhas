package com.lebhas.creativesaas.profile.cache;

import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.domain.PreferredLanguage;
import com.lebhas.creativesaas.profile.domain.ThemePreference;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisRateLimitService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileRedisBatch12UnitTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void redisProfileCacheWorks() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        UserProfileCacheService cache = profileCache(redisCacheService);
        UserProfileView view = profileView();
        when(redisCacheService.get(ProfileRedisKeys.userProfile(USER_ID), UserProfileView.class)).thenReturn(Optional.of(view));

        assertThat(cache.cache(USER_ID, view)).isTrue();
        assertThat(cache.get(USER_ID)).contains(view);
        verify(redisCacheService).set(eq(ProfileRedisKeys.userProfile(USER_ID)), eq(view), any(Duration.class));
    }

    @Test
    void redisProfileCacheInvalidatesAfterUpdate() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);

        assertThat(profileCache(redisCacheService).invalidate(USER_ID)).isTrue();

        verify(redisCacheService).delete(ProfileRedisKeys.userProfile(USER_ID));
    }

    @Test
    void redisProfileImageSignedUrlCacheWorks() {
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        ProfileImageUrlCacheService cache = imageCache(redisCacheService);
        Instant expiresAt = Instant.now().plusSeconds(600);
        var entry = new ProfileImageUrlCacheService.ProfileImageUrlCacheEntry(USER_ID, "https://signed.example", expiresAt);
        when(redisCacheService.get(ProfileRedisKeys.profileImageUrl(USER_ID), ProfileImageUrlCacheService.ProfileImageUrlCacheEntry.class))
                .thenReturn(Optional.of(entry));

        assertThat(cache.cache(USER_ID, "https://signed.example", expiresAt)).isTrue();
        assertThat(cache.get(USER_ID)).contains(entry);
        verify(redisCacheService).set(eq(ProfileRedisKeys.profileImageUrl(USER_ID)), any(), any(Duration.class));
    }

    @Test
    void redisPasswordChangeLockWorks() {
        RedisLockService redisLockService = mock(RedisLockService.class);
        RedisLockService.RedisLockToken token = new RedisLockService.RedisLockToken(
                ProfileRedisKeys.lockProfilePassword(USER_ID),
                "token",
                Instant.now().plusSeconds(15));
        when(redisLockService.acquire(eq(ProfileRedisKeys.lockProfilePassword(USER_ID)), any(Duration.class)))
                .thenReturn(Optional.of(token));
        when(redisLockService.releaseQuietly(token)).thenReturn(true);
        ProfileLockService lockService = new ProfileLockService(redisLockService, new ProfileRedisTtlStrategy(), mock(SecurityAuditLogger.class));

        assertThat(lockService.acquirePasswordLock(USER_ID)).contains(token);
        assertThat(lockService.releaseQuietly(token)).isTrue();
    }

    private static UserProfileCacheService profileCache(RedisCacheService redisCacheService) {
        return new UserProfileCacheService(redis(redisCacheService), new ProfileRedisTtlStrategy());
    }

    private static ProfileImageUrlCacheService imageCache(RedisCacheService redisCacheService) {
        return new ProfileImageUrlCacheService(redis(redisCacheService), new ProfileRedisTtlStrategy());
    }

    private static ProfileRedisAccessSupport redis(RedisCacheService redisCacheService) {
        return new ProfileRedisAccessSupport(redisCacheService, mock(RedisRateLimitService.class), mock(SecurityAuditLogger.class));
    }

    private static UserProfileView profileView() {
        return new UserProfileView(
                UUID.randomUUID(),
                USER_ID,
                "Ariana",
                "Rahman",
                "Ariana Rahman",
                null,
                null,
                null,
                null,
                null,
                "Asia/Dhaka",
                "en",
                new UserProfileView.AccountSettingsView(UUID.randomUUID(), PreferredLanguage.BOTH, ThemePreference.SYSTEM, true, true, false, null, null),
                null,
                null);
    }
}
