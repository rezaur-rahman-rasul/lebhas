package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;

public record UpdateMasterProviderRequest(
        String displayName,
        String description,
        ProviderStatus status,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment
) {
}
