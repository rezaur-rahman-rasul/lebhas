package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.WorkspaceAiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceAiUsageRepository extends JpaRepository<WorkspaceAiUsage, UUID> {

    Optional<WorkspaceAiUsage> findByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    List<WorkspaceAiUsage> findAllByDeletedFalse();
}
