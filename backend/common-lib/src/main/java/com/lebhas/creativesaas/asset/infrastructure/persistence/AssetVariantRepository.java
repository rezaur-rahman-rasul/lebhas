package com.lebhas.creativesaas.asset.infrastructure.persistence;

import com.lebhas.creativesaas.asset.domain.AssetVariantEntity;
import com.lebhas.creativesaas.asset.domain.AssetVariantType;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetVariantRepository extends TenantAwareRepository<AssetVariantEntity> {

    List<AssetVariantEntity> findAllByAssetIdAndDeletedFalse(UUID assetId);

    Optional<AssetVariantEntity> findByIdAndAssetIdAndDeletedFalse(UUID id, UUID assetId);

    Optional<AssetVariantEntity> findFirstByWorkspaceIdAndAssetIdAndVariantTypeAndDeletedFalse(
            UUID workspaceId,
            UUID assetId,
            AssetVariantType variantType
    );
}
