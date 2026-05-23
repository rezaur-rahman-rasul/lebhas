package com.lebhas.creativesaas.asset.storage;

public interface StorageProvider {

    com.lebhas.creativesaas.asset.domain.StorageProvider providerType();

    StorageObjectResponse upload(StorageObjectRequest request);

    void delete(String bucket, String objectKey);

    SignedUrlResponse generateSignedUrl(SignedUrlRequest request);

    StorageService.StoredObjectMetadata getMetadata(String bucket, String objectKey);
}
