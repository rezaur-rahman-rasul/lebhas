package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.CreativePipelineLayer;
import com.lebhas.ai.domain.LayerToolMapping;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class LayerToolResolver {

    private final LayerToolMappingRepository layerToolMappingRepository;
    private final AiToolProviderRepository aiToolProviderRepository;

    public LayerToolResolver(
            LayerToolMappingRepository layerToolMappingRepository,
            AiToolProviderRepository aiToolProviderRepository
    ) {
        this.layerToolMappingRepository = layerToolMappingRepository;
        this.aiToolProviderRepository = aiToolProviderRepository;
    }

    @Transactional(readOnly = true)
    public List<LayerToolCandidate> resolveCandidates(CreativePipelineLayer layer) {
        return layerToolMappingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layer.getId())
                .stream()
                .filter(LayerToolMapping::isEnabled)
                .map(mapping -> toCandidate(layer, mapping))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(candidate -> candidate.mapping().getPriorityOrder()))
                .toList();
    }

    private Optional<LayerToolCandidate> toCandidate(CreativePipelineLayer layer, LayerToolMapping mapping) {
        return aiToolProviderRepository.findById(mapping.getProviderId())
                .filter(provider -> !provider.isDeleted())
                .filter(provider -> provider.isEnabled() && provider.getStatus() == ProviderStatus.ACTIVE)
                .filter(provider -> provider.isWorkspaceRoutingEligible() && provider.isPlanRoutingEligible())
                .filter(provider -> supportsLayer(provider, layer))
                .map(provider -> new LayerToolCandidate(
                        mapping,
                        provider,
                        numericMetadata(provider.getCostMetadata(), "estimatedCost"),
                        numericMetadata(provider.getQualityMetadata(), "qualityScore")));
    }

    private boolean supportsLayer(AiToolProvider provider, CreativePipelineLayer layer) {
        List<String> supportedLayers = provider.getSupportedLayers();
        return supportedLayers.isEmpty() || supportedLayers.contains(layer.getLayerType().name());
    }

    private BigDecimal numericMetadata(java.util.Map<String, Object> metadata, String key) {
        if (metadata == null || metadata.get(key) == null) {
            return null;
        }
        try {
            return new BigDecimal(metadata.get(key).toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
