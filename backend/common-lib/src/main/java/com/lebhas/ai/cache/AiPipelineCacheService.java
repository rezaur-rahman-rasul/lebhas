package com.lebhas.ai.cache;

import com.lebhas.ai.application.dto.CreativePipelineLayerView;
import com.lebhas.ai.application.dto.CreativePipelineView;

import java.util.Optional;
import java.util.UUID;

public class AiPipelineCacheService {

    private final AiRedisAccessSupport redisAccessSupport;
    private final AiRedisTtlStrategy ttlStrategy;

    public AiPipelineCacheService(AiRedisAccessSupport redisAccessSupport, AiRedisTtlStrategy ttlStrategy) {
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<ActivePipelineCacheEntry> getActivePipeline() {
        return redisAccessSupport.read(
                AiRedisKeyConstants.activePipeline(),
                ActivePipelineCacheEntry.class,
                "active-pipeline-cache-read",
                null);
    }

    public boolean storeActivePipeline(ActivePipelineCacheEntry entry) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.activePipeline(),
                entry,
                ttlStrategy.activePipelineTtl(),
                "active-pipeline-cache-write",
                null);
    }

    public boolean invalidateActivePipeline() {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.activePipeline(),
                "active-pipeline-cache-delete",
                null);
    }

    public Optional<CreativePipelineView> getPipeline(UUID pipelineId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.pipeline(pipelineId),
                CreativePipelineView.class,
                "pipeline-cache-read",
                null);
    }

    public boolean storePipeline(CreativePipelineView pipeline) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.pipeline(pipeline.id()),
                pipeline,
                ttlStrategy.pipelineDefinitionTtl(),
                "pipeline-cache-write",
                null);
    }

    public boolean invalidatePipeline(UUID pipelineId) {
        return redisAccessSupport.delete(
                AiRedisKeyConstants.pipeline(pipelineId),
                "pipeline-cache-delete",
                null);
    }

    public Optional<CreativePipelineLayerView> getLayer(UUID layerId) {
        return redisAccessSupport.read(
                AiRedisKeyConstants.layer(layerId),
                CreativePipelineLayerView.class,
                "pipeline-layer-cache-read",
                null);
    }

    public boolean storeLayer(CreativePipelineLayerView layer) {
        return redisAccessSupport.write(
                AiRedisKeyConstants.layer(layer.id()),
                layer,
                ttlStrategy.layerDefinitionTtl(),
                "pipeline-layer-cache-write",
                null);
    }

    public boolean invalidateLayer(UUID layerId) {
        boolean layerDeleted = redisAccessSupport.delete(
                AiRedisKeyConstants.layer(layerId),
                "pipeline-layer-cache-delete",
                null);
        boolean mappingDeleted = redisAccessSupport.delete(
                AiRedisKeyConstants.layerMapping(layerId),
                "pipeline-layer-mapping-cache-delete",
                null);
        return layerDeleted && mappingDeleted;
    }
}
