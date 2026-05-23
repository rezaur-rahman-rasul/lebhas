package com.lebhas.ai.application.dto;

import java.util.List;

public record ProviderBenchmarkSummary(
        List<ProviderBenchmarkResult> results
) {
    public ProviderBenchmarkSummary {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
