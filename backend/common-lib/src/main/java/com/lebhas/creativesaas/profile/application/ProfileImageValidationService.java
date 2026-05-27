package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProfileImageValidationService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> MIME_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final long maxFileSize;

    public ProfileImageValidationService(@Value("${platform.profile.image.max-size-bytes:5242880}") long maxFileSize) {
        this.maxFileSize = maxFileSize <= 0 ? 5 * 1024 * 1024L : maxFileSize;
    }

    public ValidatedProfileImageUpload validate(ProfileImageUploadUrlRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Profile image upload request is required");
        }
        String fileName = sanitizeFileName(request.fileName());
        String mimeType = normalizeMimeType(request.mimeType());
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw validation("mimeType", "Profile image type is not supported");
        }
        if (request.fileSize() <= 0 || request.fileSize() > maxFileSize) {
            throw validation("fileSize", "Profile image size exceeds the allowed limit");
        }
        String detectedExtension = extension(fileName);
        String storageExtension = MIME_EXTENSIONS.get(mimeType);
        if (!extensionMatches(detectedExtension, storageExtension)) {
            throw validation("fileName", "Profile image extension does not match the MIME type");
        }
        return new ValidatedProfileImageUpload(fileName, mimeType, request.fileSize(), storageExtension, maxFileSize);
    }

    public void validateStoredObject(long contentLength, long expectedFileSize) {
        if (contentLength <= 0) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Uploaded profile image is empty");
        }
        if (contentLength > maxFileSize || (expectedFileSize > 0 && contentLength != expectedFileSize)) {
            throw new BusinessException(ErrorCode.ASSET_METADATA_INVALID, "Uploaded profile image size is invalid");
        }
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw validation("fileName", "Profile image filename is required");
        }
        String sanitized = fileName.trim()
                .replace("\\", "/");
        int separator = sanitized.lastIndexOf('/');
        if (separator >= 0) {
            sanitized = sanitized.substring(separator + 1);
        }
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.isBlank() || sanitized.length() > 255 || sanitized.startsWith(".")) {
            throw validation("fileName", "Profile image filename is invalid");
        }
        return sanitized;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw validation("mimeType", "Profile image MIME type is required");
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private static String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw validation("fileName", "Profile image extension is required");
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean extensionMatches(String detectedExtension, String storageExtension) {
        if ("jpg".equals(storageExtension)) {
            return "jpg".equals(detectedExtension) || "jpeg".equals(detectedExtension);
        }
        return storageExtension.equals(detectedExtension);
    }

    private static BusinessException validation(String field, String message) {
        return new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                message,
                List.of(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), field, message)));
    }

    public record ValidatedProfileImageUpload(
            String fileName,
            String mimeType,
            long fileSize,
            String extension,
            long maxFileSize
    ) {
    }
}
