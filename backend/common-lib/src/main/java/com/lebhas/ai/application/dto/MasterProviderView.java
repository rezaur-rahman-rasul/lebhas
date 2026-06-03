package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CredentialStatus;
import com.lebhas.ai.domain.ProviderConnectionTestStatus;
import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MasterProviderView(
        UUID id,
        String providerKey,
        String providerCode,
        String displayName,
        String category,
        ProviderType providerType,
        ProviderStatus status,
        String description,
        List<String> supportedCapabilities,
        List<ProviderEnvironment> supportedEnvironments,
        boolean supportsSandbox,
        boolean supportsLive,
        ProviderEnvironment defaultEnvironment,
        CredentialStatus credentialStatus,
        boolean credentialConfigured,
        ProviderEnvironment activeEnvironment,
        boolean webhookConfigured,
        String webhookUrl,
        ProviderConnectionTestStatus lastTestStatus,
        Instant lastTestedAt,
        String lastTestMessage,
        boolean active,
        boolean systemDefault,
        boolean secretsHidden,
        Instant credentialUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
