package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.asset.application.SignedUrlService;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.CreativeRequestQueryService;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
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
    public List<GeneratedVersionView> listByWorkspace(
            UUID workspaceId,
            String status,
            String creativeType,
            String platform,
            String search
    ) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        String normalizedStatus = normalizeFilter(status);
        String normalizedCreativeType = normalizeFilter(creativeType);
        String normalizedPlatform = normalizeFilter(platform);
        String normalizedSearch = normalizeSearch(search);

        return generatedVersionService.listByWorkspace(workspaceId).stream()
                .filter(version -> matchesStatus(version, normalizedStatus))
                .filter(version -> matchesCreativeType(version, normalizedCreativeType))
                .filter(version -> matchesPlatform(version, normalizedPlatform))
                .filter(version -> matchesSearch(version, normalizedSearch))
                .map(generatedVersionViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneratedVersionView getById(UUID workspaceId, UUID generatedVersionId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        GeneratedVersionEntity version = generatedVersionService.requireByIdAndWorkspaceId(workspaceId, generatedVersionId);
        requireAccessibleRequestIfPresent(workspaceId, version, access);
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

    private void requireAccessibleRequestIfPresent(
            UUID workspaceId,
            GeneratedVersionEntity version,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        if (safeRequest(version) != null) {
            creativeRequestQueryService.requireAccessibleRequest(workspaceId, version.getCreativeRequestId(), access);
        }
    }

    private boolean matchesStatus(GeneratedVersionEntity version, String status) {
        if (status == null) {
            return true;
        }
        return status.equals(version.getGenerationStatus().name())
                || status.equals(version.getApprovalStatus().name())
                || status.equals(version.getStatus().name())
                || ("PENDING_REVIEW".equals(status) && switch (version.getApprovalStatus()) {
                    case SUBMITTED, IN_REVIEW, RESUBMITTED -> true;
                    default -> false;
                })
                || ("COMPLETED".equals(status) && version.getGenerationStatus().isReady());
    }

    private boolean matchesCreativeType(GeneratedVersionEntity version, String creativeType) {
        if (creativeType == null) {
            return true;
        }
        CreativeRequestEntity request = safeRequest(version);
        return request != null
                && request.getCreativeType() != null
                && creativeType.equals(normalizeFilter(request.getCreativeType().name()));
    }

    private boolean matchesPlatform(GeneratedVersionEntity version, String platform) {
        if (platform == null) {
            return true;
        }
        CreativeRequestEntity request = safeRequest(version);
        return request != null && platform.equals(normalizeFilter(request.getTargetPlatform()));
    }

    private boolean matchesSearch(GeneratedVersionEntity version, String search) {
        if (search == null) {
            return true;
        }
        CreativeRequestEntity request = safeRequest(version);
        String haystack = String.join(" ",
                string(version.getId()),
                string(version.getCreativeRequestId()),
                string(version.getVersionName()),
                string(version.getFailureReason()),
                request == null ? "" : string(request.getRequestName()),
                request == null ? "" : string(request.getSourcePrompt()),
                request == null ? "" : string(request.getEnhancedPrompt()));
        return haystack.toLowerCase().contains(search);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }

    private String normalizeSearch(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private CreativeRequestEntity safeRequest(GeneratedVersionEntity version) {
        try {
            return version.getCreativeRequest();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
