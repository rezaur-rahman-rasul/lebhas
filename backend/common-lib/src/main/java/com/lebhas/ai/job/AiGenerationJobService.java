package com.lebhas.ai.job;

import com.lebhas.ai.config.AiProviderProperties;
import com.lebhas.ai.dto.AiGenerationRequest;
import com.lebhas.ai.dto.AiGenerationResponse;
import com.lebhas.ai.event.AiGenerationJobRequestedEvent;
import com.lebhas.ai.provider.AiProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiGenerationJobService {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationJobService.class);

    private final AiProviderRouter aiProviderRouter;
    private final AiProviderProperties properties;

    public AiGenerationJobService(AiProviderRouter aiProviderRouter, AiProviderProperties properties) {
        this.aiProviderRouter = aiProviderRouter;
        this.properties = properties;
    }

    public AiGenerationResponse execute(AiGenerationRequest request) {
        AiGenerationResponse response = aiProviderRouter.generate(request);
        if (!response.success() && properties.getJob().isEmitFailureMetadata()) {
            log.warn("ai_generation_job_failed provider={} model={} jobId={} creativeRequestId={} message={}",
                    response.providerName(),
                    response.model(),
                    request.jobId(),
                    request.creativeRequestId(),
                    response.message());
        }
        return response;
    }

    public AiGenerationJobRequestedEvent toRequestedEvent(AiGenerationRequest request) {
        return new AiGenerationJobRequestedEvent(
                request.workspaceId(),
                request.creativeRequestId(),
                request.jobId(),
                aiProviderRouter.activeProviderType(request),
                aiProviderRouter.plannedModelName(request),
                Instant.now());
    }

    public int maxAttempts() {
        return properties.getJob().getMaxAttempts();
    }

    public Map<String, Object> describeRouting(AiGenerationRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", aiProviderRouter.plannedProviderName(request));
        metadata.put("model", aiProviderRouter.plannedModelName(request));
        metadata.put("maxAttempts", properties.getJob().getMaxAttempts());
        return Map.copyOf(metadata);
    }
}
