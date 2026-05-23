package com.lebhas.creativesaas.asset.storage;

public record StorageObjectResponse(
        com.lebhas.creativesaas.asset.domain.StorageProvider provider,
        String bucket,
        String objectKey,
        String fileName,
        String cdnUrl
) {
}
