package com.lebhas.creativesaas.generation.infrastructure.persistence;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CreativeGenerationRequestRepository
        extends JpaRepository<CreativeGenerationRequestEntity, UUID>, JpaSpecificationExecutor<CreativeGenerationRequestEntity> {

    Optional<CreativeGenerationRequestEntity> findByIdAndWorkspaceIdAndDeletedFalse(UUID id, UUID workspaceId);

    long countByWorkspaceIdAndStatusAndDeletedFalse(UUID workspaceId, CreativeGenerationStatus status);
}
