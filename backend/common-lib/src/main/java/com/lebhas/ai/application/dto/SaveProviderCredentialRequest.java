package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaveProviderCredentialRequest(
        @NotNull
        ProviderEnvironment environment,
        String secret,
        String webhookUrl,
        BigDecimal availableCreditBalance,
        boolean active
) {
}
