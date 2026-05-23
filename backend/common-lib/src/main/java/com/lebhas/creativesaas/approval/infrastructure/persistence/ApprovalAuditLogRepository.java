package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalAuditLog;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface ApprovalAuditLogRepository extends TenantAwareRepository<ApprovalAuditLog> {

    Optional<ApprovalAuditLog> findByEventIdAndDeletedFalse(String eventId);

    List<ApprovalAuditLog> findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            UUID approvalRequestId
    );

    List<ApprovalAuditLog> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            UUID generatedVersionId
    );
}
