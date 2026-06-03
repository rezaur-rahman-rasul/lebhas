package com.lebhas.ai.application.dto;

import java.util.Map;

public record AiProviderCredentialCommand(
        String credentialName,
        String secretValue,
        boolean active,
        Map<String, Object> metadata
) {
}
