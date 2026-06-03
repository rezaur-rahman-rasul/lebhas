package com.lebhas.creativesaas.imagecreative.application.dto;

import java.util.List;

public record ProductImageCreativeReadinessView(
        boolean ready,
        boolean workspaceReady,
        boolean packageReady,
        boolean creditsReady,
        boolean providerReady,
        boolean routingReady,
        boolean productAssetReady,
        List<String> messages,
        List<ReadinessMessage> readinessMessages
) {
    public record ReadinessMessage(
            String code,
            String message
    ) {
    }
}
