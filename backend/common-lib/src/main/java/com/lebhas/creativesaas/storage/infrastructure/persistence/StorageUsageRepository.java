package com.lebhas.creativesaas.storage.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;

import java.util.Optional;
import java.util.UUID;

public interface StorageUsageRepository extends TenantAwareRepository<StorageUsageEntity> {

    Optional<StorageUsageEntity> findFirstByWorkspaceIdAndDeletedFalse(UUID workspaceId);

    Optional<StorageUsageEntity> findByWorkspaceIdAndDeletedFalse(UUID workspaceId);
}
