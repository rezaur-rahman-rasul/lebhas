package com.lebhas.ai.application.dto;

import com.lebhas.ai.domain.CreativeToolCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CreativeToolCommand(
        String toolCode,
        String toolName,
        CreativeToolCategory toolCategory,
        boolean enabled,
        String description,
        List<CreativeToolCapabilityCommand> capabilities,
        ToolCreditCostPolicyCommand costPolicy,
        Map<String, Object> metadata
) {
    public record CreativeToolCapabilityCommand(String capabilityCode, boolean enabled, Map<String, Object> metadata) {
    }

    public record ToolCreditCostPolicyCommand(
            String policyCode,
            BigDecimal creditCost,
            boolean enabled,
            Instant effectiveFrom,
            Instant effectiveUntil,
            Map<String, Object> metadata
    ) {
    }
}
