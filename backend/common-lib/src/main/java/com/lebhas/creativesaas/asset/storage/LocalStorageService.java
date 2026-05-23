package com.lebhas.creativesaas.asset.storage;

import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalStorageService implements StorageService, LocalAssetContentAccessor {

    private final StorageProperties storageProperties;
    private final StoragePathResolver storagePathResolver;
    private final LocalSignedAssetUrlService localSignedAssetUrlService;

    public LocalStorageService(
            StorageProperties storageProperties,
            StoragePathResolver storagePathResolver,
            LocalSignedAssetUrlService localSignedAssetUrlService
    ) {
        this.storageProperties = storageProperties;
        this.storagePathResolver = storagePathResolver;
        this.localSignedAssetUrlService = localSignedAssetUrlService;
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public StoredObject store(StorageUploadRequest request) {
        String storageKey = storagePathResolver.resolveRaw(
                request.workspaceId(),
                request.projectId(),
                request.assetId());
        Path root = storageProperties.getLocal().getRootPath().toAbsolutePath().normalize();
        Path targetFile = root.resolve(storageKey).normalize();
        if (!targetFile.startsWith(root)) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Resolved storage path is outside the configured root");
        }
        try {
            Files.createDirectories(targetFile.getParent());
            try (InputStream inputStream = request.file().getInputStream()) {
                Files.copy(inputStream, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredObject(
                    request.storedFileName(),
                    storageProperties.getBucket(),
                    storageKey,
                    null,
                    null,
                    null);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Local asset storage failed");
        }
    }

    @Override
    public StoredObject storeGenerated(GeneratedStorageUploadRequest request) {
        String storageKey = storagePathResolver.resolveGenerated(
                request.workspaceId(),
                request.projectId(),
                request.outputId());
        Path root = storageProperties.getLocal().getRootPath().toAbsolutePath().normalize();
        Path targetFile = root.resolve(storageKey).normalize();
        if (!targetFile.startsWith(root)) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Resolved storage path is outside the configured root");
        }
        try {
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, request.content());
            return new StoredObject(
                    request.outputId() + "." + request.fileExtension(),
                    storageProperties.getBucket(),
                    storageKey,
                    null,
                    null,
                    null);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Local generated creative storage failed");
        }
    }

    @Override
    public SignedAssetUrl generatePreviewUrl(AssetEntity asset) {
        return localSignedAssetUrlService.createUrl(asset.getId(), LocalAssetAccessMode.PREVIEW);
    }

    @Override
    public SignedAssetUrl generateDownloadUrl(AssetEntity asset) {
        return localSignedAssetUrlService.createUrl(asset.getId(), LocalAssetAccessMode.DOWNLOAD);
    }

    @Override
    public void delete(AssetEntity asset) {
        Path targetFile = resolvePath(asset);
        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Local asset deletion failed");
        }
    }

    @Override
    public StoredObjectMetadata getMetadata(AssetEntity asset) {
        Path targetFile = resolvePath(asset);
        try {
            if (!Files.exists(targetFile)) {
                throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
            }
            return new StoredObjectMetadata(
                    Files.size(targetFile),
                    Files.getLastModifiedTime(targetFile).toInstant());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Local asset metadata could not be read");
        }
    }

    @Override
    public Resource open(AssetEntity asset) {
        return new FileSystemResource(resolvePath(asset));
    }

    private Path resolvePath(AssetEntity asset) {
        Path root = storageProperties.getLocal().getRootPath().toAbsolutePath().normalize();
        Path targetFile = root.resolve(asset.getStorageKey()).normalize();
        if (!targetFile.startsWith(root)) {
            throw new BusinessException(ErrorCode.ASSET_STORAGE_FAILURE, "Resolved storage path is outside the configured root");
        }
        return targetFile;
    }
}
