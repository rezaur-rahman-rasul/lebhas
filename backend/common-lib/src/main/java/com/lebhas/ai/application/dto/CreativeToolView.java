package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativeToolCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreativeToolView(
        UUID id,
        String toolCode,
        String toolName,
        CreativeToolCategory toolCategory,
        boolean enabled,
        String description,
        List<CreativeToolCapabilityView> capabilities,
        List<ToolCreditCostPolicyView> costPolicies,
        Map<String, Object> metadata
) {
    public record CreativeToolCapabilityView(UUID id, String capabilityCode, boolean enabled, Map<String, Object> metadata) {
    }

    public record ToolCreditCostPolicyView(
            UUID id,
            String policyCode,
            BigDecimal creditCost,
            boolean enabled,
            Instant effectiveFrom,
            Instant effectiveUntil,
            Map<String, Object> metadata
    ) {
    }
}
