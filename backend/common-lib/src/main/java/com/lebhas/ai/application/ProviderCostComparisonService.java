package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.domain.AiModel;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.LayerToolMapping;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiModelRepository;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.ai.infrastructure.persistence.LayerToolMappingRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProviderCostComparisonService {

    private final LayerToolMappingRepository mappingRepository;
    private final AiToolProviderRepository providerRepository;
    private final AiModelRepository modelRepository;
    private final CostEfficiencyCalculator costEfficiencyCalculator;

    public ProviderCostComparisonService(
            LayerToolMappingRepository mappingRepository,
            AiToolProviderRepository providerRepository,
            AiModelRepository modelRepository,
            CostEfficiencyCalculator costEfficiencyCalculator
    ) {
        this.mappingRepository = mappingRepository;
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.costEfficiencyCalculator = costEfficiencyCalculator;
    }

    @Transactional(readOnly = true)
    public List<ProviderCostOption> compareProviderCostEfficiency(UUID layerId, CostEstimateInput input) {
        CostEstimateInput normalizedInput = input == null ? CostEstimateInput.defaultInput() : input;
        return mappingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layerId).stream()
                .map(mapping -> toOption(layerId, mapping, normalizedInput.requestedUnits()))
                .flatMap(Optional::stream)
                .sorted(optionComparator())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderCostOption cheapestEligibleProvider(UUID layerId, CostEstimateInput input) {
        return compareProviderCostEfficiency(layerId, input).stream()
                .filter(ProviderCostOption::eligible)
                .filter(ProviderCostOption::costKnown)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ProviderCostOption currentPriorityProvider(UUID layerId, CostEstimateInput input) {
        CostEstimateInput normalizedInput = input == null ? CostEstimateInput.defaultInput() : input;
        return mappingRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(layerId).stream()
                .filter(LayerToolMapping::isEnabled)
                .findFirst()
                .flatMap(mapping -> toOption(layerId, mapping, normalizedInput.requestedUnits()))
                .orElse(null);
    }

    private Optional<ProviderCostOption> toOption(UUID layerId, LayerToolMapping mapping, BigDecimal requestedUnits) {
        AiToolProvider provider = providerRepository.findById(mapping.getProviderId())
                .filter(current -> !current.isDeleted())
                .orElse(null);
        if (provider == null) {
            return Optional.empty();
        }
        AiModel model = resolveModel(mapping, provider.getId()).orElse(null);
        boolean eligible = mapping.isEnabled()
                && provider.isEnabled()
                && provider.getStatus() == ProviderStatus.ACTIVE
                && (model == null || (model.isEnabled() && model.getStatus() == ProviderStatus.ACTIVE));
        String reason = eligibilityReason(mapping, provider, model);
        return Optional.of(costEfficiencyCalculator.toOption(
                layerId,
                mapping,
                provider,
                model,
                requestedUnits,
                eligible,
                reason));
    }

    private Optional<AiModel> resolveModel(LayerToolMapping mapping, UUID providerId) {
        if (mapping.getModelId() != null) {
            return modelRepository.findById(mapping.getModelId()).filter(model -> !model.isDeleted());
        }
        return modelRepository.findAllByProviderIdAndDeletedFalseOrderByModelNameAsc(providerId).stream()
                .filter(AiModel::isDefaultModel)
                .findFirst()
                .or(() -> modelRepository.findAllByProviderIdAndDeletedFalseOrderByModelNameAsc(providerId).stream().findFirst());
    }

    private String eligibilityReason(LayerToolMapping mapping, AiToolProvider provider, AiModel model) {
        if (!mapping.isEnabled()) {
            return "Layer tool mapping is disabled";
        }
        if (!provider.isEnabled() || provider.getStatus() != ProviderStatus.ACTIVE) {
            return "Provider is not active";
        }
        if (model != null && (!model.isEnabled() || model.getStatus() != ProviderStatus.ACTIVE)) {
            return "Model is not active";
        }
        return null;
    }

    private Comparator<ProviderCostOption> optionComparator() {
        return Comparator
                .comparing((ProviderCostOption option) -> !option.eligible())
                .thenComparing(option -> !option.costKnown())
                .thenComparing(option -> option.estimatedCostUsd() == null ? BigDecimal.ZERO : option.estimatedCostUsd())
                .thenComparing((ProviderCostOption option) -> option.qualityToCostRatio() == null ? BigDecimal.ZERO : option.qualityToCostRatio(), Comparator.reverseOrder());
    }

    @Transactional(readOnly = true)
    public AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findById(providerId)
                .filter(provider -> !provider.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }
}
