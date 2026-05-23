package com.lebhas.creativesaas.workspace.infrastructure.persistence;

import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrandProfileRepository extends JpaRepository<BrandProfileEntity, UUID> {

    Optional<BrandProfileEntity> findByWorkspaceIdAndDeletedFalse(UUID workspaceId);
}
