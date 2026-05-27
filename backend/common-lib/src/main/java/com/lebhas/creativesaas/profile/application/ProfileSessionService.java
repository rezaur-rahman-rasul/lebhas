package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.identity.domain.RefreshTokenEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.RefreshTokenRepository;
import com.lebhas.creativesaas.profile.application.dto.UserSessionView;
import com.lebhas.creativesaas.profile.cache.ProfileLockService;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileSessionService {

    private final CurrentUserContext currentUserContext;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRevocationService sessionRevocationService;
    private final ProfileLockService profileLockService;
    private final Clock clock;

    public ProfileSessionService(
            CurrentUserContext currentUserContext,
            RefreshTokenRepository refreshTokenRepository,
            SessionRevocationService sessionRevocationService,
            ProfileLockService profileLockService,
            Clock clock
    ) {
        this.currentUserContext = currentUserContext;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRevocationService = sessionRevocationService;
        this.profileLockService = profileLockService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<UserSessionView> listOwnSessions() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        Instant now = clock.instant();
        Map<String, List<RefreshTokenEntity>> byDevice = refreshTokenRepository
                .findAllByUserIdAndDeletedFalse(currentUser.userId())
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        token -> normalizeDeviceId(token.getDeviceId()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        return byDevice.entrySet()
                .stream()
                .map(entry -> toView(entry.getKey(), entry.getValue(), currentUser, now))
                .sorted(Comparator.comparing(UserSessionView::active).reversed()
                        .thenComparing(UserSessionView::lastUsedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserSessionView::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public UserSessionView revokeOwnSession(String sessionId) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        String deviceId = normalizeSessionId(sessionId);
        if (normalizeDeviceId(currentUser.deviceId()).equals(deviceId)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Current session cannot be revoked from this endpoint. Use logout instead.");
        }
        RedisLockService.RedisLockToken lockToken = profileLockService.acquireSessionLock(currentUser.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Session update is already in progress"));
        try {
            sessionRevocationService.revokeSession(currentUser, deviceId);
            List<RefreshTokenEntity> tokens = refreshTokenRepository.findAllByUserIdAndDeviceIdAndDeletedFalse(
                    currentUser.userId(),
                    deviceId);
            if (tokens.isEmpty()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found");
            }
            return toView(deviceId, tokens, currentUser, clock.instant());
        } finally {
            profileLockService.releaseQuietly(lockToken);
        }
    }

    private UserSessionView toView(String deviceId, List<RefreshTokenEntity> tokens, CurrentUser currentUser, Instant now) {
        RefreshTokenEntity latest = tokens.stream()
                .max(Comparator.comparing(this::lastSeenAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));
        boolean active = tokens.stream().anyMatch(token -> !token.isRevoked() && !token.isExpired(now));
        Instant revokedAt = tokens.stream()
                .map(RefreshTokenEntity::getRevokedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new UserSessionView(
                deviceId,
                latest.getWorkspaceId(),
                deviceId,
                maskIp(latest.getClientIp()),
                latest.getUserAgent(),
                normalizeDeviceId(currentUser.deviceId()).equals(deviceId),
                active,
                latest.getLastUsedAt(),
                latest.getExpiresAt(),
                revokedAt,
                latest.getCreatedAt());
    }

    private Instant lastSeenAt(RefreshTokenEntity token) {
        if (token.getLastUsedAt() != null) {
            return token.getLastUsedAt();
        }
        return token.getCreatedAt();
    }

    private static String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "sessionId is required");
        }
        return normalizeDeviceId(sessionId);
    }

    private static String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "default-device" : deviceId.trim();
    }

    private static String maskIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        String trimmed = ipAddress.trim();
        if (trimmed.contains(":")) {
            int index = trimmed.indexOf(':');
            return index <= 0 ? "***" : trimmed.substring(0, index) + ":****";
        }
        String[] parts = trimmed.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return "***";
    }
}
