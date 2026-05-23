package com.lebhas.creativesaas.asset.storage;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

@Deprecated(forRemoval = false)
public class CloudflareR2StorageProvider extends R2StorageProvider {

    public CloudflareR2StorageProvider(
            StorageProperties storageProperties,
            R2StorageProperties r2StorageProperties,
            StoragePathBuilder storagePathBuilder,
            S3Client s3Client,
            S3Presigner s3Presigner,
            Clock clock
    ) {
        super(storageProperties, r2StorageProperties, storagePathBuilder, s3Client, s3Presigner, clock);
    }
}
