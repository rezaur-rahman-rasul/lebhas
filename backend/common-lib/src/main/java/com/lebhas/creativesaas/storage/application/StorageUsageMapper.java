package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.storage.cache.dto.StorageUsageCacheEntry;
import com.lebhas.creativesaas.storage.domain.StorageUsageEntity;
import com.lebhas.creativesaas.storage.event.StorageUsageUpdatedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class StorageUsageMapper {

    public StorageUsageCacheEntry toCacheEntry(StorageUsageEntity entity, Instant cachedAt) {
        return new StorageUsageCacheEntry(
                entity.getWorkspaceId(),
                entity.getTotalUsedBytes(),
                entity.getRawAssetBytes(),
                entity.getGeneratedAssetBytes(),
                entity.getVariantBytes(),
                entity.getDeletedBytes(),
                entity.getLastCalculatedAt(),
                cachedAt == null ? Instant.now() : cachedAt);
    }

    public StorageUsageUpdatedEvent toUpdatedEvent(
            StorageUsageEntity entity,
            UUID assetId,
            Long storageLimitBytes,
            String reason,
            Instant occurredAt
    ) {
        return new StorageUsageUpdatedEvent(
                null,
                occurredAt,
                entity.getWorkspaceId(),
                entity.getId(),
                assetId,
                entity.getTotalUsedBytes(),
                storageLimitBytes,
                entity.getRawAssetBytes(),
                entity.getGeneratedAssetBytes(),
                entity.getVariantBytes(),
                entity.getDeletedBytes(),
                reason,
                entity.getLastCalculatedAt());
    }
}
