package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface CreativeApprovalHistoryRepository extends JpaRepository<CreativeApprovalHistoryEntity, UUID> {

    List<CreativeApprovalHistoryEntity> findByWorkspaceIdAndApprovalIdOrderByCreatedAtAsc(UUID workspaceId, UUID approvalId);
}
