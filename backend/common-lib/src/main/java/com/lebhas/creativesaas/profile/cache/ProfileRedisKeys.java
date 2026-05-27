package com.lebhas.creativesaas.profile.cache;

import java.util.UUID;

public final class ProfileRedisKeys {

    private ProfileRedisKeys() {
    }

    public static String userProfile(UUID userId) {
        return "profile:user:" + requireId(userId, "userId");
    }

    public static String accountSettings(UUID userId) {
        return "profile:settings:" + requireId(userId, "userId");
    }

    public static String profileImageUrl(UUID userId) {
        return "profile:image:url:" + requireId(userId, "userId");
    }

    public static String profileImageUploadSession(UUID uploadReferenceId) {
        return "profile:image:upload:" + requireId(uploadReferenceId, "uploadReferenceId");
    }

    public static String profileUpdateRateLimit(UUID userId) {
        return "profile:update:ratelimit:" + requireId(userId, "userId");
    }

    public static String profilePasswordRateLimit(UUID userId) {
        return "profile:password:ratelimit:" + requireId(userId, "userId");
    }

    public static String recentSecurityActivity(UUID userId) {
        return "profile:security:recent:" + requireId(userId, "userId");
    }

    public static String userSession(UUID userId) {
        return "session:user:" + requireId(userId, "userId");
    }

    public static String tokenBlacklist(String tokenId) {
        return "token:blacklist:" + normalize(tokenId);
    }

    public static String lockProfileUpdate(UUID userId) {
        return "lock:profile:update:" + requireId(userId, "userId");
    }

    public static String lockProfilePassword(UUID userId) {
        return "lock:profile:password:" + requireId(userId, "userId");
    }

    public static String lockProfileImage(UUID userId) {
        return "lock:profile:image:" + requireId(userId, "userId");
    }

    public static String lockProfileSession(UUID userId) {
        return "lock:profile:session:" + requireId(userId, "userId");
    }

    private static UUID requireId(UUID id, String fieldName) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return id;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
