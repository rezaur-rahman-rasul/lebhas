package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.profile.cache.ProfileRedisKeys;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileImageUploadSessionService {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageUploadSessionService.class);
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final RedisCacheService redisCacheService;
    private final SecurityAuditLogger securityAuditLogger;

    public ProfileImageUploadSessionService(RedisCacheService redisCacheService, SecurityAuditLogger securityAuditLogger) {
        this.redisCacheService = redisCacheService;
        this.securityAuditLogger = securityAuditLogger;
    }

    public ProfileImageUploadSession create(
            UUID userId,
            String objectKey,
            String fileName,
            String mimeType,
            String extension,
            long fileSize,
            Instant expiresAt
    ) {
        UUID uploadReferenceId = UUID.randomUUID();
        ProfileImageUploadSession session = new ProfileImageUploadSession(
                uploadReferenceId,
                userId,
                objectKey,
                fileName,
                mimeType,
                extension,
                fileSize,
                Instant.now(),
                expiresAt);
        try {
            redisCacheService.set(ProfileRedisKeys.profileImageUploadSession(uploadReferenceId), session, ttl(expiresAt));
            return session;
        } catch (RuntimeException exception) {
            logFailure("profile_image_upload_session_create", uploadReferenceId, exception);
            throw new BusinessException(ErrorCode.REDIS_OPERATION_FAILED, "Profile image upload session could not be created");
        }
    }

    public ProfileImageUploadSession require(UUID uploadReferenceId, UUID expectedUserId) {
        if (uploadReferenceId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "uploadReferenceId is required");
        }
        Optional<ProfileImageUploadSession> session = read(uploadReferenceId);
        ProfileImageUploadSession resolved = session.orElseThrow(() ->
                new BusinessException(ErrorCode.UPLOAD_SESSION_NOT_FOUND, "Profile image upload session not found"));
        if (!resolved.userId().equals(expectedUserId) || resolved.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UPLOAD_SESSION_NOT_FOUND, "Profile image upload session not found");
        }
        return resolved;
    }

    public void delete(UUID uploadReferenceId) {
        if (uploadReferenceId == null) {
            return;
        }
        try {
            redisCacheService.delete(ProfileRedisKeys.profileImageUploadSession(uploadReferenceId));
        } catch (RuntimeException exception) {
            logFailure("profile_image_upload_session_delete", uploadReferenceId, exception);
        }
    }

    private Optional<ProfileImageUploadSession> read(UUID uploadReferenceId) {
        try {
            return redisCacheService.get(ProfileRedisKeys.profileImageUploadSession(uploadReferenceId), ProfileImageUploadSession.class);
        } catch (RuntimeException exception) {
            logFailure("profile_image_upload_session_read", uploadReferenceId, exception);
            return Optional.empty();
        }
    }

    private Duration ttl(Instant expiresAt) {
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            return DEFAULT_TTL;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        return ttl.isNegative() || ttl.isZero() ? DEFAULT_TTL : ttl;
    }

    private void logFailure(String operation, UUID uploadReferenceId, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("profile_image_upload_session_redis_failure operation={} uploadReferenceId={} reason={}",
                operation,
                uploadReferenceId,
                reason);
        securityAuditLogger.logRedisFailure(operation, reason);
    }

    public record ProfileImageUploadSession(
            UUID uploadReferenceId,
            UUID userId,
            String objectKey,
            String fileName,
            String mimeType,
            String extension,
            long fileSize,
            Instant createdAt,
            Instant expiresAt
    ) {
    }
}
