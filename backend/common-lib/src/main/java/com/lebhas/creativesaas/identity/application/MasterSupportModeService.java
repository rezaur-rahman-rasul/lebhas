package com.lebhas.creativesaas.identity.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.SecurityAuditLogger;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisPermissionCache;
import com.lebhas.creativesaas.redis.RedisPermissionVersionService;
import com.lebhas.creativesaas.redis.RedisSessionService;
import com.lebhas.creativesaas.redis.RedisWorkspaceContextCache;
import com.lebhas.creativesaas.workspace.application.dto.SupportModeView;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class MasterSupportModeService {

    private final CurrentUserContext currentUserContext;
    private final WorkspaceRepository workspaceRepository;
    private final RedisSessionService redisSessionService;
    private final RedisLockService redisLockService;
    private final RedisWorkspaceContextCache redisWorkspaceContextCache;
    private final RedisPermissionCache redisPermissionCache;
    private final RedisPermissionVersionService redisPermissionVersionService;
    private final DomainEventPublisher domainEventPublisher;
    private final SecurityAuditLogger securityAuditLogger;
    private final SessionProperties sessionProperties;
    private final Clock clock;

    public MasterSupportModeService(
            CurrentUserContext currentUserContext,
            WorkspaceRepository workspaceRepository,
            RedisSessionService redisSessionService,
            RedisLockService redisLockService,
            RedisWorkspaceContextCache redisWorkspaceContextCache,
            RedisPermissionCache redisPermissionCache,
            RedisPermissionVersionService redisPermissionVersionService,
            DomainEventPublisher domainEventPublisher,
            SecurityAuditLogger securityAuditLogger,
            SessionProperties sessionProperties,
            Clock clock
    ) {
        this.currentUserContext = currentUserContext;
        this.workspaceRepository = workspaceRepository;
        this.redisSessionService = redisSessionService;
        this.redisLockService = redisLockService;
        this.redisWorkspaceContextCache = redisWorkspaceContextCache;
        this.redisPermissionCache = redisPermissionCache;
        this.redisPermissionVersionService = redisPermissionVersionService;
        this.domainEventPublisher = domainEventPublisher;
        this.securityAuditLogger = securityAuditLogger;
        this.sessionProperties = sessionProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SupportModeView currentSupportMode() {
        CurrentUser currentUser = requireMaster();
        return redisSessionService.getSupportSession(currentUser.userId())
                .map(this::toView)
                .orElseGet(() -> new SupportModeView(currentUser.userId(), null, currentUser.deviceId(), false, null, null));
    }

    @Transactional
    public SupportModeView enterSupportMode(UUID workspaceId) {
        CurrentUser currentUser = requireMaster();
        WorkspaceEntity workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
        RedisLockService.RedisLockToken lockToken = redisLockService.acquire(
                        "lock:support:" + currentUser.userId(),
                        Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Support mode is already being updated"));
        try {
            Instant startedAt = clock.instant();
            Instant expiresAt = startedAt.plus(sessionProperties.getSupportModeTtl());
            String deviceId = currentUser.deviceId() == null ? "unknown" : currentUser.deviceId();
            RedisSessionService.SupportSession supportSession = new RedisSessionService.SupportSession(
                    currentUser.userId(),
                    workspace.getId(),
                    deviceId,
                    startedAt,
                    expiresAt);
            redisSessionService.storeSupportSession(currentUser.userId(), supportSession, sessionProperties.getSupportModeTtl());
            invalidateSupportCaches(workspace.getId(), currentUser.userId());
            securityAuditLogger.logSupportModeEntered(currentUser.userId(), workspace.getId(), deviceId);
            publishSafely(
                    KafkaTopicConstants.MASTER_SUPPORT_ENTERED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.MASTER_SUPPORT_ENTERED,
                            workspace.getId(),
                            currentUser.userId(),
                            startedAt,
                            Map.of(
                                    "masterUserId", currentUser.userId().toString(),
                                    "workspaceId", workspace.getId().toString(),
                                    "deviceId", deviceId)));
            return toView(supportSession);
        } finally {
            redisLockService.release(lockToken);
        }
    }

    @Transactional
    public SupportModeView exitSupportMode() {
        CurrentUser currentUser = requireMaster();
        RedisLockService.RedisLockToken lockToken = redisLockService.acquire(
                        "lock:support:" + currentUser.userId(),
                        Duration.ofSeconds(10))
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Support mode is already being updated"));
        try {
            RedisSessionService.SupportSession currentSession = redisSessionService.getSupportSession(currentUser.userId()).orElse(null);
            redisSessionService.deleteSupportSession(currentUser.userId());
            if (currentSession != null) {
                invalidateSupportCaches(currentSession.workspaceId(), currentUser.userId());
                securityAuditLogger.logSupportModeExited(currentUser.userId(), currentSession.workspaceId(), currentSession.deviceId());
                publishSafely(
                        KafkaTopicConstants.MASTER_SUPPORT_EXITED,
                        new BaseDomainEvent(
                                KafkaTopicConstants.MASTER_SUPPORT_EXITED,
                                currentSession.workspaceId(),
                                currentUser.userId(),
                                clock.instant(),
                                Map.of(
                                        "masterUserId", currentUser.userId().toString(),
                                        "workspaceId", currentSession.workspaceId().toString(),
                                        "deviceId", currentSession.deviceId())));
            }
            return new SupportModeView(currentUser.userId(), null, currentUser.deviceId(), false, null, null);
        } finally {
            redisLockService.release(lockToken);
        }
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }

    private void invalidateSupportCaches(UUID workspaceId, UUID masterUserId) {
        redisWorkspaceContextCache.invalidate(workspaceId, masterUserId);
        redisPermissionCache.invalidate(workspaceId, masterUserId);
        redisPermissionVersionService.increment(workspaceId);
    }

    private SupportModeView toView(RedisSessionService.SupportSession supportSession) {
        return new SupportModeView(
                supportSession.masterUserId(),
                supportSession.workspaceId(),
                supportSession.deviceId(),
                true,
                supportSession.startedAt(),
                supportSession.expiresAt());
    }

    private void publishSafely(String topic, BaseDomainEvent event) {
        try {
            domainEventPublisher.publish(topic, event);
        } catch (RuntimeException exception) {
            securityAuditLogger.logKafkaPublishFailure(event.getEventType(), event.getWorkspaceId(), exception.getMessage());
        }
    }
}
