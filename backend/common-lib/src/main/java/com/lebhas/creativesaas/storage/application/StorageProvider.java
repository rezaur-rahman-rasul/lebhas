package com.lebhas.creativesaas.storage.application;

import com.lebhas.creativesaas.storage.config.StorageConfigProperties;

public interface StorageProvider {

    com.lebhas.creativesaas.asset.domain.StorageProvider providerType();

    String resolveBucket(StorageConfigProperties properties);

    String resolveCdnUrl(StorageConfigProperties properties, String objectKey);
}
