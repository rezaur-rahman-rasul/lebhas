package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.AiModelView;
import com.lebhas.ai.application.dto.AiProviderView;
import com.lebhas.ai.application.dto.AiToolCapabilityView;
import com.lebhas.ai.config.AiProviderRegistryProperties;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolCapability;
import com.lebhas.ai.domain.AiToolProvider;

import java.util.List;

public class AiProviderRegistryMapper {

    private final AiProviderRegistryProperties properties;

    public AiProviderRegistryMapper(AiProviderRegistryProperties properties) {
        this.properties = properties;
    }

    public AiProviderView toView(AiToolProvider provider, List<AiModel> models, List<AiToolCapability> capabilities) {
        return new AiProviderView(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getProviderType(),
                provider.getStatus(),
                provider.isEnabled(),
                provider.getSupportedLayers(),
                provider.getCredentialConfigKey(),
                properties.hasCredentialConfiguration(provider.getCredentialConfigKey()),
                provider.isFallbackEligible(),
                provider.isWorkspaceRoutingEligible(),
                provider.isPlanRoutingEligible(),
                provider.getCostMetadata(),
                provider.getQualityMetadata(),
                provider.getRateLimitMetadata(),
                models.stream().map(this::toView).toList(),
                capabilities.stream().map(this::toView).toList());
    }

    public AiModelView toView(AiModel model) {
        return new AiModelView(
                model.getId(),
                model.getProviderId(),
                model.getModelCode(),
                model.getModelName(),
                model.getStatus(),
                model.isEnabled(),
                model.isDefaultModel(),
                model.getCapabilities(),
                model.getCostMetadata(),
                model.getQualityMetadata(),
                model.getRateLimitMetadata());
    }

    public AiToolCapabilityView toView(AiToolCapability capability) {
        return new AiToolCapabilityView(
                capability.getId(),
                capability.getProviderId(),
                capability.getCapabilityCode(),
                capability.getLayerCode(),
                capability.getModelCode(),
                capability.isEnabled(),
                capability.getMetadata());
    }
}
