package com.lebhas.creativesaas.profile.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ProfileRedisTtlStrategy {

    private static final Duration PROFILE_TTL = Duration.ofMinutes(15);
    private static final Duration SETTINGS_TTL = Duration.ofMinutes(15);
    private static final Duration PROFILE_IMAGE_URL_TTL = Duration.ofMinutes(10);
    private static final Duration SECURITY_ACTIVITY_TTL = Duration.ofMinutes(5);
    private static final Duration PROFILE_UPDATE_RATE_WINDOW = Duration.ofMinutes(1);
    private static final Duration PASSWORD_RATE_WINDOW = Duration.ofMinutes(5);
    private static final Duration LOCK_TTL = Duration.ofSeconds(15);
    private static final Duration SESSION_LOCK_TTL = Duration.ofSeconds(20);
    private static final Duration MIN_TTL = Duration.ofSeconds(5);

    public Duration userProfileTtl() {
        return PROFILE_TTL;
    }

    public Duration accountSettingsTtl() {
        return SETTINGS_TTL;
    }

    public Duration profileImageUrlTtl() {
        return PROFILE_IMAGE_URL_TTL;
    }

    public Duration profileImageUrlTtl(Instant expiresAt) {
        if (expiresAt == null) {
            return profileImageUrlTtl();
        }
        Duration untilExpiry = Duration.between(Instant.now(), expiresAt).minusSeconds(30);
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            return MIN_TTL;
        }
        return untilExpiry.compareTo(PROFILE_IMAGE_URL_TTL) < 0 ? untilExpiry : PROFILE_IMAGE_URL_TTL;
    }

    public Duration securityActivityTtl() {
        return SECURITY_ACTIVITY_TTL;
    }

    public Duration profileUpdateRateWindow() {
        return PROFILE_UPDATE_RATE_WINDOW;
    }

    public Duration passwordRateWindow() {
        return PASSWORD_RATE_WINDOW;
    }

    public Duration lockTtl() {
        return LOCK_TTL;
    }

    public Duration sessionLockTtl() {
        return SESSION_LOCK_TTL;
    }

    public Duration positive(Duration requested, Duration fallback) {
        if (requested == null || requested.isNegative() || requested.isZero()) {
            return fallback;
        }
        return requested;
    }
}
