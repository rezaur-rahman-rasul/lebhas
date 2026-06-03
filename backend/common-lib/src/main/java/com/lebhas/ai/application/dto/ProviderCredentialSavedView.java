package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.ProviderEnvironment;

import java.util.UUID;

public record ProviderCredentialSavedView(
        UUID providerId,
        String providerKey,
        String providerCode,
        String displayName,
        String category,
        ProviderEnvironment environment,
        CredentialStatus credentialStatus,
        boolean active,
        boolean secretsHidden
) {
}
