package com.lebhas.creativesaas.generatedversion.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionApprovalHistory;

import java.util.List;
import java.util.UUID;

public interface GeneratedVersionApprovalHistoryRepository extends TenantAwareRepository<GeneratedVersionApprovalHistory> {

    List<GeneratedVersionApprovalHistory> findAllByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtAsc(
            UUID workspaceId,
            UUID generatedVersionId
    );
}
