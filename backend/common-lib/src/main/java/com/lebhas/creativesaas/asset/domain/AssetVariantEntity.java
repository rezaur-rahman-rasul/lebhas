package com.lebhas.creativesaas.asset.domain;

import com.lebhas.creativesaas.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "asset_variants",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asset_variants_asset_variant_type",
                columnNames = {"asset_id", "variant_type"}
        )
)
public class AssetVariantEntity extends TenantAwareEntity {

    @Column(name = "asset_id", nullable = false, updatable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 30)
    private AssetVariantType variantType;

    @Column(name = "storage_key", nullable = false, length = 600)
    private String storageKey;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    protected AssetVariantEntity() {
    }

    public static AssetVariantEntity create(
            UUID workspaceId,
            UUID assetId,
            AssetVariantType variantType,
            String storageKey,
            Integer width,
            Integer height,
            long fileSize,
            String mimeType
    ) {
        AssetVariantEntity entity = new AssetVariantEntity();
        entity.assignWorkspace(workspaceId);
        entity.assetId = require(assetId, "assetId");
        entity.variantType = variantType == null ? AssetVariantType.PROCESSED : variantType;
        entity.storageKey = normalizeRequired(storageKey, "storageKey");
        entity.width = width;
        entity.height = height;
        entity.fileSize = Math.max(fileSize, 0L);
        entity.mimeType = normalizeNullable(mimeType);
        return entity;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public AssetVariantType getVariantType() {
        return variantType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void updateMetadata(
            String storageKey,
            Integer width,
            Integer height,
            long fileSize,
            String mimeType
    ) {
        this.storageKey = normalizeRequired(storageKey, "storageKey");
        this.width = width;
        this.height = height;
        this.fileSize = Math.max(fileSize, 0L);
        this.mimeType = normalizeNullable(mimeType);
    }

    private static UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
