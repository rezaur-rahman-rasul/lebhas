package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.storage.application.dto.StorageFileView;
import com.lebhas.creativesaas.storage.config.StorageConfigProperties;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFileEntity;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;
import com.lebhas.creativesaas.storage.infrastructure.persistence.StorageFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class StorageFileService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final StorageFileRepository storageFileRepository;
    private final StorageFileViewMapper storageFileViewMapper;
    private final StorageConfigProperties storageConfigProperties;

    public StorageFileService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            StorageFileRepository storageFileRepository,
            StorageFileViewMapper storageFileViewMapper,
            StorageConfigProperties storageConfigProperties
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.storageFileRepository = storageFileRepository;
        this.storageFileViewMapper = storageFileViewMapper;
        this.storageConfigProperties = storageConfigProperties;
    }

    @Transactional(readOnly = true)
    public StorageFileView getStorageFile(UUID workspaceId, UUID fileId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return storageFileViewMapper.toView(requireStorageFile(workspaceId, fileId));
    }

    @Transactional(readOnly = true)
    public Optional<StorageFileEntity> findDuplicateByHash(UUID workspaceId, String hash) {
        if (!StringUtils.hasText(hash)) {
            return Optional.empty();
        }
        return storageFileRepository.findFirstByWorkspaceIdAndHashAndDeletedFalse(workspaceId, hash.trim().toLowerCase());
    }

    @Transactional
    public StorageFileEntity registerRawUpload(
            UUID workspaceId,
            UUID projectId,
            StorageProvider provider,
            String bucket,
            String objectKey,
            String cdnUrl,
            String mimeType,
            String fileExtension,
            long fileSize,
            Integer width,
            Integer height,
            Long duration,
            MultipartFile file
    ) {
        return register(
                workspaceId,
                projectId,
                provider,
                bucket,
                objectKey,
                cdnUrl,
                mimeType,
                fileExtension,
                fileSize,
                width,
                height,
                duration,
                StorageFilePurpose.RAW,
                digest(file));
    }

    @Transactional
    public StorageFileEntity registerRawUpload(
            UUID workspaceId,
            UUID projectId,
            StorageProvider provider,
            String bucket,
            String objectKey,
            String cdnUrl,
            StorageMetadataExtractor.ExtractedMetadata metadata
    ) {
        return register(
                workspaceId,
                projectId,
                provider,
                bucket,
                objectKey,
                cdnUrl,
                metadata.mimeType(),
                metadata.fileExtension(),
                metadata.fileSize(),
                metadata.width(),
                metadata.height(),
                metadata.duration(),
                StorageFilePurpose.RAW,
                metadata.sha256());
    }

    @Transactional
    public StorageFileEntity registerGeneratedOutput(
            UUID workspaceId,
            UUID projectId,
            StorageProvider provider,
            String bucket,
            String objectKey,
            String cdnUrl,
            String mimeType,
            String fileExtension,
            long fileSize,
            Integer width,
            Integer height,
            Long duration,
            byte[] content
    ) {
        return register(
                workspaceId,
                projectId,
                provider,
                bucket,
                objectKey,
                cdnUrl,
                mimeType,
                fileExtension,
                fileSize,
                width,
                height,
                duration,
                StorageFilePurpose.GENERATED,
                digest(content));
    }

    @Transactional(readOnly = true)
    public Optional<StorageFileEntity> findByObjectKey(UUID workspaceId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return Optional.empty();
        }
        return storageFileRepository.findFirstByWorkspaceIdAndObjectKeyAndDeletedFalse(workspaceId, objectKey.trim());
    }

    @Transactional(readOnly = true)
    public StorageFileEntity requireStorageFile(UUID workspaceId, UUID fileId) {
        return storageFileRepository.findByIdAndWorkspaceIdAndDeletedFalse(fileId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND));
    }

    private StorageFileEntity register(
            UUID workspaceId,
            UUID projectId,
            StorageProvider provider,
            String bucket,
            String objectKey,
            String cdnUrl,
            String mimeType,
            String fileExtension,
            long fileSize,
            Integer width,
            Integer height,
            Long duration,
            StorageFilePurpose purpose,
            String hash
    ) {
        StorageFileEntity file = StorageFileEntity.create(
                workspaceId,
                projectId,
                provider,
                resolveBucket(bucket),
                objectKey,
                resolveCdnUrl(cdnUrl, objectKey),
                mimeType,
                fileExtension,
                fileSize,
                hash,
                width,
                height,
                duration,
                StorageClass.STANDARD,
                purpose);
        return storageFileRepository.save(file);
    }

    private String resolveBucket(String bucket) {
        if (StringUtils.hasText(bucket)) {
            return bucket.trim();
        }
        return storageConfigProperties.getDefaultBucket();
    }

    private String resolveCdnUrl(String cdnUrl, String objectKey) {
        if (StringUtils.hasText(cdnUrl)) {
            return cdnUrl.trim();
        }
        if (!StringUtils.hasText(storageConfigProperties.getDefaultCdnBaseUrl())) {
            return null;
        }
        return storageConfigProperties.getDefaultCdnBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
    }

    private String digest(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return digest(inputStream);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_HASH_FAILED, "Storage file hash could not be calculated");
        }
    }

    private String digest(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_HASH_FAILED, "Storage file hash could not be calculated");
        }
    }

    private String digest(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_HASH_FAILED, "Storage file hash could not be calculated");
        }
    }
}
