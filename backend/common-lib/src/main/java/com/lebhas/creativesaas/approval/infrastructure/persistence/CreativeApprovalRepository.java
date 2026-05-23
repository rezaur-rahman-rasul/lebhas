package com.lebhas.creativesaas.approval.infrastructure.persistence;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalEntity;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Deprecated(forRemoval = true)
public interface CreativeApprovalRepository
        extends JpaRepository<CreativeApprovalEntity, UUID>, JpaSpecificationExecutor<CreativeApprovalEntity> {

    Optional<CreativeApprovalEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    boolean existsByWorkspaceIdAndCreativeOutputIdAndStatusInAndDeletedFalse(
            UUID workspaceId,
            UUID creativeOutputId,
            Collection<CreativeApprovalStatus> statuses
    );
}
