package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.domain.UploadSessionStatus;
import com.lebhas.creativesaas.redis.RedisCacheService;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UploadChunkTracker {

    private static final Duration STATE_TTL = Duration.ofHours(24);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;

    public UploadChunkTracker(
            RedisCacheService redisCacheService,
            RedisKeyBuilder redisKeyBuilder
    ) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public UploadChunkState initialize(String uploadId, int chunkCount) {
        UploadChunkState state = new UploadChunkState(
                uploadId,
                Math.max(chunkCount, 1),
                Set.of(),
                0,
                UploadSessionStatus.PENDING.name(),
                Instant.now());
        redisCacheService.set(redisKeyBuilder.uploadChunk(uploadId), state, STATE_TTL);
        return state;
    }

    public UploadChunkState markChunkUploaded(String uploadId, int chunkIndex) {
        UploadChunkState current = get(uploadId).orElseGet(() -> initialize(uploadId, 1));
        Set<Integer> chunks = new LinkedHashSet<>(current.uploadedChunks());
        chunks.add(chunkIndex);
        int completionPercentage = Math.min(100, (int) Math.round((chunks.size() * 100.0d) / current.chunkCount()));
        UploadChunkState updated = new UploadChunkState(
                uploadId,
                current.chunkCount(),
                Set.copyOf(chunks),
                completionPercentage,
                completionPercentage >= 100 ? UploadSessionStatus.COMPLETED.name() : UploadSessionStatus.UPLOADING.name(),
                Instant.now());
        redisCacheService.set(redisKeyBuilder.uploadChunk(uploadId), updated, STATE_TTL);
        return updated;
    }

    public UploadChunkState markFailed(String uploadId) {
        UploadChunkState current = get(uploadId).orElseGet(() -> initialize(uploadId, 1));
        UploadChunkState updated = new UploadChunkState(
                uploadId,
                current.chunkCount(),
                current.uploadedChunks(),
                current.completionPercentage(),
                UploadSessionStatus.FAILED.name(),
                Instant.now());
        redisCacheService.set(redisKeyBuilder.uploadChunk(uploadId), updated, STATE_TTL);
        return updated;
    }

    public Optional<UploadChunkState> get(String uploadId) {
        return redisCacheService.get(redisKeyBuilder.uploadChunk(uploadId), UploadChunkState.class);
    }

    public record UploadChunkState(
            String uploadId,
            int chunkCount,
            Set<Integer> uploadedChunks,
            int completionPercentage,
            String uploadStatus,
            Instant updatedAt
    ) {
    }
}
