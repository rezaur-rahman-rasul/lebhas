package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderStatus;

public record UpdateProviderStatusRequest(
        ProviderStatus status
) {
}
