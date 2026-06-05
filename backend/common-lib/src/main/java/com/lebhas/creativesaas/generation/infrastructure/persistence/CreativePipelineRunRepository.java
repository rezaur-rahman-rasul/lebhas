package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.generation.domain.CreativePipelineRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreativePipelineRunRepository extends JpaRepository<CreativePipelineRunEntity, UUID> {

    Optional<CreativePipelineRunEntity> findFirstByCreativeRequestIdAndWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID creativeRequestId, UUID workspaceId);

    Optional<CreativePipelineRunEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);
}
