package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CreativePipelineCommand;
import com.lebhas.ai.application.dto.CreativePipelineLayerCommand;
import com.lebhas.ai.application.dto.CreativePipelineLayerView;
import com.lebhas.ai.application.dto.CreativePipelineView;
import com.lebhas.ai.application.dto.LayerCostPolicyCommand;
import com.lebhas.ai.application.dto.LayerQualityPolicyCommand;
import com.lebhas.ai.application.dto.LayerRoutingPolicyCommand;
import com.lebhas.ai.application.dto.LayerToolMappingCommand;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CreativePipeline;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.CreativePipelineStatus;
import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.domain.LayerQualityPolicy;
import com.lebhas.ai.domain.LayerRoutingPolicy;
import com.lebhas.ai.domain.LayerRoutingStrategy;
import com.lebhas.ai.domain.LayerToolMapping;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolCapabilityRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineLayerRepository;
import com.lebhas.ai.infrastructure.persistence.CreativePipelineRepository;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerRoutingPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
public class MasterCreativePipelineManagementService {

    private static final String FALLBACK_POLICY_CODE = "FALLBACK";

    private final CreativePipelineRepository pipelineRepository;
    private final CreativePipelineLayerRepository layerRepository;
    private final LayerToolMappingRepository toolMappingRepository;
    private final LayerRoutingPolicyRepository routingPolicyRepository;
    private final LayerCostPolicyRepository costPolicyRepository;
    private final LayerQualityPolicyRepository qualityPolicyRepository;
    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final AiToolCapabilityRepository capabilityRepository;
    private final CreativePipelineMapper mapper;

    public MasterCreativePipelineManagementService(
            CreativePipelineRepository pipelineRepository,
            CreativePipelineLayerRepository layerRepository,
            LayerToolMappingRepository toolMappingRepository,
            LayerRoutingPolicyRepository routingPolicyRepository,
            LayerCostPolicyRepository costPolicyRepository,
            LayerQualityPolicyRepository qualityPolicyRepository,
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            AiToolCapabilityRepository capabilityRepository,
            CreativePipelineMapper mapper
    ) {
        this.pipelineRepository = pipelineRepository;
        this.layerRepository = layerRepository;
        this.toolMappingRepository = toolMappingRepository;
        this.routingPolicyRepository = routingPolicyRepository;
        this.costPolicyRepository = costPolicyRepository;
        this.qualityPolicyRepository = qualityPolicyRepository;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.capabilityRepository = capabilityRepository;
        this.mapper = mapper;
    }

    public CreativePipelineView createPipeline(CreativePipelineCommand command) {
        if (pipelineRepository.existsByPipelineCodeAndDeletedFalse(AiToolProvider.normalizeCode(command.pipelineCode(), "pipelineCode"))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Creative pipeline code already exists");
        }
        CreativePipeline pipeline = CreativePipeline.create(
                command.pipelineCode(),
                command.pipelineName(),
                command.description(),
                command.status(),
                command.active(),
                command.version(),
                command.metadata());
        if (pipeline.isActive()) {
            deactivateActivePipeline();
        }
        return pipelineView(pipelineRepository.save(pipeline));
    }

    public CreativePipelineView updatePipeline(UUID pipelineId, CreativePipelineCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        pipeline.update(
                command.pipelineName(),
                command.description(),
                command.status(),
                command.active(),
                command.version(),
                command.metadata());
        if (pipeline.isActive()) {
            deactivateActivePipelineExcept(pipeline.getId());
        }
        return pipelineView(pipelineRepository.save(pipeline));
    }

    public CreativePipelineView disablePipeline(UUID pipelineId) {
        CreativePipeline pipeline = pipeline(pipelineId);
        pipeline.deactivate();
        return pipelineView(pipelineRepository.save(pipeline));
    }

    public CreativePipelineView createLayer(UUID pipelineId, CreativePipelineLayerCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        layerRepository.findByPipelineIdAndLayerTypeAndDeletedFalse(pipeline.getId(), command.layerType())
                .ifPresent(layer -> {
                    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Pipeline layer already exists");
                });
        CreativePipelineLayer layer = CreativePipelineLayer.create(
                pipeline.getId(),
                command.layerType(),
                command.layerCode(),
                command.layerName(),
                command.sortOrder(),
                command.enabled(),
                command.required(),
                command.retryable(),
                command.configuration());
        layerRepository.save(layer);
        return pipelineView(pipeline);
    }

    public CreativePipelineView updateLayer(UUID pipelineId, UUID layerId, CreativePipelineLayerCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        layer.update(
                command.layerName(),
                command.sortOrder(),
                command.enabled(),
                command.required(),
                command.retryable(),
                command.configuration());
        layerRepository.save(layer);
        return pipelineView(pipeline);
    }

    public CreativePipelineView disableLayer(UUID pipelineId, UUID layerId) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        layer.update(layer.getLayerName(), layer.getSortOrder(), false, layer.isRequired(), layer.isRetryable(), layer.getConfiguration());
        layerRepository.save(layer);
        return pipelineView(pipeline);
    }

    public CreativePipelineView assignTool(UUID pipelineId, UUID layerId, LayerToolMappingCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        validateProviderReferences(command);
        String mappingCode = AiToolProvider.normalizeCode(command.mappingCode(), "mappingCode");
        LayerToolMapping mapping = toolMappingRepository
                .findByPipelineLayerIdAndMappingCodeAndDeletedFalse(layer.getId(), mappingCode)
                .map(existing -> {
                    existing.update(
                            command.modelId(),
                            command.capabilityId(),
                            command.priorityOrder(),
                            command.routingWeight(),
                            command.enabled(),
                            command.fallbackEligible(),
                            command.routingMetadata());
                    return existing;
                })
                .orElseGet(() -> LayerToolMapping.create(
                        layer.getId(),
                        command.providerId(),
                        command.modelId(),
                        command.capabilityId(),
                        command.mappingCode(),
                        command.priorityOrder(),
                        command.routingWeight(),
                        command.enabled(),
                        command.fallbackEligible(),
                        command.routingMetadata()));
        toolMappingRepository.save(mapping);
        return pipelineView(pipeline);
    }

    public CreativePipelineView configureRoutingPolicy(UUID pipelineId, UUID layerId, LayerRoutingPolicyCommand command) {
        return upsertRoutingPolicy(pipelineId, layerId, command);
    }

    public CreativePipelineView configureFallbackPolicy(UUID pipelineId, UUID layerId, LayerRoutingPolicyCommand command) {
        LayerRoutingPolicyCommand fallbackCommand = new LayerRoutingPolicyCommand(
                command.policyCode() == null || command.policyCode().isBlank() ? FALLBACK_POLICY_CODE : command.policyCode(),
                LayerRoutingStrategy.FALLBACK_CHAIN,
                command.priorityOrder(),
                command.enabled(),
                command.conditions(),
                command.rules());
        return upsertRoutingPolicy(pipelineId, layerId, fallbackCommand);
    }

    public CreativePipelineView configureCostPolicy(UUID pipelineId, UUID layerId, LayerCostPolicyCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        String policyCode = AiToolProvider.normalizeCode(command.policyCode(), "policyCode");
        LayerCostPolicy policy = costPolicyRepository
                .findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(layer.getId(), policyCode)
                .map(existing -> {
                    existing.update(
                            command.enabled(),
                            command.priorityOrder(),
                            command.currency(),
                            command.maxCostPerRun(),
                            command.costRules(),
                            command.budgetMetadata());
                    return existing;
                })
                .orElseGet(() -> LayerCostPolicy.create(
                        layer.getId(),
                        command.policyCode(),
                        command.enabled(),
                        command.priorityOrder(),
                        command.currency(),
                        command.maxCostPerRun(),
                        command.costRules(),
                        command.budgetMetadata()));
        costPolicyRepository.save(policy);
        return pipelineView(pipeline);
    }

    public CreativePipelineView configureQualityPolicy(UUID pipelineId, UUID layerId, LayerQualityPolicyCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        String policyCode = AiToolProvider.normalizeCode(command.policyCode(), "policyCode");
        LayerQualityPolicy policy = qualityPolicyRepository
                .findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(layer.getId(), policyCode)
                .map(existing -> {
                    existing.update(
                            command.enabled(),
                            command.priorityOrder(),
                            command.minQualityScore(),
                            command.qualityRules(),
                            command.evaluationMetadata());
                    return existing;
                })
                .orElseGet(() -> LayerQualityPolicy.create(
                        layer.getId(),
                        command.policyCode(),
                        command.enabled(),
                        command.priorityOrder(),
                        command.minQualityScore(),
                        command.qualityRules(),
                        command.evaluationMetadata()));
        qualityPolicyRepository.save(policy);
        return pipelineView(pipeline);
    }

    public CreativePipelineView configureRetryPolicy(UUID pipelineId, UUID layerId, boolean retryable, Map<String, Object> configuration) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        layer.update(layer.getLayerName(), layer.getSortOrder(), layer.isEnabled(), layer.isRequired(), retryable, configuration);
        layerRepository.save(layer);
        return pipelineView(pipeline);
    }

    @Transactional(readOnly = true)
    public List<CreativePipelineView> listPipelines() {
        return pipelineRepository.findAllByDeletedFalseOrderByPipelineNameAscVersionDesc().stream()
                .map(this::pipelineView)
                .toList();
    }

    @Transactional(readOnly = true)
    public CreativePipelineView getPipeline(UUID pipelineId) {
        return pipelineView(pipeline(pipelineId));
    }

    private CreativePipelineView upsertRoutingPolicy(UUID pipelineId, UUID layerId, LayerRoutingPolicyCommand command) {
        CreativePipeline pipeline = pipeline(pipelineId);
        CreativePipelineLayer layer = layer(pipeline.getId(), layerId);
        String policyCode = AiToolProvider.normalizeCode(command.policyCode(), "policyCode");
        LayerRoutingPolicy policy = routingPolicyRepository
                .findByPipelineLayerIdAndPolicyCodeAndDeletedFalse(layer.getId(), policyCode)
                .map(existing -> {
                    existing.update(
                            command.routingStrategy(),
                            command.priorityOrder(),
                            command.enabled(),
                            command.conditions(),
                            command.rules());
                    return existing;
                })
                .orElseGet(() -> LayerRoutingPolicy.create(
                        layer.getId(),
                        command.policyCode(),
                        command.routingStrategy(),
                        command.priorityOrder(),
                        command.enabled(),
                        command.conditions(),
                        command.rules()));
        routingPolicyRepository.save(policy);
        return pipelineView(pipeline);
    }

    private void validateProviderReferences(LayerToolMappingCommand command) {
        providerRepository.findById(command.providerId())
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
        if (command.modelId() != null) {
            modelRepository.findById(command.modelId())
                    .filter(model -> !model.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI model not found"));
        }
        if (command.capabilityId() != null) {
            capabilityRepository.findById(command.capabilityId())
                    .filter(capability -> !capability.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI capability not found"));
        }
    }

    private CreativePipelineView pipelineView(CreativePipeline pipeline) {
        List<CreativePipelineLayerView> layers = layerRepository.findAllByPipelineIdAndDeletedFalseOrderBySortOrderAsc(pipeline.getId()).stream()
                .map(this::layerView)
                .toList();
        return mapper.toView(pipeline, layers);
    }

    private CreativePipelineLayerView layerView(CreativePipelineLayer layer) {
        return mapper.toView(
                layer,
                toolMappingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId()),
                routingPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId()),
                costPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId()),
                qualityPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId()));
    }

    private CreativePipeline pipeline(UUID pipelineId) {
        return pipelineRepository.findById(pipelineId)
                .filter(pipeline -> !pipeline.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative pipeline not found"));
    }

    private CreativePipelineLayer layer(UUID pipelineId, UUID layerId) {
        return layerRepository.findById(layerId)
                .filter(layer -> !layer.isDeleted())
                .filter(layer -> layer.getPipelineId().equals(pipelineId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Creative pipeline layer not found"));
    }

    private void deactivateActivePipeline() {
        pipelineRepository.findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus.ACTIVE)
                .ifPresent(active -> {
                    active.deactivate();
                    pipelineRepository.save(active);
                });
    }

    private void deactivateActivePipelineExcept(UUID pipelineId) {
        pipelineRepository.findFirstByActiveTrueAndStatusAndDeletedFalse(CreativePipelineStatus.ACTIVE)
                .filter(active -> !active.getId().equals(pipelineId))
                .ifPresent(active -> {
                    active.deactivate();
                    pipelineRepository.save(active);
                });
    }
}
