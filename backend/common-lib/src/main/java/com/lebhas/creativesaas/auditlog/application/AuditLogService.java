package com.lebhas.creativesaas.auditlog.application;

import com.lebhas.creativesaas.auditlog.cache.AuditRecentCacheService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.auditlog.domain.AuditLog;
import com.lebhas.creativesaas.auditlog.infrastructure.persistence.AuditLogRepository;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditEventMapper auditEventMapper;
    private final CurrentUserContext currentUserContext;
    private AuditRecentCacheService auditRecentCacheService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            AuditEventMapper auditEventMapper,
            CurrentUserContext currentUserContext
    ) {
        this.auditLogRepository = auditLogRepository;
        this.auditEventMapper = auditEventMapper;
        this.currentUserContext = currentUserContext;
    }

    @Autowired(required = false)
    void setAuditRecentCacheService(AuditRecentCacheService auditRecentCacheService) {
        this.auditRecentCacheService = auditRecentCacheService;
    }

    @Transactional
    public Optional<AuditLogView> append(AuditLogCommand command) {
        if (auditLogRepository.existsBySourceEventIdAndDeletedFalse(command.sourceEventId())) {
            return Optional.empty();
        }
        AuditLog auditLog = auditEventMapper.toEntity(command);
        AuditLog saved = auditLogRepository.save(auditLog);
        invalidateAuditCache(saved.getWorkspaceId());
        return Optional.of(auditEventMapper.toView(saved));
    }

    @Transactional
    public Optional<AuditLogView> appendUserAction(
            UUID workspaceId,
            String sourceEventId,
            UUID actorUserId,
            AuditActionType actionType,
            AuditOutcome outcome,
            String entityType,
            UUID entityId,
            String summary,
            Map<String, ?> metadata,
            String ipAddress,
            String userAgent
    ) {
        return append(new AuditLogCommand(
                workspaceId,
                sourceEventId,
                actorUserId,
                actionType,
                outcome,
                entityType,
                entityId,
                summary,
                auditEventMapper.metadataJson(metadata),
                ipAddress,
                userAgent,
                Instant.now()));
    }

    @Transactional
    public Optional<AuditLogView> appendCurrentUserAction(
            UUID workspaceId,
            String sourceEventId,
            AuditActionType actionType,
            AuditOutcome outcome,
            String entityType,
            UUID entityId,
            String summary,
            Map<String, ?> metadata,
            String ipAddress,
        String userAgent
    ) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        UUID effectiveWorkspaceId = workspaceId == null ? currentUserContext.requireWorkspaceId() : workspaceId;
        return appendUserAction(
                effectiveWorkspaceId,
                sourceEventId,
                currentUser.userId(),
                actionType,
                outcome,
                entityType,
                entityId,
                summary,
                metadata,
                ipAddress,
                userAgent);
    }

    @Transactional
    public Optional<AuditLogView> appendCurrentPlatformAction(
            String sourceEventId,
            AuditActionType actionType,
            AuditOutcome outcome,
            String entityType,
            UUID entityId,
            String summary,
            Map<String, ?> metadata,
            String ipAddress,
            String userAgent
    ) {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        return appendUserAction(
                null,
                sourceEventId,
                currentUser.userId(),
                actionType,
                outcome,
                entityType,
                entityId,
                summary,
                metadata,
                ipAddress,
                userAgent);
    }

    private void invalidateAuditCache(UUID workspaceId) {
        if (workspaceId != null && auditRecentCacheService != null) {
            auditRecentCacheService.invalidateRecentAudit(workspaceId);
        }
    }
}
