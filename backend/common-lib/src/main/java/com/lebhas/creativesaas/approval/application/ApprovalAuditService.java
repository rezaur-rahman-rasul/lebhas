package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalAuditLogView;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditLog;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@Deprecated(forRemoval = true)
public class ApprovalAuditService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalAuditService.class);

    private final ApprovalAuditLogRepository approvalAuditLogRepository;
    private final ApprovalMapper approvalMapper;

    public ApprovalAuditService(
            ApprovalAuditLogRepository approvalAuditLogRepository,
            ApprovalMapper approvalMapper
    ) {
        this.approvalAuditLogRepository = approvalAuditLogRepository;
        this.approvalMapper = approvalMapper;
    }

    @Transactional
    public ApprovalAuditLog record(
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID actorId,
            ApprovalAuditAction action,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus,
            String details
    ) {
        return record(
                null,
                workspaceId,
                approvalRequestId,
                generatedVersionId,
                actorId,
                action,
                previousStatus,
                newStatus,
                details);
    }

    @Transactional
    public ApprovalAuditLog record(
            String eventId,
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID actorId,
            ApprovalAuditAction action,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus,
            String details
    ) {
        if (StringUtils.hasText(eventId)) {
            return approvalAuditLogRepository.findByEventIdAndDeletedFalse(eventId)
                    .orElseGet(() -> save(eventId, workspaceId, approvalRequestId, generatedVersionId, actorId, action, previousStatus, newStatus, details));
        }
        return save(null, workspaceId, approvalRequestId, generatedVersionId, actorId, action, previousStatus, newStatus, details);
    }

    @Transactional(readOnly = true)
    public List<ApprovalAuditLogView> listByApprovalRequest(UUID workspaceId, UUID approvalRequestId) {
        return approvalAuditLogRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
                        workspaceId,
                        approvalRequestId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalAuditLogView> listByGeneratedVersion(UUID workspaceId, UUID generatedVersionId) {
        return approvalAuditLogRepository.findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtAsc(
                        workspaceId,
                        generatedVersionId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }

    private ApprovalAuditLog save(
            String eventId,
            UUID workspaceId,
            UUID approvalRequestId,
            UUID generatedVersionId,
            UUID actorId,
            ApprovalAuditAction action,
            ApprovalStatus previousStatus,
            ApprovalStatus newStatus,
            String details
    ) {
        ApprovalAuditLog auditLog = approvalAuditLogRepository.save(ApprovalAuditLog.create(
                eventId,
                workspaceId,
                approvalRequestId,
                generatedVersionId,
                actorId,
                action,
                previousStatus,
                newStatus,
                details));
        log.info("approval_audit workspaceId={} approvalRequestId={} generatedVersionId={} actorId={} action={} previousStatus={} newStatus={} eventId={}",
                workspaceId, approvalRequestId, generatedVersionId, actorId, action, previousStatus, newStatus, eventId);
        return auditLog;
    }
}
