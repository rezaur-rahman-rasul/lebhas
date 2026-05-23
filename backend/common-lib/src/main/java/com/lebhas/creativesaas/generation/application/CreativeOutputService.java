package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputUrlView;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputView;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeOutputEntity;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeGenerationRequestRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeOutputRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreativeOutputService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeOutputRepository outputRepository;
    private final CreativeGenerationRequestRepository requestRepository;
    private final AssetRepository assetRepository;
    private final StorageService storageService;
    private final CreativeGenerationViewMapper viewMapper;
    private final CreativeGenerationActivityLogger activityLogger;

    public CreativeOutputService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeOutputRepository outputRepository,
            CreativeGenerationRequestRepository requestRepository,
            AssetRepository assetRepository,
            StorageService storageService,
            CreativeGenerationViewMapper viewMapper,
            CreativeGenerationActivityLogger activityLogger
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.outputRepository = outputRepository;
        this.requestRepository = requestRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
        this.viewMapper = viewMapper;
        this.activityLogger = activityLogger;
    }

    @Transactional(readOnly = true)
    public CreativeOutputView get(UUID workspaceId, UUID outputId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        CreativeOutputEntity output = requireVisibleOutput(workspaceId, outputId, access);
        return viewMapper.toOutputView(output);
    }

    @Transactional(readOnly = true)
    public CreativeOutputUrlView previewUrl(UUID workspaceId, UUID outputId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        CreativeOutputEntity output = requireVisibleOutput(workspaceId, outputId, access);
        AssetEntity asset = requireGeneratedAsset(workspaceId, output);
        StorageService.SignedAssetUrl signedUrl = storageService.generatePreviewUrl(asset);
        return new CreativeOutputUrlView(signedUrl.url(), signedUrl.expiresAt());
    }

    @Transactional(readOnly = true)
    public CreativeOutputUrlView downloadUrl(UUID workspaceId, UUID outputId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
        CreativeOutputEntity output = requireVisibleOutput(workspaceId, outputId, access);
        AssetEntity asset = requireGeneratedAsset(workspaceId, output);
        StorageService.SignedAssetUrl signedUrl = storageService.generateDownloadUrl(asset);
        return new CreativeOutputUrlView(signedUrl.url(), signedUrl.expiresAt());
    }

    private CreativeOutputEntity requireVisibleOutput(
            UUID workspaceId,
            UUID outputId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        CreativeOutputEntity output = outputRepository.findByIdAndWorkspaceIdAndDeletedFalse(outputId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_OUTPUT_NOT_FOUND));
        CreativeGenerationRequestEntity request = requestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(output.getRequestId(), workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
        boolean privileged = access.effectiveRole().isMaster() || access.effectiveRole() == Role.ADMIN;
        if (!privileged && !request.getUserId().equals(access.currentUser().userId())) {
            activityLogger.logAuthorizationFailure(workspaceId, access.currentUser().userId(), "creative_output_owner_required");
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return output;
    }

    private AssetEntity requireGeneratedAsset(UUID workspaceId, CreativeOutputEntity output) {
        if (output.getGeneratedAssetId() == null) {
            throw new BusinessException(ErrorCode.CREATIVE_OUTPUT_NOT_FOUND);
        }
        return assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(output.getGeneratedAssetId(), workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }
}
