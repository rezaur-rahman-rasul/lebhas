package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.AssetProcessingStateCacheEntry;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import com.lebhas.creativesaas.asset.domain.ProcessingStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetProcessingRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetProcessingRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public void markPending(UUID workspaceId, UUID assetId) {
        store(workspaceId, assetId, ProcessingStatus.PENDING, PreviewStatus.PENDING, false, false, null, null, null, Instant.now(), null);
    }

    public void markProcessing(UUID workspaceId, UUID assetId) {
        AssetProcessingStateCacheEntry current = get(workspaceId, assetId).orElse(null);
        store(
                workspaceId,
                assetId,
                ProcessingStatus.PROCESSING,
                PreviewStatus.PROCESSING,
                false,
                current != null && current.thumbnailReady(),
                current == null ? null : current.coordinator(),
                current == null ? null : current.jobId(),
                null,
                current == null ? Instant.now() : current.startedAt(),
                null);
    }

    public void markReady(UUID workspaceId, UUID assetId, boolean thumbnailReady) {
        AssetProcessingStateCacheEntry current = get(workspaceId, assetId).orElse(null);
        store(
                workspaceId,
                assetId,
                ProcessingStatus.READY,
                PreviewStatus.READY,
                true,
                thumbnailReady,
                current == null ? null : current.coordinator(),
                current == null ? null : current.jobId(),
                null,
                current == null ? Instant.now() : current.startedAt(),
                Instant.now());
    }

    public void markFailed(UUID workspaceId, UUID assetId, String errorMessage) {
        AssetProcessingStateCacheEntry current = get(workspaceId, assetId).orElse(null);
        store(
                workspaceId,
                assetId,
                ProcessingStatus.FAILED,
                PreviewStatus.FAILED,
                false,
                false,
                current == null ? null : current.coordinator(),
                current == null ? null : current.jobId(),
                errorMessage,
                current == null ? Instant.now() : current.startedAt(),
                Instant.now());
    }

    public Optional<AssetProcessingStateCacheEntry> get(UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(
                AssetCacheKeys.assetProcessing(assetId),
                AssetProcessingStateCacheEntry.class,
                workspaceId,
                assetId);
    }

    public void invalidate(UUID workspaceId, UUID assetId) {
        redisAccessSupport.delete(AssetCacheKeys.assetProcessing(assetId), workspaceId, assetId);
    }

    private void store(
            UUID workspaceId,
            UUID assetId,
            ProcessingStatus processingStatus,
            PreviewStatus previewStatus,
            boolean previewReady,
            boolean thumbnailReady,
            String coordinator,
            String jobId,
            String errorMessage,
            Instant startedAt,
            Instant completedAt
    ) {
        redisAccessSupport.write(
                AssetCacheKeys.assetProcessing(assetId),
                new AssetProcessingStateCacheEntry(
                        assetId,
                        workspaceId,
                        processingStatus.name(),
                        previewStatus.name(),
                        previewReady,
                        thumbnailReady,
                        coordinator,
                        jobId,
                        errorMessage,
                        startedAt,
                        completedAt,
                        Instant.now()),
                ttlStrategy.processingStateTtl(),
                workspaceId,
                assetId);
    }
}
