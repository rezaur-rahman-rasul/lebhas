package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.asset.application.AssetFileValidationService;
import com.lebhas.creativesaas.asset.domain.AssetCategory;
import com.lebhas.creativesaas.asset.domain.AssetFileType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class StorageMetadataExtractor {

    private final AssetFileValidationService assetFileValidationService;

    public StorageMetadataExtractor(AssetFileValidationService assetFileValidationService) {
        this.assetFileValidationService = assetFileValidationService;
    }

    public ExtractedMetadata extract(MultipartFile file, AssetCategory assetCategory) {
        AssetFileValidationService.ValidatedAssetFile validatedFile = assetFileValidationService.validate(file, assetCategory);
        return new ExtractedMetadata(
                validatedFile.originalFileName(),
                validatedFile.sanitizedFileName(),
                validatedFile.extension(),
                validatedFile.mimeType(),
                validatedFile.size(),
                validatedFile.fileType(),
                sha256(file),
                validatedFile.width(),
                validatedFile.height(),
                validatedFile.duration());
    }

    private String sha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_HASH_FAILED, "Storage file hash could not be calculated");
        }
    }

    public record ExtractedMetadata(
            String originalFileName,
            String sanitizedFileName,
            String fileExtension,
            String mimeType,
            long fileSize,
            AssetFileType fileType,
            String sha256,
            Integer width,
            Integer height,
            Long duration
    ) {
    }
}
