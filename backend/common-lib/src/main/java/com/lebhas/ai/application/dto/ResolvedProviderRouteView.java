package com.lebhas.ai.application.dto;

import java.util.UUID;

public record ResolvedProviderRouteView(
        UUID policyId,
        UUID toolId,
        String qualityMode,
        UUID providerId,
        UUID modelId,
        boolean fallbackSelected,
        String reason
) {
}
