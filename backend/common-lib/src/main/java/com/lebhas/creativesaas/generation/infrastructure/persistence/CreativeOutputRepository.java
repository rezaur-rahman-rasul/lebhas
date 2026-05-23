package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.generation.domain.CreativeOutputEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreativeOutputRepository extends JpaRepository<CreativeOutputEntity, UUID> {

    Optional<CreativeOutputEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    List<CreativeOutputEntity> findByWorkspaceIdAndRequestIdAndDeletedFalseOrderByCreatedAtAsc(UUID workspaceId, UUID requestId);
}
