package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import jakarta.validation.constraints.NotNull;

public record TestProviderConnectionRequest(
        @NotNull
        ProviderEnvironment environment,
        String secret
) {
}
