package com.lebhas.creativesaas.auditlog.application;

import com.lebhas.creativesaas.auditlog.infrastructure.persistence.AuditLogRepository;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AuditEventMapper auditEventMapper;

    public AuditQueryService(
            AuditLogRepository auditLogRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AuditEventMapper auditEventMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.auditEventMapper = auditEventMapper;
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listWorkspaceAuditLogs(UUID workspaceId, int limit) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return auditLogRepository.findAllByWorkspaceIdAndDeletedFalseOrderByAuditAtDesc(
                        workspaceId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(auditEventMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listActorAuditLogs(UUID workspaceId, UUID actorUserId, int limit) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return auditLogRepository.findAllByWorkspaceIdAndActorUserIdAndDeletedFalseOrderByAuditAtDesc(
                        workspaceId,
                        actorUserId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(auditEventMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listMasterAuditLogs(int limit) {
        return auditLogRepository.findAllByDeletedFalseOrderByAuditAtDesc(PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(auditEventMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listReferenceAuditLogs(UUID workspaceId, String entityType, UUID entityId, int limit) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return auditLogRepository.findAllByWorkspaceIdAndEntityTypeAndEntityIdAndDeletedFalseOrderByAuditAtDesc(
                        workspaceId,
                        entityType,
                        entityId,
                        PageRequest.of(0, normalizeLimit(limit)))
                .stream()
                .map(auditEventMapper::toView)
                .toList();
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
