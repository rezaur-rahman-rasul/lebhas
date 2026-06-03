package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.application.PipelineResolutionContext;
import com.lebhas.creativesaas.creativerequest.application.PipelineResolver;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import org.springframework.stereotype.Component;

@Component
public class GenerationExecutionContextFactory {

    private final CreativeRequestRepository creativeRequestRepository;
    private final PipelineResolver pipelineResolver;

    public GenerationExecutionContextFactory(
            CreativeRequestRepository creativeRequestRepository,
            PipelineResolver pipelineResolver
    ) {
        this.creativeRequestRepository = creativeRequestRepository;
        this.pipelineResolver = pipelineResolver;
    }

    public GenerationExecutionContext create(GenerationJobEntity job) {
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(job.getCreativeRequestId(), job.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        PipelineResolutionContext pipeline = pipelineResolver.resolve(job.getWorkspaceId());
        return new GenerationExecutionContext(job, request, pipeline);
    }
}
