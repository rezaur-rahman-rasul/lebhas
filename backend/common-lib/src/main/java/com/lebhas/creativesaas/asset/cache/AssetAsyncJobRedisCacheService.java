package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.AsyncJobCoordinationCacheEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetAsyncJobRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public AssetAsyncJobRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public boolean queueJob(String jobId, String jobType, UUID workspaceId, UUID assetId, String coordinator) {
        String coordinationKey = AssetCacheKeys.asyncCoordination(assetId, jobType);
        boolean acquired = redisAccessSupport.putIfAbsent(
                coordinationKey,
                jobId,
                ttlStrategy.asyncJobTtl(),
                workspaceId,
                assetId);
        if (!acquired) {
            return false;
        }
        redisAccessSupport.write(
                AssetCacheKeys.asyncJob(jobId),
                new AsyncJobCoordinationCacheEntry(
                        jobId,
                        coordinationKey,
                        workspaceId,
                        assetId,
                        coordinator,
                        jobType,
                        "QUEUED",
                        1,
                        Instant.now(),
                        null,
                        null,
                        null),
                ttlStrategy.asyncJobTtl(),
                workspaceId,
                assetId);
        return true;
    }

    public void markRunning(String jobId, UUID workspaceId, UUID assetId) {
        AsyncJobCoordinationCacheEntry current = get(jobId, workspaceId, assetId).orElse(null);
        if (current == null) {
            return;
        }
        redisAccessSupport.write(
                AssetCacheKeys.asyncJob(jobId),
                new AsyncJobCoordinationCacheEntry(
                        current.jobId(),
                        current.coordinationKey(),
                        current.workspaceId(),
                        current.assetId(),
                        current.coordinator(),
                        current.jobType(),
                        "RUNNING",
                        current.attempts(),
                        current.queuedAt(),
                        Instant.now(),
                        null,
                        null),
                ttlStrategy.asyncJobTtl(),
                workspaceId,
                assetId);
    }

    public void markCompleted(String jobId, UUID workspaceId, UUID assetId) {
        AsyncJobCoordinationCacheEntry current = get(jobId, workspaceId, assetId).orElse(null);
        if (current == null) {
            return;
        }
        redisAccessSupport.write(
                AssetCacheKeys.asyncJob(jobId),
                new AsyncJobCoordinationCacheEntry(
                        current.jobId(),
                        current.coordinationKey(),
                        current.workspaceId(),
                        current.assetId(),
                        current.coordinator(),
                        current.jobType(),
                        "COMPLETED",
                        current.attempts(),
                        current.queuedAt(),
                        current.leasedAt(),
                        Instant.now(),
                        null),
                ttlStrategy.asyncJobTtl(),
                workspaceId,
                assetId);
        redisAccessSupport.delete(current.coordinationKey(), workspaceId, assetId);
    }

    public void markFailed(String jobId, UUID workspaceId, UUID assetId, String errorMessage) {
        AsyncJobCoordinationCacheEntry current = get(jobId, workspaceId, assetId).orElse(null);
        if (current == null) {
            return;
        }
        redisAccessSupport.write(
                AssetCacheKeys.asyncJob(jobId),
                new AsyncJobCoordinationCacheEntry(
                        current.jobId(),
                        current.coordinationKey(),
                        current.workspaceId(),
                        current.assetId(),
                        current.coordinator(),
                        current.jobType(),
                        "FAILED",
                        current.attempts(),
                        current.queuedAt(),
                        current.leasedAt(),
                        Instant.now(),
                        errorMessage),
                ttlStrategy.asyncJobTtl(),
                workspaceId,
                assetId);
        redisAccessSupport.delete(current.coordinationKey(), workspaceId, assetId);
    }

    public Optional<AsyncJobCoordinationCacheEntry> get(String jobId, UUID workspaceId, UUID assetId) {
        return redisAccessSupport.read(
                AssetCacheKeys.asyncJob(jobId),
                AsyncJobCoordinationCacheEntry.class,
                workspaceId,
                assetId);
    }

    public void invalidate(String jobId, UUID workspaceId, UUID assetId) {
        AsyncJobCoordinationCacheEntry current = get(jobId, workspaceId, assetId).orElse(null);
        redisAccessSupport.delete(AssetCacheKeys.asyncJob(jobId), workspaceId, assetId);
        if (current != null) {
            redisAccessSupport.delete(current.coordinationKey(), workspaceId, assetId);
        }
    }
}
