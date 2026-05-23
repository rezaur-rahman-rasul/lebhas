package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.asset.application.SignedUrlService;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestQueryService;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GeneratedVersionQueryService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestQueryService creativeRequestQueryService;
    private final GeneratedVersionService generatedVersionService;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;
    private final AssetRepository assetRepository;
    private final SignedUrlService signedUrlService;

    public GeneratedVersionQueryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestQueryService creativeRequestQueryService,
            GeneratedVersionService generatedVersionService,
            GeneratedVersionViewMapper generatedVersionViewMapper,
            AssetRepository assetRepository,
            SignedUrlService signedUrlService
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestQueryService = creativeRequestQueryService;
        this.generatedVersionService = generatedVersionService;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
        this.assetRepository = assetRepository;
        this.signedUrlService = signedUrlService;
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionView> listByCreativeRequest(UUID workspaceId, UUID creativeRequestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        creativeRequestQueryService.requireAccessibleRequest(workspaceId, creativeRequestId, access);
        return generatedVersionService.listByCreativeRequest(workspaceId, creativeRequestId).stream()
                .map(generatedVersionViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneratedVersionView getById(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        GeneratedVersionEntity version = generatedVersionService.requireByIdAndWorkspaceId(workspaceId, generatedVersionId);
        creativeRequestQueryService.requireAccessibleRequest(workspaceId, version.getCreativeRequestId(), access);
        return generatedVersionViewMapper.toView(version);
    }

    @Transactional(readOnly = true)
    public AssetUrlView previewUrl(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        GeneratedVersionEntity version = generatedVersionService.requireByIdAndWorkspaceId(workspaceId, generatedVersionId);
        creativeRequestQueryService.requireAccessibleRequest(workspaceId, version.getCreativeRequestId(), access);
        AssetEntity asset = requirePreviewAsset(workspaceId, version);
        return signedUrlService.previewUrl(asset);
    }

    private AssetEntity requirePreviewAsset(UUID workspaceId, GeneratedVersionEntity version) {
        if (version.getAssetId() == null) {
            throw new BusinessException(ErrorCode.ASSET_PREVIEW_NOT_READY, "Generated version preview is not ready");
        }
        return assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(version.getAssetId(), workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }
}
