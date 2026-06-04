package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.UploadAssetCommand;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.asset.domain.AssetType;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.campaign.domain.ProjectCampaignEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.exception.TenantIsolationException;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.product.application.ProductServiceCatalogService;
import com.lebhas.creativesaas.product.domain.ProductServiceEntity;
import com.lebhas.creativesaas.storage.application.StorageMetadataExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AssetValidationService {

    private static final Set<AssetType> ALLOWED_UPLOAD_TYPES = EnumSet.of(
            AssetType.RAW_IMAGE,
            AssetType.GENERATED_CREATIVE,
            AssetType.BRAND_LOGO,
            AssetType.PRODUCT_IMAGE,
            AssetType.PACKAGING_IMAGE,
            AssetType.EXPORT_IMAGE,
            AssetType.EXPORT_VIDEO,
            AssetType.THUMBNAIL,
            AssetType.REFERENCE_ASSET
    );

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AssetRepository assetRepository;
    private final ProjectCampaignService projectCampaignService;
    private final ProductServiceCatalogService productServiceCatalogService;
    private final BrandService brandService;
    private final StorageMetadataExtractor storageMetadataExtractor;
    private final AssetActivityLogger assetActivityLogger;
    private final AssetServiceProperties assetServiceProperties;
    private final AssetStorageUsageService assetStorageUsageService;

    public AssetValidationService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AssetRepository assetRepository,
            ProjectCampaignService projectCampaignService,
            ProductServiceCatalogService productServiceCatalogService,
            BrandService brandService,
            StorageMetadataExtractor storageMetadataExtractor,
            AssetActivityLogger assetActivityLogger,
            AssetServiceProperties assetServiceProperties,
            AssetStorageUsageService assetStorageUsageService
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.assetRepository = assetRepository;
        this.projectCampaignService = projectCampaignService;
        this.productServiceCatalogService = productServiceCatalogService;
        this.brandService = brandService;
        this.storageMetadataExtractor = storageMetadataExtractor;
        this.assetActivityLogger = assetActivityLogger;
        this.assetServiceProperties = assetServiceProperties;
        this.assetStorageUsageService = assetStorageUsageService;
    }

    @Transactional(readOnly = true)
    public UploadValidationContext validateUpload(UploadAssetCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.ASSET_UPLOAD);
        CampaignContext campaignContext = command.projectId() == null
                ? new CampaignContext(null, null, null)
                : validateProjectContext(command.workspaceId(), command.projectId());
        validateUploadCount(command.workspaceId());
        StorageMetadataExtractor.ExtractedMetadata metadata = extractMetadata(command, access.currentUser().userId());
        AssetType assetType = resolveUploadAssetType(command.assetType(), command.assetCategory(), metadata.fileType());
        AssetCategory assetCategory = resolveAssetCategory(command.assetCategory(), assetType, metadata.fileType());
        return new UploadValidationContext(
                access,
                campaignContext,
                metadata,
                assetType,
                assetCategory,
                normalizeTags(command.tags()));
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireViewAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.ASSET_VIEW);
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireUpdateAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.ASSET_UPDATE);
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireDeleteAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.ASSET_DELETE);
    }

    @Transactional(readOnly = true)
    public WorkspaceAuthorizationService.WorkspaceAccess requireDownloadAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requirePermission(workspaceId, Permission.CREATIVE_DOWNLOAD);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAsset(UUID workspaceId, UUID assetId) {
        return assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(assetId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAssetForSignedAccess(UUID assetId) {
        AssetEntity asset = assetRepository.findByIdAndDeletedFalse(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
        if (!asset.isReady()) {
            throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
        }
        return asset;
    }

    public void validateOwnership(
            AssetEntity asset,
            WorkspaceAuthorizationService.WorkspaceAccess access,
            Permission delegatedPermission
    ) {
        if (!asset.getWorkspaceId().equals(access.workspace().getId())) {
            throw new TenantIsolationException(ErrorCode.WORKSPACE_ACCESS_DENIED.defaultMessage());
        }
        if (access.currentUser().isMaster()) {
            return;
        }
        if (asset.getUploadedBy() != null && asset.getUploadedBy().equals(access.currentUser().userId())) {
            return;
        }
        if (delegatedPermission != null && access.permissions().contains(delegatedPermission)) {
            return;
        }
        assetActivityLogger.logAccessDenied(access.workspace().getId(), access.currentUser().userId(), asset.getId(), "ownership_required");
        throw new BusinessException(ErrorCode.FORBIDDEN, "Asset ownership validation failed");
    }

    @Transactional(readOnly = true)
    public void validateWorkspaceQuota(UUID workspaceId, long incomingBytes) {
        assetStorageUsageService.validateWorkspaceQuota(workspaceId, incomingBytes, assetServiceProperties);
    }

    public Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            String value = tag.trim().toLowerCase();
            if (value.length() > 80) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Asset tags must be 80 characters or fewer");
            }
            normalized.add(value);
        }
        return Set.copyOf(normalized);
    }

    public AssetType resolveUploadAssetType(AssetType requestedType, AssetCategory requestedCategory, AssetFileType fileType) {
        AssetType normalized = requestedType == null ? deriveType(requestedCategory, fileType) : normalizeRequestedType(requestedType, fileType);
        if (!ALLOWED_UPLOAD_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Asset type is not allowed for upload");
        }
        validateAssetTypeCompatibility(normalized, fileType);
        return normalized;
    }

    public AssetCategory resolveAssetCategory(AssetCategory requestedCategory, AssetType assetType, AssetFileType fileType) {
        if (requestedCategory != null) {
            return requestedCategory;
        }
        return switch (assetType) {
            case BRAND_LOGO -> AssetCategory.BRAND_LOGO;
            case PRODUCT_IMAGE -> AssetCategory.PRODUCT_IMAGE;
            case PACKAGING_IMAGE -> AssetCategory.PACKAGING_IMAGE;
            case EXPORT_IMAGE -> AssetCategory.EXPORT_IMAGE;
            case EXPORT_VIDEO -> AssetCategory.EXPORT_VIDEO;
            case REFERENCE_ASSET -> fileType == AssetFileType.VIDEO ? AssetCategory.REFERENCE_VIDEO : AssetCategory.REFERENCE_ASSET;
            default -> fileType == AssetFileType.VIDEO ? AssetCategory.EXPORT_VIDEO : AssetCategory.OTHER;
        };
    }

    public boolean isGeneratedAsset(AssetType assetType) {
        return assetType == AssetType.GENERATED || assetType == AssetType.GENERATED_CREATIVE;
    }

    @Transactional(readOnly = true)
    public CampaignContext validateProjectContext(UUID workspaceId, UUID projectId) {
        ProjectCampaignEntity campaign = projectCampaignService.requireProjectCampaign(workspaceId, projectId);
        ProductServiceEntity productService = productServiceCatalogService.requireProductService(workspaceId, campaign.getProductServiceId());
        if (!campaign.getBrandId().equals(productService.getBrandId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Project campaign relationship is inconsistent");
        }
        brandService.requireBrand(workspaceId, campaign.getBrandId());
        return new CampaignContext(campaign.getBrandId(), productService.getId(), campaign.getId());
    }

    private void validateUploadCount(UUID workspaceId) {
        if (!assetServiceProperties.isUploadCountLimited()) {
            return;
        }
        long currentAssetCount = assetRepository.countByWorkspaceIdAndDeletedFalse(workspaceId);
        if (currentAssetCount >= assetServiceProperties.getMaxUploadCountPerWorkspace()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Workspace upload count limit has been reached");
        }
    }

    private StorageMetadataExtractor.ExtractedMetadata extractMetadata(
            UploadAssetCommand command,
            UUID actorUserId
    ) {
        try {
            return storageMetadataExtractor.extract(command.file(), command.assetCategory());
        } catch (BusinessException exception) {
            assetActivityLogger.logValidationFailure(command.workspaceId(), actorUserId, exception.getMessage());
            throw exception;
        }
    }

    private AssetType deriveType(AssetCategory requestedCategory, AssetFileType fileType) {
        if (requestedCategory == null) {
            return fileType == AssetFileType.VIDEO ? AssetType.EXPORT_VIDEO : AssetType.RAW_IMAGE;
        }
        return switch (requestedCategory) {
            case BRAND_LOGO -> AssetType.BRAND_LOGO;
            case PRODUCT_IMAGE -> AssetType.PRODUCT_IMAGE;
            case PACKAGING_IMAGE -> AssetType.PACKAGING_IMAGE;
            case EXPORT_IMAGE -> AssetType.EXPORT_IMAGE;
            case PRODUCT_VIDEO, EXPORT_VIDEO -> AssetType.EXPORT_VIDEO;
            case REFERENCE_IMAGE, REFERENCE_VIDEO, REFERENCE_ASSET -> AssetType.REFERENCE_ASSET;
            default -> fileType == AssetFileType.VIDEO ? AssetType.EXPORT_VIDEO : AssetType.RAW_IMAGE;
        };
    }

    private AssetType normalizeRequestedType(AssetType requestedType, AssetFileType fileType) {
        return switch (requestedType) {
            case RAW -> AssetType.RAW_IMAGE;
            case GENERATED -> AssetType.GENERATED_CREATIVE;
            case PROCESSED -> fileType == AssetFileType.VIDEO ? AssetType.EXPORT_VIDEO : AssetType.EXPORT_IMAGE;
            default -> requestedType;
        };
    }

    private void validateAssetTypeCompatibility(AssetType assetType, AssetFileType fileType) {
        switch (assetType) {
            case BRAND_LOGO -> {
                if (fileType != AssetFileType.IMAGE && fileType != AssetFileType.VECTOR_IMAGE) {
                    throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Brand logos must be image or vector assets");
                }
            }
            case PRODUCT_IMAGE, PACKAGING_IMAGE, EXPORT_IMAGE, RAW_IMAGE, THUMBNAIL -> {
                if (fileType != AssetFileType.IMAGE) {
                    throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Requested asset type requires an image upload");
                }
            }
            case EXPORT_VIDEO -> {
                if (fileType != AssetFileType.VIDEO) {
                    throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Requested asset type requires a video upload");
                }
            }
            case GENERATED_CREATIVE, REFERENCE_ASSET -> {
                if (fileType == null) {
                    throw new BusinessException(ErrorCode.ASSET_FILE_TYPE_INVALID, "Asset file type could not be resolved");
                }
            }
            default -> {
            }
        }
    }

    public record CampaignContext(UUID brandId, UUID productServiceId, UUID projectId) {
    }

    public record UploadValidationContext(
            WorkspaceAuthorizationService.WorkspaceAccess access,
            CampaignContext campaignContext,
            StorageMetadataExtractor.ExtractedMetadata metadata,
            AssetType assetType,
            AssetCategory assetCategory,
            Set<String> tags
    ) {
    }
}
