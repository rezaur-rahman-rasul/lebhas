package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.cache.ActivePipelineCacheEntry;
import com.lebhas.ai.cache.AiPipelineCacheService;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.CreativePipelineStatus;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PipelineResolver {

    private final WorkspacePlanContextService workspacePlanContextService;
    private final CreativePipelineRepository creativePipelineRepository;
    private final CreativePipelineLayerRepository creativePipelineLayerRepository;
    private final AiPipelineCacheService aiPipelineCacheService;

    public PipelineResolver(
            WorkspacePlanContextService workspacePlanContextService,
            CreativePipelineRepository creativePipelineRepository,
            CreativePipelineLayerRepository creativePipelineLayerRepository,
            AiPipelineCacheService aiPipelineCacheService
    ) {
        this.workspacePlanContextService = workspacePlanContextService;
        this.creativePipelineRepository = creativePipelineRepository;
        this.creativePipelineLayerRepository = creativePipelineLayerRepository;
        this.aiPipelineCacheService = aiPipelineCacheService;
    }

    @Transactional(readOnly = true)
    public PipelineResolutionContext resolve(UUID workspaceId) {
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        if (planContext.subscription() == null || planContext.pricingPlan() == null || planContext.featurePolicy() == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "An active workspace subscription and feature policy are required before generation orchestration");
        }

        CreativePipeline pipeline = resolveActivePipeline();
        List<CreativePipelineLayer> layers = creativePipelineLayerRepository
                .findAllByPipelineIdAndDeletedFalseOrderBySortOrderAsc(pipeline.getId())
                .stream()
                .filter(CreativePipelineLayer::isEnabled)
                .toList();
        if (layers.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Creative generation layers are not configured");
        }
        return new PipelineResolutionContext(planContext, pipeline, layers);
    }

    private CreativePipeline resolveActivePipeline() {
        return aiPipelineCacheService.getActivePipeline()
                .flatMap(entry -> creativePipelineRepository.findById(entry.pipelineId()))
                .filter(pipeline -> !pipeline.isDeleted() && pipeline.isActive() && pipeline.getStatus() == CreativePipelineStatus.ACTIVE)
                .orElseGet(() -> {
                    CreativePipeline pipeline = creativePipelineRepository
                            .findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus.ACTIVE)
                            .orElseThrow(() -> new BusinessException(
                                    ErrorCode.BUSINESS_RULE_VIOLATION,
                                    "An active creative pipeline is required before generation orchestration"));
                    aiPipelineCacheService.storeActivePipeline(new ActivePipelineCacheEntry(
                            pipeline.getId(),
                            pipeline.getPipelineCode(),
                            pipeline.getVersion(),
                            null));
                    return pipeline;
                });
    }
}
