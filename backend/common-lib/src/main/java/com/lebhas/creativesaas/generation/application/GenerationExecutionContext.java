package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.creativerequest.application.PipelineResolutionContext;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;

public record GenerationExecutionContext(
        GenerationJobEntity job,
        CreativeRequestEntity request,
        PipelineResolutionContext pipeline
) {
}
