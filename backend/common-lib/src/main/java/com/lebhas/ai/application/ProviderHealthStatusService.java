package com.lebhas.ai.application;

import com.lebhas.ai.domain.AiProviderMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProviderHealthStatusService {

    private static final BigDecimal HEALTHY_THRESHOLD = new BigDecimal("0.9500");
    private static final BigDecimal DEGRADED_THRESHOLD = new BigDecimal("0.8000");

    private final ProviderReliabilityScorer reliabilityScorer;

    public ProviderHealthStatusService(ProviderReliabilityScorer reliabilityScorer) {
        this.reliabilityScorer = reliabilityScorer;
    }

    public String status(List<AiProviderMetrics> metrics) {
        if (metrics == null || metrics.isEmpty() || metrics.stream().mapToLong(AiProviderMetrics::getTotalRequests).sum() == 0) {
            return "NO_DATA";
        }
        BigDecimal reliability = reliabilityScorer.aggregateReliabilityScore(metrics);
        if (reliability.compareTo(HEALTHY_THRESHOLD) >= 0) {
            return "HEALTHY";
        }
        if (reliability.compareTo(DEGRADED_THRESHOLD) >= 0) {
            return "DEGRADED";
        }
        return "UNHEALTHY";
    }
}
