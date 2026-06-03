package com.lebhas.creativesaas.asset.storage;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("assetStoragePathBuilder")
public class StoragePathBuilder {

    public String buildAssetPath(UUID workspaceId, UUID projectId, UUID assetId, String filename) {
        return "workspaces/%s/projects/%s/assets/%s".formatted(
                require(workspaceId, "workspaceId"),
                require(projectId, "projectId"),
                objectName(assetId, filename));
    }

    public String buildWorkspaceAssetPath(UUID workspaceId, UUID assetId, String filename) {
        return "workspaces/%s/assets/%s".formatted(
                require(workspaceId, "workspaceId"),
                objectName(assetId, filename));
    }

    public String buildProfilePath(UUID userId, UUID assetId, String filename) {
        return "profiles/%s/%s".formatted(
                require(userId, "userId"),
                objectName(assetId, filename));
    }

    public String buildVariantPath(UUID workspaceId, UUID assetId, String variantType, String filename) {
        return "variants/workspaces/%s/assets/%s/%s/%s".formatted(
                require(workspaceId, "workspaceId"),
                require(assetId, "assetId"),
                sanitizeSegment(variantType, "variantType"),
                sanitizeFilename(filename));
    }

    private UUID require(UUID value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private String sanitizeFilename(String value) {
        String sanitized = sanitizeSegment(value, "filename")
                .replace("/", "-")
                .replace("\\", "-");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("filename must not be blank");
        }
        return sanitized;
    }

    private String objectName(UUID assetId, String filename) {
        String sanitized = sanitizeFilename(filename);
        int dotIndex = sanitized.lastIndexOf('.');
        String extension = dotIndex >= 0 ? sanitized.substring(dotIndex) : "";
        return require(assetId, "assetId") + extension;
    }

    private String sanitizeSegment(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim()
                .replaceAll("[\\r\\n]+", "-")
                .replaceAll("\\s+", "-");
    }
}
