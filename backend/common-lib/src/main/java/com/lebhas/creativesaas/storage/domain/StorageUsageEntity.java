package com.lebhas.creativesaas.storage.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "storage_usage",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_storage_usage_workspace",
                columnNames = {"workspace_id"}
        )
)
public class StorageUsageEntity extends TenantAwareEntity {

    @Column(name = "total_bytes_used", nullable = false)
    private long totalUsedBytes;

    @Column(name = "raw_asset_bytes", nullable = false)
    private long rawAssetBytes;

    @Column(name = "generated_asset_bytes", nullable = false)
    private long generatedAssetBytes;

    @Column(name = "variant_bytes", nullable = false)
    private long variantBytes;

    @Column(name = "deleted_bytes", nullable = false)
    private long deletedBytes;

    @Column(name = "last_calculated_at")
    private Instant lastCalculatedAt;

    protected StorageUsageEntity() {
    }

    public static StorageUsageEntity create(
            UUID workspaceId,
            long totalUsedBytes,
            long rawAssetBytes,
            long generatedAssetBytes,
            long variantBytes,
            long deletedBytes,
            Instant lastCalculatedAt
    ) {
        StorageUsageEntity entity = new StorageUsageEntity();
        entity.assignWorkspace(workspaceId);
        entity.totalUsedBytes = Math.max(totalUsedBytes, 0L);
        entity.rawAssetBytes = Math.max(rawAssetBytes, 0L);
        entity.generatedAssetBytes = Math.max(generatedAssetBytes, 0L);
        entity.variantBytes = Math.max(variantBytes, 0L);
        entity.deletedBytes = Math.max(deletedBytes, 0L);
        entity.lastCalculatedAt = lastCalculatedAt;
        return entity;
    }

    public long getTotalUsedBytes() {
        return totalUsedBytes;
    }

    public long getRawAssetBytes() {
        return rawAssetBytes;
    }

    public long getGeneratedAssetBytes() {
        return generatedAssetBytes;
    }

    public long getVariantBytes() {
        return variantBytes;
    }

    public long getDeletedBytes() {
        return deletedBytes;
    }

    public Instant getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void updateTotals(
            long totalUsedBytes,
            long rawAssetBytes,
            long generatedAssetBytes,
            long variantBytes,
            long deletedBytes,
            Instant lastCalculatedAt
    ) {
        this.totalUsedBytes = Math.max(totalUsedBytes, 0L);
        this.rawAssetBytes = Math.max(rawAssetBytes, 0L);
        this.generatedAssetBytes = Math.max(generatedAssetBytes, 0L);
        this.variantBytes = Math.max(variantBytes, 0L);
        this.deletedBytes = Math.max(deletedBytes, 0L);
        this.lastCalculatedAt = lastCalculatedAt;
    }
}
