package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    List<ApprovalHistory> findAllByApprovalWorkflowIdOrderByCreatedAtAsc(UUID approvalWorkflowId);

    List<ApprovalHistory> findAllByApprovalWorkflowIdAndActionByOrderByCreatedAtAsc(UUID approvalWorkflowId, UUID actionBy);
}
