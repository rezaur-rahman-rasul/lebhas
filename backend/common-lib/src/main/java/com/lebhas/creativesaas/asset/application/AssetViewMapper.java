package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetFolderView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class AssetViewMapper {

    private final AssetMapper assetMapper;

    public AssetViewMapper(AssetMapper assetMapper) {
        this.assetMapper = assetMapper;
    }

    public AssetView toAssetView(AssetEntity asset) {
        return assetMapper.toAssetView(asset);
    }

    public AssetFolderView toFolderView(AssetFolderEntity folder) {
        return new AssetFolderView(
                folder.getId(),
                folder.getWorkspaceId(),
                folder.getName(),
                folder.getParentFolderId(),
                folder.getDescription(),
                folder.getCreatedBy(),
                folder.getCreatedAt(),
                folder.getUpdatedAt());
    }
}
