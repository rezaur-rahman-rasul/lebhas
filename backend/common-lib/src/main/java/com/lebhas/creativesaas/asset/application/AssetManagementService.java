package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.application.dto.UpdateAssetCommand;
import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.common.api.PagedResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AssetManagementService {

    private final AssetService assetService;

    public AssetManagementService(AssetService assetService) {
        this.assetService = assetService;
    }

    @Transactional
    public AssetView uploadAsset(UploadAssetCommand command) {
        return assetService.uploadAsset(command);
    }

    @Transactional(readOnly = true)
    public PagedResult<AssetView> listAssets(AssetListCriteria criteria) {
        return assetService.listAssets(criteria);
    }

    @Transactional(readOnly = true)
    public AssetView getAsset(UUID workspaceId, UUID assetId) {
        return assetService.getAsset(workspaceId, assetId);
    }

    @Transactional
    public AssetView updateAsset(UpdateAssetCommand command) {
        return assetService.updateAsset(command);
    }

    @Transactional
    public void deleteAsset(UUID workspaceId, UUID assetId) {
        assetService.deleteAsset(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetUrlView generatePreviewUrl(UUID workspaceId, UUID assetId) {
        return assetService.generatePreviewUrl(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetUrlView generateDownloadUrl(UUID workspaceId, UUID assetId) {
        return assetService.generateDownloadUrl(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAssetForSignedAccess(UUID assetId) {
        return assetService.requireAssetForSignedAccess(assetId);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAsset(UUID workspaceId, UUID assetId) {
        return assetService.requireAsset(workspaceId, assetId);
    }
}
