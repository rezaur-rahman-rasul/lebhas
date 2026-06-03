package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.application.dto.AssetListCriteria;
import com.lebhas.creativesaas.asset.application.dto.AssetUrlView;
import com.lebhas.creativesaas.asset.application.dto.AssetView;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetSpecifications;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetQueryService {

    private static final Logger log = LoggerFactory.getLogger(AssetQueryService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration STALE_UPLOAD_GRACE = Duration.ofMinutes(5);

    private final AssetValidationService assetValidationService;
    private final AssetRepository assetRepository;
    private final AssetCacheService assetCacheService;
    private final AssetMapper assetMapper;
    private final SignedUrlService signedUrlService;
    private final StorageService storageService;
    private final AssetActivityLogger assetActivityLogger;
    private final AssetEventPublisher assetEventPublisher;
    private final AssetHardDeleteService assetHardDeleteService;

    public AssetQueryService(
            AssetValidationService assetValidationService,
            AssetRepository assetRepository,
            AssetCacheService assetCacheService,
            AssetMapper assetMapper,
            SignedUrlService signedUrlService,
            StorageService storageService,
            AssetActivityLogger assetActivityLogger,
            AssetEventPublisher assetEventPublisher,
            AssetHardDeleteService assetHardDeleteService
    ) {
        this.assetValidationService = assetValidationService;
        this.assetRepository = assetRepository;
        this.assetCacheService = assetCacheService;
        this.assetMapper = assetMapper;
        this.signedUrlService = signedUrlService;
        this.storageService = storageService;
        this.assetActivityLogger = assetActivityLogger;
        this.assetEventPublisher = assetEventPublisher;
        this.assetHardDeleteService = assetHardDeleteService;
    }

    @Transactional
    public PagedResult<AssetView> listAssets(AssetListCriteria criteria) {
        WorkspaceAuthorizationService.WorkspaceAccess access = assetValidationService.requireViewAccess(criteria.workspaceId());
        if (criteria.projectId() != null) {
            assetValidationService.validateProjectContext(criteria.workspaceId(), criteria.projectId());
        }
        return assetCacheService.getOrLoadList(criteria, () -> {
            Pageable pageable = PageRequest.of(
                    Math.max(criteria.page(), 0),
                    Math.min(criteria.size() <= 0 ? DEFAULT_PAGE_SIZE : criteria.size(), MAX_PAGE_SIZE),
                    Sort.by(
                            criteria.sortDirection() == null ? Sort.Direction.DESC : criteria.sortDirection(),
                            resolveSortBy(criteria.sortBy())));
            Page<AssetEntity> page = assetRepository.findAll(AssetSpecifications.forList(criteria), pageable);
            if (page.isEmpty() && isDefaultWorkspaceList(criteria) && assetRepository.countByWorkspaceIdAndDeletedFalse(criteria.workspaceId()) > 0) {
                page = assetRepository.findAllByWorkspaceIdAndDeletedFalse(criteria.workspaceId(), pageable);
            }
            List<AssetEntity> visibleAssets = removeMissingStorageAssets(page.getContent(), access.currentUser().userId());
            return new PagedResult<>(
                    visibleAssets.stream().map(assetMapper::toAssetView).toList(),
                    Math.max(0L, page.getTotalElements() - (page.getContent().size() - visibleAssets.size())),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize(),
                    page.isFirst(),
                    page.isLast());
        });
    }

    @Transactional(readOnly = true)
    public AssetView getAsset(UUID workspaceId, UUID assetId) {
        assetValidationService.requireViewAccess(workspaceId);
        return assetCacheService.getOrLoadAsset(
                workspaceId,
                assetId,
                () -> assetMapper.toAssetView(assetValidationService.requireAsset(workspaceId, assetId)));
    }

    @Transactional
    public AssetUrlView generatePreviewUrl(UUID workspaceId, UUID assetId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = assetValidationService.requireViewAccess(workspaceId);
        AssetEntity asset = assetValidationService.requireAsset(workspaceId, assetId);
        ensureStorageObjectAvailable(asset, access.currentUser().userId());
        AssetUrlView urlView = signedUrlService.previewUrl(asset);
        assetActivityLogger.logSignedUrlGenerated(workspaceId, assetId, access.currentUser().userId(), "preview");
        return urlView;
    }

    @Transactional
    public AssetUrlView generateDownloadUrl(UUID workspaceId, UUID assetId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = assetValidationService.requireDownloadAccess(workspaceId);
        AssetEntity asset = assetValidationService.requireAsset(workspaceId, assetId);
        ensureStorageObjectAvailable(asset, access.currentUser().userId());
        assetActivityLogger.logDownloadRequested(workspaceId, assetId, access.currentUser().userId(), "download");
        assetEventPublisher.publish(
                KafkaTopicConstants.ASSET_DOWNLOAD_REQUESTED,
                workspaceId,
                assetId,
                Map.of(
                        "workspaceId", workspaceId.toString(),
                        "assetId", assetId.toString(),
                        "projectId", asset.getProjectId() == null ? "" : asset.getProjectId().toString(),
                        "actorUserId", access.currentUser().userId().toString(),
                        "permission", Permission.CREATIVE_DOWNLOAD.name()));
        AssetUrlView urlView = signedUrlService.downloadUrl(asset);
        assetActivityLogger.logSignedUrlGenerated(workspaceId, assetId, access.currentUser().userId(), "download");
        return urlView;
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAsset(UUID workspaceId, UUID assetId) {
        return assetValidationService.requireAsset(workspaceId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetEntity requireAssetForSignedAccess(UUID assetId) {
        return assetValidationService.requireAssetForSignedAccess(assetId);
    }

    private String resolveSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "createdAt";
        }
        return switch (sortBy.trim()) {
            case "displayName" -> "displayName";
            case "updatedAt" -> "updatedAt";
            default -> "createdAt";
        };
    }

    private boolean isDefaultWorkspaceList(AssetListCriteria criteria) {
        return criteria.projectId() == null
                && criteria.assetType() == null
                && criteria.assetCategory() == null
                && criteria.previewStatus() == null
                && criteria.processingStatus() == null
                && criteria.uploadedBy() == null
                && criteria.status() == null
                && !StringUtils.hasText(criteria.keyword())
                && criteria.createdFrom() == null
                && criteria.createdTo() == null;
    }

    private void ensureStorageObjectAvailable(AssetEntity asset, UUID actorUserId) {
        if (!StringUtils.hasText(asset.getStorageKey())) {
            markMissingStorageDeleted(asset, actorUserId);
            throw missingStorageObject(asset.getId());
        }
        try {
            storageService.getMetadata(asset);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.ASSET_STORAGE_FAILURE
                    || exception.getErrorCode() == ErrorCode.STORAGE_FILE_NOT_FOUND) {
                markMissingStorageDeleted(asset, actorUserId);
                throw missingStorageObject(asset.getId());
            }
            throw exception;
        }
    }

    private List<AssetEntity> removeMissingStorageAssets(List<AssetEntity> assets, UUID actorUserId) {
        List<AssetEntity> visibleAssets = new ArrayList<>(assets.size());
        for (AssetEntity asset : assets) {
            if (!shouldVerifyStorage(asset)) {
                visibleAssets.add(asset);
                continue;
            }
            if (!StringUtils.hasText(asset.getStorageKey())) {
                markMissingStorageDeleted(asset, actorUserId);
                continue;
            }
            try {
                storageService.getMetadata(asset);
                visibleAssets.add(asset);
            } catch (BusinessException exception) {
                if (exception.getErrorCode() == ErrorCode.ASSET_STORAGE_FAILURE
                        || exception.getErrorCode() == ErrorCode.STORAGE_FILE_NOT_FOUND
                        || isStaleUpload(asset)) {
                    markMissingStorageDeleted(asset, actorUserId);
                } else {
                    visibleAssets.add(asset);
                }
            }
        }
        return visibleAssets;
    }

    private boolean shouldVerifyStorage(AssetEntity asset) {
        if (asset.getStatus() == AssetStatus.GENERATED_METADATA_ONLY || asset.getStatus() == AssetStatus.DELETED) {
            return false;
        }
        return asset.getStatus() == AssetStatus.AVAILABLE
                || asset.getStatus() == AssetStatus.READY
                || asset.getStatus() == AssetStatus.FAILED
                || isStaleUpload(asset);
    }

    private boolean isStaleUpload(AssetEntity asset) {
        if (asset.getStatus() != AssetStatus.UPLOADING && asset.getStatus() != AssetStatus.UPLOAD_PENDING) {
            return false;
        }
        Instant createdAt = asset.getCreatedAt();
        return createdAt != null && createdAt.isBefore(Instant.now().minus(STALE_UPLOAD_GRACE));
    }

    private void markMissingStorageDeleted(AssetEntity asset, UUID actorUserId) {
        if (asset.getStatus() == AssetStatus.DELETED) {
            return;
        }
        assetHardDeleteService.deleteAssetRecordOnly(asset, actorUserId);
        log.warn("Asset hard-deleted because storage object is missing assetId={} workspaceId={} storageKey={}",
                asset.getId(),
                asset.getWorkspaceId(),
                asset.getStorageKey());
    }

    private BusinessException missingStorageObject(UUID assetId) {
        return new BusinessException(
                ErrorCode.STORAGE_FILE_NOT_FOUND,
                "Asset file is not available in storage. Re-upload or delete this asset.",
                List.of(ApiError.of(
                        "ASSET_STORAGE_OBJECT_MISSING",
                        "assetId",
                        "The asset database record exists, but the R2 object is missing for asset " + assetId)));
    }
}
