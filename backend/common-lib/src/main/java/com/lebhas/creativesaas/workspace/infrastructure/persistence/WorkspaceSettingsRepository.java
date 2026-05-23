package com.lebhas.creativesaas.workspace.infrastructure.persistence;

import com.lebhas.creativesaas.workspace.domain.WorkspaceSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkspaceSettingsRepository extends JpaRepository<WorkspaceSettingsEntity, UUID> {
}
