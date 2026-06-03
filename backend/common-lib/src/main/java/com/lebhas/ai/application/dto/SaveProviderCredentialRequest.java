package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import jakarta.validation.constraints.NotNull;

public record SaveProviderCredentialRequest(
        @NotNull
        ProviderEnvironment environment,
        String secret,
        String webhookUrl,
        boolean active
) {
}
