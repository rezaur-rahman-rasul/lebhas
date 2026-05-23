package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.CreativeReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface CreativeReviewCommentRepository extends JpaRepository<CreativeReviewCommentEntity, UUID> {

    List<CreativeReviewCommentEntity> findByWorkspaceIdAndApprovalIdAndDeletedFalseOrderByCreatedAtAsc(UUID workspaceId, UUID approvalId);
}
