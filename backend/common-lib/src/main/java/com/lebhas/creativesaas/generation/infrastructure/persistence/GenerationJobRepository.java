package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationJobRepository extends TenantAwareRepository<GenerationJobEntity> {

    Optional<GenerationJobEntity> findByIdAndDeletedFalse(UUID id);

    Optional<GenerationJobEntity> findFirstByRequestIdAndWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID requestId, UUID workspaceId);

    Optional<GenerationJobEntity> findFirstByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    List<GenerationJobEntity> findAllByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            UUID creativeRequestId
    );

    List<GenerationJobEntity> findAllByWorkspaceIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            UUID workspaceId,
            GenerationJobStatus status
    );
}
