package com.lebhas.creativesaas.generation.cache;

import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GenerationJobCacheService {

    private final GenerationRedisKeys redisKeys;
    private final GenerationRedisAccessSupport redisAccessSupport;
    private final GenerationRedisTtlStrategy ttlStrategy;

    public GenerationJobCacheService(
            GenerationRedisKeys redisKeys,
            GenerationRedisAccessSupport redisAccessSupport,
            GenerationRedisTtlStrategy ttlStrategy
    ) {
        this.redisKeys = redisKeys;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<GenerationJobCacheEntry> get(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        return redisAccessSupport.read(
                redisKeys.generationJob(jobId),
                GenerationJobCacheEntry.class,
                "generation_job_get",
                GenerationRedisOperationContext.job(workspaceId, creativeRequestId, jobId));
    }

    public boolean store(GenerationJobEntity job) {
        return store(GenerationJobCacheEntry.from(job));
    }

    public boolean store(GenerationJobCacheEntry entry) {
        if (entry == null || entry.id() == null) {
            return false;
        }
        return redisAccessSupport.write(
                redisKeys.generationJob(entry.id()),
                entry,
                ttlStrategy.generationJobTtl(),
                "generation_job_put",
                GenerationRedisOperationContext.job(entry.workspaceId(), entry.creativeRequestId(), entry.id()));
    }

    public boolean invalidate(UUID workspaceId, UUID creativeRequestId, UUID jobId) {
        return redisAccessSupport.delete(
                redisKeys.generationJob(jobId),
                "generation_job_delete",
                GenerationRedisOperationContext.job(workspaceId, creativeRequestId, jobId));
    }

    public record GenerationJobCacheEntry(
            UUID id,
            UUID creativeRequestId,
            UUID workspaceId,
            GenerationJobStatus status,
            String provider,
            String model,
            Instant queuedAt,
            Instant processingStartedAt,
            Instant completedAt,
            Instant failedAt,
            int retryCount,
            String failureReason,
            Instant createdAt,
            Instant cachedAt
    ) {
        public static GenerationJobCacheEntry from(GenerationJobEntity job) {
            if (job == null) {
                return null;
            }
            return new GenerationJobCacheEntry(
                    job.getId(),
                    job.getCreativeRequestId(),
                    job.getWorkspaceId(),
                    job.getJobStatus(),
                    job.getProvider(),
                    job.getModel(),
                    job.getQueuedAt(),
                    job.getProcessingStartedAt(),
                    job.getCompletedAt(),
                    job.getFailedAt(),
                    job.getRetryCount(),
                    job.getFailureReason(),
                    job.getCreatedAt(),
                    Instant.now());
        }
    }
}
