package com.lebhas.creativesaas.storage.application.dto;

import com.lebhas.creativesaas.asset.domain.StorageProvider;
import com.lebhas.creativesaas.storage.domain.StorageClass;
import com.lebhas.creativesaas.storage.domain.StorageFilePurpose;

import java.time.Instant;
import java.util.UUID;

public record StorageFileView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        StorageProvider provider,
        String bucket,
        String objectKey,
        String cdnUrl,
        String mimeType,
        String fileExtension,
        long fileSize,
        String hash,
        Integer width,
        Integer height,
        Long duration,
        StorageClass storageClass,
        StorageFilePurpose filePurpose,
        Instant createdAt,
        Instant updatedAt
) {
}
