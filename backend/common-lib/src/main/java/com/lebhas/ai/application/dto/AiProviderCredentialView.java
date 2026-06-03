package com.lebhas.ai.application.dto;

import java.util.Map;
import java.util.UUID;

public record AiProviderCredentialView(
        UUID id,
        UUID providerId,
        String credentialName,
        String maskedSecret,
        boolean active,
        Map<String, Object> metadata
) {
}
