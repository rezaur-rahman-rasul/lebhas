package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationRequestView;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputView;
import com.lebhas.creativesaas.generation.application.dto.GenerationJobView;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeOutputEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import org.springframework.stereotype.Component;

@Component
public class CreativeGenerationViewMapper {

    private final GenerationJsonCodec jsonCodec;

    public CreativeGenerationViewMapper(GenerationJsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    public CreativeGenerationRequestView toRequestView(CreativeGenerationRequestEntity request) {
        return new CreativeGenerationRequestView(
                request.getId(),
                request.getWorkspaceId(),
                request.getUserId(),
                request.getPromptHistoryId(),
                request.getSourcePrompt(),
                request.getEnhancedPrompt(),
                request.getPlatform(),
                request.getCampaignObjective(),
                request.getCreativeType(),
                request.getOutputFormat(),
                request.getLanguage(),
                jsonCodec.readMap(request.getBrandContextSnapshot()),
                jsonCodec.readListOfMaps(request.getAssetContextSnapshot()),
                jsonCodec.readMap(request.getGenerationConfig()),
                request.getStatus(),
                request.getAiProvider(),
                request.getAiModel(),
                request.getRequestedAt(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getFailedAt(),
                request.getErrorMessage(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public CreativeOutputView toOutputView(CreativeOutputEntity output) {
        return new CreativeOutputView(
                output.getId(),
                output.getWorkspaceId(),
                output.getRequestId(),
                output.getGeneratedAssetId(),
                output.getCreativeType(),
                output.getPlatform(),
                output.getOutputFormat(),
                output.getWidth(),
                output.getHeight(),
                output.getDuration(),
                output.getFileSize(),
                output.getPreviewUrl(),
                output.getDownloadUrl(),
                output.getCaption(),
                output.getHeadline(),
                output.getCtaText(),
                jsonCodec.readMap(output.getMetadata()),
                output.getStatus(),
                output.getCreatedAt(),
                output.getUpdatedAt());
    }

    public GenerationJobView toJobView(GenerationJobEntity job) {
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
