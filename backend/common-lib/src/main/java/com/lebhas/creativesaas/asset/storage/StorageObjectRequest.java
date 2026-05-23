package com.lebhas.creativesaas.asset.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public record StorageObjectRequest(
        UUID workspaceId,
        UUID projectId,
        UUID assetId,
        String variantType,
        String fileName,
        String mimeType,
        long contentLength,
        String bucket,
        ContentStreamSupplier contentStreamSupplier
) {

    public StorageObjectRequest {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        if (assetId == null) {
            throw new IllegalArgumentException("assetId must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must not be negative");
        }
        if (contentStreamSupplier == null) {
            throw new IllegalArgumentException("contentStreamSupplier must not be null");
        }
    }

    @FunctionalInterface
    public interface ContentStreamSupplier {
        InputStream openStream() throws IOException;
    }
}
