package com.lebhas.creativesaas.creativerequest.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FallbackResolver {

    public Optional<LayerToolCandidate> resolveFallback(List<LayerToolCandidate> candidates, Set<UUID> attemptedProviderIds) {
        Set<UUID> attempted = attemptedProviderIds == null ? Set.of() : attemptedProviderIds;
        return candidates.stream()
                .filter(candidate -> candidate.mapping().isFallbackEligible())
                .filter(candidate -> candidate.provider().isFallbackEligible())
                .filter(candidate -> !attempted.contains(candidate.provider().getId()))
                .findFirst();
    }
}
