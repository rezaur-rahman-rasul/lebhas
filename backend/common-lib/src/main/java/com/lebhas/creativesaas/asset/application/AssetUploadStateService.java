package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.cache.TemporaryUploadStateRedisCacheService;
import com.lebhas.creativesaas.asset.cache.dto.UploadProgressCacheEntry;
import com.lebhas.creativesaas.asset.cache.dto.UploadStateCacheEntry;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.asset.domain.UploadSessionStatus;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetUploadStateService {

    private static final Duration DEDUPE_TTL = Duration.ofHours(6);

    private final TemporaryUploadStateRedisCacheService temporaryUploadStateRedisCacheService;
    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public AssetUploadStateService(
            TemporaryUploadStateRedisCacheService temporaryUploadStateRedisCacheService,
            RedisCacheService redisCacheService,
            RedisKeyBuilder redisKeyBuilder
    ) {
        this.temporaryUploadStateRedisCacheService = temporaryUploadStateRedisCacheService;
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void initialize(UploadSessionEntity session) {
        String uploadId = session.getId().toString();
        temporaryUploadStateRedisCacheService.storeState(
                new UploadStateCacheEntry(
                        uploadId,
                        session.getId(),
                        session.getWorkspaceId(),
                        session.getProjectId(),
                        session.getAssetId(),
                        session.getUploadedBy(),
                        session.getHash(),
                        session.getFileSize(),
                        0L,
                        session.getStatus().name(),
                        Instant.now(),
                        Instant.now()));
        temporaryUploadStateRedisCacheService.storeProgress(
                session.getWorkspaceId(),
                session.getAssetId(),
                new UploadProgressCacheEntry(uploadId, 0, session.getStatus().name(), Instant.now()));
    }

    public void markUploading(UploadSessionEntity session) {
        update(session, 0L, UploadSessionStatus.UPLOADING);
    }

    public void markCompleted(UploadSessionEntity session, UUID assetId) {
        update(session, session.getFileSize(), UploadSessionStatus.COMPLETED, assetId);
    }

    public void markFailed(UploadSessionEntity session) {
        update(session, 0L, UploadSessionStatus.FAILED, session.getAssetId());
    }

    public Optional<UploadStateSnapshot> get(String uploadId) {
        return temporaryUploadStateRedisCacheService.getState(uploadId, null, null)
                .map(snapshot -> new UploadStateSnapshot(
                        snapshot.uploadId(),
                        snapshot.uploadSessionId(),
                        snapshot.workspaceId(),
                        snapshot.projectId(),
                        snapshot.assetId(),
                        snapshot.uploadedBy(),
                        snapshot.hash(),
                        snapshot.totalBytes(),
                        snapshot.uploadedBytes(),
                        snapshot.uploadStatus(),
                        snapshot.createdAt(),
                        snapshot.updatedAt()));
    }

    public Optional<UploadProgressSnapshot> getProgress(String uploadId) {
        return temporaryUploadStateRedisCacheService.getProgress(uploadId, null, null)
                .map(snapshot -> new UploadProgressSnapshot(
                        snapshot.uploadId(),
                        snapshot.progressPercentage(),
                        snapshot.uploadStatus(),
                        snapshot.updatedAt()));
    }

    public Optional<UUID> findDuplicate(String sha256) {
        DuplicateUploadSnapshot snapshot = redisCacheService.get(
                redisKeyBuilder.uploadDedupe(sha256),
                DuplicateUploadSnapshot.class).orElse(null);
        return snapshot == null ? Optional.empty() : Optional.of(snapshot.assetId());
    }

    public void rememberDuplicate(String sha256, UUID assetId, UUID workspaceId, UUID projectId) {
        redisCacheService.set(
                redisKeyBuilder.uploadDedupe(sha256),
                new DuplicateUploadSnapshot(assetId, workspaceId, projectId, Instant.now()),
                DEDUPE_TTL);
    }

    private void update(UploadSessionEntity session, long uploadedBytes, UploadSessionStatus status) {
        update(session, uploadedBytes, status, session.getAssetId());
    }

    private void update(UploadSessionEntity session, long uploadedBytes, UploadSessionStatus status, UUID assetId) {
        String uploadId = session.getId().toString();
        temporaryUploadStateRedisCacheService.storeState(
                new UploadStateCacheEntry(
                        uploadId,
                        session.getId(),
                        session.getWorkspaceId(),
                        session.getProjectId(),
                        assetId,
                        session.getUploadedBy(),
                        session.getHash(),
                        session.getFileSize(),
                        Math.min(uploadedBytes, session.getFileSize()),
                        status.name(),
                        session.getCreatedAt(),
                        Instant.now()));
        int percent = session.getFileSize() <= 0 ? 0 : (int) Math.min(100, Math.round((uploadedBytes * 100.0d) / session.getFileSize()));
        temporaryUploadStateRedisCacheService.storeProgress(
                session.getWorkspaceId(),
                assetId,
                new UploadProgressCacheEntry(uploadId, percent, status.name(), Instant.now()));
    }

    public record UploadStateSnapshot(
            String uploadId,
            UUID uploadSessionId,
            UUID workspaceId,
            UUID projectId,
            UUID assetId,
            UUID uploadedBy,
            String hash,
            long totalBytes,
            long uploadedBytes,
            String uploadStatus,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UploadProgressSnapshot(
            String uploadId,
            int progressPercentage,
            String uploadStatus,
            Instant updatedAt
    ) {
    }

    public record DuplicateUploadSnapshot(
            UUID assetId,
            UUID workspaceId,
            UUID projectId,
            Instant rememberedAt
    ) {
    }
}
