package com.lebhas.creativesaas.project.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.project.domain.ProjectEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends TenantAwareRepository<ProjectEntity> {

    List<ProjectEntity> findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(UUID workspaceId);

    Optional<ProjectEntity> findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(UUID workspaceId);
}
