package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.cache.GenerationJobCacheService;
import com.lebhas.creativesaas.generation.application.dto.GenerationJobView;
import com.lebhas.creativesaas.generation.application.dto.GenerationJobDetailView;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionViewMapper;
import com.lebhas.creativesaas.generatedversion.application.GeneratedVersionService;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.generation.event.GenerationJobQueuedEventDto;
import com.lebhas.creativesaas.generation.infrastructure.persistence.GenerationJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class GenerationJobService {

    private final GenerationJobRepository generationJobRepository;
    private final GenerationJobCacheService generationJobCacheService;
    private final GenerationEventProducer generationEventProducer;
    private final GeneratedVersionService generatedVersionService;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;

    public GenerationJobService(
            GenerationJobRepository generationJobRepository,
            GenerationJobCacheService generationJobCacheService,
            GenerationEventProducer generationEventProducer,
            GeneratedVersionService generatedVersionService,
            GeneratedVersionViewMapper generatedVersionViewMapper
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generationJobCacheService = generationJobCacheService;
        this.generationEventProducer = generationEventProducer;
        this.generatedVersionService = generatedVersionService;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
    }

    @Transactional
    public GenerationJobEntity queue(CreativeRequestEntity request, String queueName) {
        return queue(request, queueName, request.getRequestedVersions());
    }

    @Transactional
    public GenerationJobEntity queue(CreativeRequestEntity request, String queueName, int maxAttempts) {
        GenerationJobEntity job = GenerationJobEntity.queue(
                request.getWorkspaceId(),
                request.getId(),
                null,
                queueName,
                maxAttempts);
        job = generationJobRepository.save(job);
        generationJobCacheService.store(job);
        generationEventProducer.publishJobQueued(new GenerationJobQueuedEventDto(
                job.getWorkspaceId(),
                job.getCreativeRequestId(),
                job.getId(),
                request.getCreditReservationId(),
                job.getQueueName(),
                Instant.now()));
        return job;
    }

    @Transactional
    public GenerationJobEntity start(UUID workspaceId, UUID jobId) {
        GenerationJobEntity job = require(workspaceId, jobId);
        job.markStarted();
        job = generationJobRepository.save(job);
        generationJobCacheService.store(job);
        return job;
    }

    @Transactional
    public GenerationJobEntity complete(UUID workspaceId, UUID jobId, String providerJobId) {
        GenerationJobEntity job = require(workspaceId, jobId);
        job.markCompleted(job.getProvider(), job.getModel(), providerJobId);
        job = generationJobRepository.save(job);
        generationJobCacheService.store(job);
        return job;
    }

    @Transactional
    public GenerationJobEntity fail(UUID workspaceId, UUID jobId, String failureReason) {
        GenerationJobEntity job = require(workspaceId, jobId);
        job.markFailed(failureReason);
        job = generationJobRepository.save(job);
        generationJobCacheService.store(job);
        return job;
    }

    @Transactional(readOnly = true)
    public GenerationJobEntity require(UUID workspaceId, UUID jobId) {
        return generationJobRepository.findByIdAndDeletedFalse(jobId)
                .filter(job -> workspaceId.equals(job.getWorkspaceId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_JOB_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public GenerationJobView getJob(UUID workspaceId, UUID jobId) {
        GenerationJobEntity job = require(workspaceId, jobId);
        return toView(job);
    }

    @Transactional(readOnly = true)
    public GenerationJobDetailView getJobDetail(UUID workspaceId, UUID jobId) {
        GenerationJobEntity job = require(workspaceId, jobId);
        return new GenerationJobDetailView(
                toView(job),
                generatedVersionService.listByCreativeRequest(workspaceId, job.getCreativeRequestId())
                        .stream()
                        .map(generatedVersionViewMapper::toView)
                        .toList());
    }

    private GenerationJobView toView(GenerationJobEntity job) {
        return new GenerationJobView(
                job.getId(),
                job.getWorkspaceId(),
                job.getRequestId(),
                job.getJobType(),
                job.getStatus(),
                job.getProviderJobId(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getQueueName(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getFailedAt(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

}
