package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.LayerCostPolicy;
import com.lebhas.ai.infrastructure.persistence.LayerCostPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CostAwareRoutingService {

    private final LayerCostPolicyRepository layerCostPolicyRepository;

    public CostAwareRoutingService(LayerCostPolicyRepository layerCostPolicyRepository) {
        this.layerCostPolicyRepository = layerCostPolicyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<LayerCostPolicy> activePolicy(UUID pipelineLayerId) {
        return layerCostPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(pipelineLayerId)
                .stream()
                .filter(LayerCostPolicy::isEnabled)
                .findFirst();
    }

    public Optional<LayerToolCandidate> chooseLowestCost(List<LayerToolCandidate> candidates, Optional<LayerCostPolicy> policy) {
        return candidates.stream()
                .filter(candidate -> withinPolicy(candidate, policy))
                .min(Comparator.comparing(candidate -> candidate.estimatedCost() == null ? BigDecimal.ZERO : candidate.estimatedCost()));
    }

    public BigDecimal estimateLayerCost(LayerToolCandidate candidate, Optional<LayerCostPolicy> policy) {
        if (candidate == null) {
            return BigDecimal.ZERO;
        }
        if (candidate.estimatedCost() != null) {
            return candidate.estimatedCost();
        }
        return policy.map(LayerCostPolicy::getMaxCostPerRun).orElse(BigDecimal.ZERO);
    }

    private boolean withinPolicy(LayerToolCandidate candidate, Optional<LayerCostPolicy> policy) {
        if (policy.isEmpty() || policy.get().getMaxCostPerRun() == null || candidate.estimatedCost() == null) {
            return true;
        }
        return candidate.estimatedCost().compareTo(policy.get().getMaxCostPerRun()) <= 0;
    }
}
