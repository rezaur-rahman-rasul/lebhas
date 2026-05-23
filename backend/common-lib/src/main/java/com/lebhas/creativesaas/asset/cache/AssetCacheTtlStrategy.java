package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.storage.StorageProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AssetCacheTtlStrategy {

    private static final Duration MIN_EPHEMERAL_TTL = Duration.ofSeconds(5);
    private static final Duration ASSET_METADATA_TTL = Duration.ofMinutes(10);
    private static final Duration ASSET_LIST_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_EPHEMERAL_TTL = Duration.ofMinutes(30);
    private static final Duration HOT_ASSET_TTL = Duration.ofHours(6);
    private static final Duration WORKSPACE_STORAGE_TTL = Duration.ofMinutes(15);
    private static final Duration UPLOAD_SESSION_TTL = Duration.ofHours(24);
    private static final Duration TEMP_UPLOAD_STATE_TTL = Duration.ofHours(24);
    private static final Duration PROCESSING_STATE_TTL = Duration.ofHours(6);
    private static final Duration ASYNC_JOB_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    private final StorageProperties storageProperties;

    public AssetCacheTtlStrategy(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public Duration assetMetadataTtl() {
        return ASSET_METADATA_TTL;
    }

    public Duration assetListTtl() {
        return ASSET_LIST_TTL;
    }

    public Duration signedUrlTtl(Instant expiresAt) {
        return shortenBeforeExpiry(expiresAt, storageProperties.getSignedUrlTtl());
    }

    public Duration hotAssetTtl() {
        return HOT_ASSET_TTL;
    }

    public Duration workspaceStorageTtl() {
        return WORKSPACE_STORAGE_TTL;
    }

    public Duration uploadSessionTtl() {
        return UPLOAD_SESSION_TTL;
    }

    public Duration temporaryUploadStateTtl() {
        return TEMP_UPLOAD_STATE_TTL;
    }

    public Duration processingStateTtl() {
        return PROCESSING_STATE_TTL;
    }

    public Duration asyncJobTtl() {
        return ASYNC_JOB_TTL;
    }

    public Duration rateLimitWindow(Duration requested) {
        if (requested == null || requested.isNegative() || requested.isZero()) {
            return DEFAULT_RATE_LIMIT_WINDOW;
        }
        return requested;
    }

    private Duration shortenBeforeExpiry(Instant expiresAt, Duration fallbackTtl) {
        Duration baseline = fallbackTtl == null || fallbackTtl.isNegative() || fallbackTtl.isZero()
                ? DEFAULT_EPHEMERAL_TTL
                : fallbackTtl;
        Duration shorterThanExpiry = baseline.minusSeconds(30);
        if (shorterThanExpiry.isNegative() || shorterThanExpiry.isZero()) {
            shorterThanExpiry = MIN_EPHEMERAL_TTL;
        }
        if (expiresAt == null) {
            return shorterThanExpiry;
        }
        Duration untilExpiry = Duration.between(Instant.now(), expiresAt).minusSeconds(30);
        if (untilExpiry.isNegative() || untilExpiry.isZero()) {
            return MIN_EPHEMERAL_TTL;
        }
        return untilExpiry.compareTo(shorterThanExpiry) < 0 ? untilExpiry : shorterThanExpiry;
    }
}
