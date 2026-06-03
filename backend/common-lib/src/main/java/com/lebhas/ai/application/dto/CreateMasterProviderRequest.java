package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderType;

public record CreateMasterProviderRequest(
        String providerCode,
        String displayName,
        ProviderType providerType,
        String description,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment,
        boolean active
) {
}
