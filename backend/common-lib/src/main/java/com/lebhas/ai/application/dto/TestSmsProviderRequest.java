package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;

public record TestSmsProviderRequest(
        ProviderEnvironment environment,
        String mobileNumber,
        String message
) {
}
