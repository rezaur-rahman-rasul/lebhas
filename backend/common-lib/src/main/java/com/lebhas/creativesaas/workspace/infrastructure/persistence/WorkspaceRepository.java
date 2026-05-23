package com.lebhas.creativesaas.workspace.infrastructure.persistence;

import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, UUID> {

    Optional<WorkspaceEntity> findByIdAndDeletedFalse(UUID id);

    Optional<WorkspaceEntity> findBySlugIgnoreCaseAndDeletedFalse(String slug);

    boolean existsBySlugIgnoreCaseAndDeletedFalse(String slug);

    List<WorkspaceEntity> findAllByDeletedFalseOrderByCreatedAtDesc();
}
