package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.ai.domain.LayerQualityPolicy;
import com.lebhas.ai.infrastructure.persistence.LayerQualityPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QualityAwareRoutingService {

    private final LayerQualityPolicyRepository layerQualityPolicyRepository;

    public QualityAwareRoutingService(LayerQualityPolicyRepository layerQualityPolicyRepository) {
        this.layerQualityPolicyRepository = layerQualityPolicyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<LayerQualityPolicy> activePolicy(UUID pipelineLayerId) {
        return layerQualityPolicyRepository.findAllByPipelineLayerIdAndDeletedFalseOrderByPriorityOrderAsc(pipelineLayerId)
                .stream()
                .filter(LayerQualityPolicy::isEnabled)
                .findFirst();
    }

    public Optional<LayerToolCandidate> chooseHighestQuality(List<LayerToolCandidate> candidates, Optional<LayerQualityPolicy> policy) {
        return candidates.stream()
                .filter(candidate -> meetsPolicy(candidate, policy))
                .max(Comparator.comparing(candidate -> candidate.qualityScore() == null ? BigDecimal.ZERO : candidate.qualityScore()));
    }

    public BigDecimal estimateQualityScore(LayerToolCandidate candidate) {
        if (candidate == null || candidate.qualityScore() == null) {
            return BigDecimal.ZERO;
        }
        return candidate.qualityScore();
    }

    private boolean meetsPolicy(LayerToolCandidate candidate, Optional<LayerQualityPolicy> policy) {
        if (policy.isEmpty() || policy.get().getMinQualityScore() == null || candidate.qualityScore() == null) {
            return true;
        }
        return candidate.qualityScore().compareTo(policy.get().getMinQualityScore()) >= 0;
    }
}
