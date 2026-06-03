package com.lebhas.ai.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tool_credit_cost_policies", schema = "platform")
public class ToolCreditCostPolicy extends BaseEntity {

    @Column(name = "tool_id", nullable = false)
    private UUID toolId;

    @Column(name = "policy_code", nullable = false, length = 120)
    private String policyCode;

    @Column(name = "credit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditCost;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_until")
    private Instant effectiveUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    protected ToolCreditCostPolicy() {
    }

    public static ToolCreditCostPolicy create(UUID toolId, String policyCode, BigDecimal creditCost, boolean enabled, Instant effectiveFrom, Instant effectiveUntil, Map<String, Object> metadata) {
        ToolCreditCostPolicy policy = new ToolCreditCostPolicy();
        policy.toolId = AiToolProvider.require(toolId, "toolId");
        policy.policyCode = AiToolProvider.normalizeCode(policyCode, "policyCode");
        policy.update(creditCost, enabled, effectiveFrom, effectiveUntil, metadata);
        return policy;
    }

    public void update(BigDecimal creditCost, boolean enabled, Instant effectiveFrom, Instant effectiveUntil, Map<String, Object> metadata) {
        if (creditCost == null || creditCost.signum() < 0) {
            throw new IllegalArgumentException("creditCost must not be negative");
        }
        this.creditCost = creditCost;
        this.enabled = enabled;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.metadata = AiToolProvider.normalizeMetadata(metadata);
    }

    public UUID getToolId() {
        return toolId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public BigDecimal getCreditCost() {
        return creditCost;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveUntil() {
        return effectiveUntil;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }
}
