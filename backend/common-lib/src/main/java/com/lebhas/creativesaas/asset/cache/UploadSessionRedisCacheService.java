package com.lebhas.creativesaas.asset.cache;

import com.lebhas.creativesaas.asset.cache.dto.UploadSessionCacheEntry;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UploadSessionRedisCacheService {

    private final AssetRedisAccessSupport redisAccessSupport;
    private final AssetCacheTtlStrategy ttlStrategy;

    public UploadSessionRedisCacheService(
            AssetRedisAccessSupport redisAccessSupport,
            AssetCacheTtlStrategy ttlStrategy
    ) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public void cacheSession(UploadSessionEntity session) {
        redisAccessSupport.write(
                AssetCacheKeys.uploadSession(session.getId()),
                new UploadSessionCacheEntry(
                        session.getId(),
                        session.getWorkspaceId(),
                        session.getProjectId(),
                        session.getAssetId(),
                        session.getUploadedBy(),
                        session.getOriginalFileName(),
                        session.getMimeType(),
                        session.getFileSize(),
                        session.getHash(),
                        session.getChunkCount(),
                        session.getCompletedChunkCount(),
                        session.getStatus().name(),
                        session.getErrorMessage(),
                        session.getCreatedAt(),
                        session.getUpdatedAt()),
                ttlStrategy.uploadSessionTtl(),
                session.getWorkspaceId(),
                session.getAssetId());
    }

    public Optional<UploadSessionCacheEntry> get(UUID workspaceId, UUID uploadSessionId) {
        return redisAccessSupport.read(
                AssetCacheKeys.uploadSession(uploadSessionId),
                UploadSessionCacheEntry.class,
                workspaceId,
                null);
    }

    public void invalidate(UUID workspaceId, UUID uploadSessionId) {
        redisAccessSupport.delete(AssetCacheKeys.uploadSession(uploadSessionId), workspaceId, null);
    }
}
