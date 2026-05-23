package com.lebhas.creativesaas.storage.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;

import java.util.Optional;
import java.util.UUID;

public interface StorageFileRepository extends TenantAwareRepository<StorageFileEntity> {

    Optional<StorageFileEntity> findFirstByWorkspaceIdAndHashAndDeletedFalse(UUID workspaceId, String hash);

    Optional<StorageFileEntity> findFirstByWorkspaceIdAndObjectKeyAndDeletedFalse(UUID workspaceId, String objectKey);
}
