package com.lebhas.ai.application;

import com.lebhas.ai.application.dto.CostEstimateInput;
import com.lebhas.ai.application.dto.LayerCostRecommendation;
import com.lebhas.ai.application.dto.ProviderCostOption;
import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.domain.LayerQualityPolicy;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class LayerCostOptimizationService {

    private final ProviderCostComparisonService providerCostComparisonService;
    private final LayerCostPolicyRepository costPolicyRepository;
    private final LayerQualityPolicyRepository qualityPolicyRepository;
    private final CostEfficiencyCalculator costEfficiencyCalculator;

    public LayerCostOptimizationService(
            ProviderCostComparisonService providerCostComparisonService,
            LayerCostPolicyRepository costPolicyRepository,
            LayerQualityPolicyRepository qualityPolicyRepository,
            CostEfficiencyCalculator costEfficiencyCalculator
    ) {
        this.providerCostComparisonService = providerCostComparisonService;
        this.costPolicyRepository = costPolicyRepository;
        this.qualityPolicyRepository = qualityPolicyRepository;
        this.costEfficiencyCalculator = costEfficiencyCalculator;
    }

    @Transactional(readOnly = true)
    public LayerCostRecommendation recommendCheaperRouting(UUID layerId, CostEstimateInput input) {
        ProviderCostOption current = providerCostComparisonService.currentPriorityProvider(layerId, input);
        ProviderCostOption recommended = providerCostComparisonService.compareProviderCostEfficiency(layerId, input).stream()
                .filter(this::passesCostPolicy)
                .filter(this::passesQualityPolicy)
                .findFirst()
                .orElse(null);
        if (current == null || recommended == null) {
            return new LayerCostRecommendation(layerId, current, recommended, BigDecimal.ZERO.setScale(6), false,
                    "No comparable provider cost option is available");
        }
        if (!costEfficiencyCalculator.isLowerCost(recommended, current)) {
            return new LayerCostRecommendation(layerId, current, recommended, BigDecimal.ZERO.setScale(6), false,
                    "Current routing is already cost efficient");
        }
        BigDecimal savings = current.estimatedCostUsd()
                .subtract(recommended.estimatedCostUsd())
                .setScale(6, RoundingMode.HALF_UP);
        return new LayerCostRecommendation(layerId, current, recommended, savings, true,
                "Cheaper eligible routing option found");
    }

    @Transactional(readOnly = true)
    public List<ProviderCostOption> eligibleCostOptions(UUID layerId, CostEstimateInput input) {
        return providerCostComparisonService.compareProviderCostEfficiency(layerId, input).stream()
                .filter(this::passesCostPolicy)
                .filter(this::passesQualityPolicy)
                .toList();
    }

    private boolean passesCostPolicy(ProviderCostOption option) {
        if (option == null || !option.eligible()) {
            return false;
        }
        List<LayerCostPolicy> policies = costPolicyRepository
                .findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(option.layerId()).stream()
                .filter(LayerCostPolicy::isEnabled)
                .toList();
        if (policies.isEmpty() || option.estimatedCostUsd() == null) {
            return true;
        }
        return policies.stream()
                .filter(policy -> policy.getMaxCostPerRun() != null)
                .allMatch(policy -> option.estimatedCostUsd().compareTo(policy.getMaxCostPerRun()) <= 0);
    }

    private boolean passesQualityPolicy(ProviderCostOption option) {
        if (option == null || !option.eligible()) {
            return false;
        }
        List<LayerQualityPolicy> policies = qualityPolicyRepository
                .findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(option.layerId()).stream()
                .filter(LayerQualityPolicy::isEnabled)
                .toList();
        if (policies.isEmpty() || option.qualityScore() == null) {
            return true;
        }
        return policies.stream()
                .filter(policy -> policy.getMinQualityScore() != null)
                .allMatch(policy -> option.qualityScore().compareTo(policy.getMinQualityScore()) >= 0);
    }
}
