package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.jwt.JwtProperties;
import com.lebhas.creativesaas.common.security.session.OpaqueTokenService;
import com.lebhas.creativesaas.identity.domain.RefreshTokenEntity;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.infrastructure.persistence.RefreshTokenRepository;
import com.lebhas.creativesaas.redis.RedisSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final RedisSessionService redisSessionService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            OpaqueTokenService opaqueTokenService,
            JwtProperties jwtProperties,
            Clock clock,
            RedisSessionService redisSessionService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.opaqueTokenService = opaqueTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.redisSessionService = redisSessionService;
    }

    @Transactional
    public IssuedRefreshToken issue(
            UserEntity user,
            UUID workspaceId,
            String deviceId,
            String clientIp,
            String userAgent
    ) {
        return issue(user, workspaceId, normalizeDeviceId(deviceId), UUID.randomUUID(), clientIp, userAgent);
    }

    @Transactional
    public IssuedRefreshToken issue(
            UserEntity user,
            UUID workspaceId,
            String deviceId,
            UUID tokenFamilyId,
            String clientIp,
            String userAgent
    ) {
        OpaqueTokenService.IssuedOpaqueToken token = opaqueTokenService.issue();
        Instant expiresAt = clock.instant().plus(jwtProperties.getRefreshTokenTtl());
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        refreshTokenRepository.save(RefreshTokenEntity.issue(
                token.tokenId(),
                user.getId(),
                workspaceId,
                normalizedDeviceId,
                tokenFamilyId,
                token.hashedSecret(),
                expiresAt,
                clientIp,
                userAgent));
        redisSessionService.storeRefreshToken(
                token.tokenId().toString(),
                new RedisSessionService.RefreshTokenSession(
                        user.getId(),
                        workspaceId,
                        normalizedDeviceId,
                        tokenFamilyId.toString(),
                        token.hashedSecret(),
                        clientIp,
                        userAgent,
                        expiresAt),
                jwtProperties.getRefreshTokenTtl());
        redisSessionService.addTokenToRefreshFamily(
                user.getId(),
                token.tokenId().toString(),
                tokenFamilyId.toString(),
                jwtProperties.getRefreshTokenTtl());
        return new IssuedRefreshToken(token.value(), token.tokenId(), expiresAt, workspaceId, normalizedDeviceId, tokenFamilyId);
    }

    @Transactional(readOnly = true)
    public ValidatedRefreshToken validate(String rawToken, String clientIp, String userAgent) {
        OpaqueTokenService.ParsedOpaqueToken parsedToken = opaqueTokenService.parse(rawToken, ErrorCode.REFRESH_TOKEN_INVALID);
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByTokenIdAndDeletedFalse(parsedToken.tokenId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (!opaqueTokenService.matches(refreshToken.getTokenHash(), parsedToken.hashedSecret())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Instant now = clock.instant();
        if (refreshToken.isRevoked()) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }
        if (refreshToken.isExpired(now)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        if (refreshToken.getClientIp() != null
                && clientIp != null
                && !refreshToken.getClientIp().isBlank()
                && !refreshToken.getClientIp().equals(clientIp.trim())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token context does not match the original client");
        }
        if (refreshToken.getUserAgent() != null
                && userAgent != null
                && !refreshToken.getUserAgent().isBlank()
                && !refreshToken.getUserAgent().equals(userAgent)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID, "Refresh token context does not match the original client");
        }
        return new ValidatedRefreshToken(refreshToken);
    }

    @Transactional
    public IssuedRefreshToken rotate(ValidatedRefreshToken validatedRefreshToken, UserEntity user, String clientIp, String userAgent) {
        Instant now = clock.instant();
        validatedRefreshToken.refreshToken().markUsed(now);
        validatedRefreshToken.refreshToken().revoke(now);
        IssuedRefreshToken replacement = issue(
                user,
                validatedRefreshToken.refreshToken().getWorkspaceId(),
                validatedRefreshToken.refreshToken().getDeviceId(),
                validatedRefreshToken.refreshToken().getTokenFamilyId(),
                clientIp,
                userAgent);
        validatedRefreshToken.refreshToken().replaceWith(replacement.tokenId());
        refreshTokenRepository.save(validatedRefreshToken.refreshToken());
        redisSessionService.deleteRefreshToken(validatedRefreshToken.refreshToken().getTokenId().toString());
        return replacement;
    }

    @Transactional
    public void revokeSilently(String rawToken, UUID expectedUserId) {
        try {
            ValidatedRefreshToken validatedRefreshToken = validate(rawToken, null, null);
            if (!validatedRefreshToken.refreshToken().getUserId().equals(expectedUserId)) {
                return;
            }
            validatedRefreshToken.refreshToken().revoke(clock.instant());
            refreshTokenRepository.save(validatedRefreshToken.refreshToken());
            redisSessionService.deleteRefreshToken(validatedRefreshToken.refreshToken().getTokenId().toString());
        } catch (BusinessException ignored) {
        }
    }

    @Transactional
    public Set<String> revokeAllForUser(UUID userId) {
        List<RefreshTokenEntity> refreshTokens = refreshTokenRepository.findAllByUserIdAndDeletedFalse(userId);
        Instant now = clock.instant();
        refreshTokens.forEach(refreshToken -> {
            refreshToken.revoke(now);
            redisSessionService.deleteRefreshToken(refreshToken.getTokenId().toString());
        });
        refreshTokenRepository.saveAll(refreshTokens);
        redisSessionService.removeRefreshFamily(userId);
        return refreshTokens.stream().map(RefreshTokenEntity::getDeviceId).collect(Collectors.toSet());
    }

    @Transactional
    public Set<String> revokeDeviceSessions(UUID userId, String deviceId) {
        List<RefreshTokenEntity> refreshTokens =
                refreshTokenRepository.findAllByUserIdAndDeviceIdAndDeletedFalse(userId, normalizeDeviceId(deviceId));
        Instant now = clock.instant();
        refreshTokens.forEach(refreshToken -> {
            refreshToken.revoke(now);
            redisSessionService.deleteRefreshToken(refreshToken.getTokenId().toString());
        });
        refreshTokenRepository.saveAll(refreshTokens);
        return refreshTokens.stream().map(RefreshTokenEntity::getDeviceId).collect(Collectors.toSet());
    }

    private String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? "default-device" : deviceId.trim();
    }

    public record IssuedRefreshToken(
            String token,
            UUID tokenId,
            Instant expiresAt,
            UUID workspaceId,
            String deviceId,
            UUID tokenFamilyId
    ) {
    }

    public record ValidatedRefreshToken(RefreshTokenEntity refreshToken) {
    }
}
