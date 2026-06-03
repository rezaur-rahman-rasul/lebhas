package com.lebhas.creativesaas.creativerequest.application.dto;

import java.util.List;
import java.util.UUID;

public record CreativeRequestReadinessView(
        UUID creativeRequestId,
        boolean ready,
        List<String> blockingReasons
) {
    public static CreativeRequestReadinessView ready(UUID creativeRequestId) {
        return new CreativeRequestReadinessView(creativeRequestId, true, List.of());
    }

    public static CreativeRequestReadinessView blocked(UUID creativeRequestId, List<String> blockingReasons) {
        return new CreativeRequestReadinessView(creativeRequestId, false, List.copyOf(blockingReasons));
    }
}
