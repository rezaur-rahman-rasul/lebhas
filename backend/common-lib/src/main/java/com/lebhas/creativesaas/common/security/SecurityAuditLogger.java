package com.lebhas.creativesaas.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

    public void logLoginAttempt(String email, UUID workspaceId, String clientIp) {
        log.info("auth_event type=login_attempt email={} workspaceId={} clientIp={}", email, workspaceId, clientIp);
    }

    public void logLoginSuccess(UUID userId, UUID workspaceId) {
        log.info("auth_event type=login_success userId={} workspaceId={}", userId, workspaceId);
    }

    public void logLoginSuccess(UUID userId, UUID workspaceId, String deviceId) {
        log.info("auth_event type=login_success userId={} workspaceId={} deviceId={}", userId, workspaceId, deviceId);
    }

    public void logMasterMobileBypassLogin(UUID userId, String mobileNumber, String clientIp, String deviceId) {
        log.info(
                "auth_event type=master_mobile_bypass_login userId={} mobileNumber={} clientIp={} deviceId={}",
                userId,
                mobileNumber,
                clientIp,
                deviceId);
    }

    public void logLoginFailure(String email, UUID workspaceId, String reason) {
        log.warn("auth_event type=login_failure email={} workspaceId={} reason={}", email, workspaceId, reason);
    }

    public void logTokenRefresh(UUID userId, UUID workspaceId) {
        log.info("auth_event type=token_refresh userId={} workspaceId={}", userId, workspaceId);
    }

    public void logTokenRefresh(UUID userId, UUID workspaceId, String deviceId) {
        log.info("auth_event type=token_refresh userId={} workspaceId={} deviceId={}", userId, workspaceId, deviceId);
    }

    public void logLogout(UUID userId, UUID workspaceId) {
        log.info("auth_event type=logout userId={} workspaceId={}", userId, workspaceId);
    }

    public void logLogout(UUID userId, UUID workspaceId, String deviceId, boolean logoutAllDevices) {
        log.info(
                "auth_event type=logout userId={} workspaceId={} deviceId={} logoutAllDevices={}",
                userId,
                workspaceId,
                deviceId,
                logoutAllDevices);
    }

    public void logRateLimitTriggered(String flow, String subject, String clientIp, String scope, long attempts) {
        log.warn(
                "auth_event type=rate_limit_triggered flow={} subject={} clientIp={} scope={} attempts={}",
                flow,
                subject,
                clientIp,
                scope,
                attempts);
    }

    public void logAccountLocked(UUID userId, String email, Instant lockedUntil) {
        log.warn("auth_event type=account_locked userId={} email={} lockedUntil={}", userId, email, lockedUntil);
    }

    public void logSecurityException(String type, String path, String reason) {
        log.warn("auth_event type={} path={} reason={}", type, path, reason);
    }

    public void logSupportModeEntered(UUID masterUserId, UUID workspaceId, String deviceId) {
        log.info(
                "auth_event type=support_mode_entered masterUserId={} workspaceId={} deviceId={}",
                masterUserId,
                workspaceId,
                deviceId);
    }

    public void logSupportModeExited(UUID masterUserId, UUID workspaceId, String deviceId) {
        log.info(
                "auth_event type=support_mode_exited masterUserId={} workspaceId={} deviceId={}",
                masterUserId,
                workspaceId,
                deviceId);
    }

    public void logKafkaPublishFailure(String eventType, UUID workspaceId, String reason) {
        log.warn("auth_event type=kafka_publish_failure eventType={} workspaceId={} reason={}", eventType, workspaceId, reason);
    }

    public void logRedisFailure(String operation, String reason) {
        log.warn("auth_event type=redis_failure operation={} reason={}", operation, reason);
    }
}
