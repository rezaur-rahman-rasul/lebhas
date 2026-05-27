package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.session.AccessTokenRevocationStore;
import com.lebhas.creativesaas.identity.domain.RefreshTokenEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.RefreshTokenRepository;
import com.lebhas.creativesaas.profile.event.ProfileEventProducer;
import com.lebhas.creativesaas.redis.RedisRealtimeStateService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionRevocationService {

    private static final Logger log = LoggerFactory.getLogger(SessionRevocationService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisSessionService redisSessionService;
    private final RedisRealtimeStateService redisRealtimeStateService;
    private final AccessTokenRevocationStore accessTokenRevocationStore;
    private final Clock clock;
    private final ProfileEventProducer profileEventProducer;
    private final ProfileNotificationActivityAuditIntegration profileIntegration;

    public SessionRevocationService(
            RefreshTokenRepository refreshTokenRepository,
            RedisSessionService redisSessionService,
            RedisRealtimeStateService redisRealtimeStateService,
            AccessTokenRevocationStore accessTokenRevocationStore,
            Clock clock,
            ProfileEventProducer profileEventProducer,
            ProfileNotificationActivityAuditIntegration profileIntegration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.redisSessionService = redisSessionService;
        this.redisRealtimeStateService = redisRealtimeStateService;
        this.accessTokenRevocationStore = accessTokenRevocationStore;
        this.clock = clock;
        this.profileEventProducer = profileEventProducer;
        this.profileIntegration = profileIntegration;
    }

    @Transactional
    public RevocationResult revokeOtherSessions(CurrentUser currentUser) {
        String currentDeviceId = normalizeDeviceId(currentUser.deviceId());
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAllByUserIdAndDeletedFalse(currentUser.userId());
        Instant now = clock.instant();
        List<RefreshTokenEntity> revoked = tokens.stream()
                .filter(token -> !token.isRevoked())
                .filter(token -> !currentDeviceId.equals(normalizeDeviceId(token.getDeviceId())))
                .peek(token -> token.revoke(now))
                .toList();
        refreshTokenRepository.saveAll(revoked);
        Set<String> revokedDeviceIds = revoked.stream()
                .map(RefreshTokenEntity::getDeviceId)
                .map(this::normalizeDeviceId)
                .collect(Collectors.toSet());
        revokedDeviceIds.forEach(deviceId -> deleteSessionState(currentUser, deviceId));
        RevocationResult result = new RevocationResult(revoked.size(), revokedDeviceIds);
        publishSessionRevoked(currentUser, result, false);
        profileIntegration.sessionRevoked(currentUser, result, false);
        return result;
    }

    @Transactional
    public RevocationResult revokeCurrentSession(CurrentUser currentUser) {
        String currentDeviceId = normalizeDeviceId(currentUser.deviceId());
        RevocationResult result = revokeDeviceSessions(currentUser, currentDeviceId, true);
        publishSessionRevoked(currentUser, result, true);
        profileIntegration.sessionRevoked(currentUser, result, true);
        return result;
    }

    @Transactional
    public RevocationResult revokeSession(CurrentUser currentUser, String sessionId) {
        String deviceId = normalizeDeviceId(sessionId);
        boolean currentSessionIncluded = normalizeDeviceId(currentUser.deviceId()).equals(deviceId);
        RevocationResult result = revokeDeviceSessions(currentUser, deviceId, currentSessionIncluded);
        publishSessionRevoked(currentUser, result, currentSessionIncluded);
        profileIntegration.sessionRevoked(currentUser, result, currentSessionIncluded);
        return result;
    }

    private RevocationResult revokeDeviceSessions(CurrentUser currentUser, String currentDeviceId, boolean currentSessionIncluded) {
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findAllByUserIdAndDeviceIdAndDeletedFalse(
                currentUser.userId(),
                currentDeviceId);
        Instant now = clock.instant();
        tokens.stream().filter(token -> !token.isRevoked()).forEach(token -> token.revoke(now));
        refreshTokenRepository.saveAll(tokens);
        deleteSessionState(currentUser, currentDeviceId);
        if (currentSessionIncluded && currentUser.tokenId() != null && currentUser.accessTokenExpiresAt() != null) {
            accessTokenRevocationStore.revoke(currentUser.tokenId(), currentUser.accessTokenExpiresAt());
        }
        return new RevocationResult(tokens.size(), Set.of(currentDeviceId));
    }

    private void publishSessionRevoked(CurrentUser currentUser, RevocationResult result, boolean currentSessionIncluded) {
        if (result.revokedTokenCount() <= 0 && result.revokedDeviceIds().isEmpty()) {
            return;
        }
        profileEventProducer.profileSessionRevoked(
                currentUser.workspaceId(),
                currentUser.userId(),
                currentUser.userId(),
                result.revokedTokenCount(),
                result.revokedDeviceIds().size(),
                currentSessionIncluded);
    }

    private void deleteSessionState(CurrentUser currentUser, String deviceId) {
        try {
            redisSessionService.deleteUserSession(currentUser.userId(), deviceId);
            if (currentUser.workspaceId() != null) {
                redisRealtimeStateService.clearWorkspaceSession(currentUser.workspaceId(), currentUser.userId(), deviceId);
            }
        } catch (RuntimeException exception) {
            log.warn("profile_session_revoke_cache_cleanup_failed userId={} deviceId={} reason={}",
                    currentUser.userId(),
                    deviceId,
                    reason(exception));
        }
    }

    private String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "default-device" : deviceId.trim();
    }

    private static String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
    }

    public record RevocationResult(int revokedTokenCount, Set<String> revokedDeviceIds) {
    }
}
