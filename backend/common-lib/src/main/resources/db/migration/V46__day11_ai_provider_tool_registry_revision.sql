CREATE TABLE IF NOT EXISTS platform.ai_provider_credentials (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    credential_name VARCHAR(120) NOT NULL,
    encrypted_secret TEXT,
    masked_secret VARCHAR(160),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ai_provider_credentials_provider
    ON platform.ai_provider_credentials (provider_id, active)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_provider_credentials_name_active
    ON platform.ai_provider_credentials (provider_id, credential_name)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.creative_tools (
    id UUID PRIMARY KEY,
    tool_code VARCHAR(120) NOT NULL,
    tool_name VARCHAR(180) NOT NULL,
    tool_category VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_tools_code_active
    ON platform.creative_tools (tool_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_creative_tools_category
    ON platform.creative_tools (tool_category, enabled)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.creative_tool_capabilities (
    id UUID PRIMARY KEY,
    tool_id UUID NOT NULL REFERENCES platform.creative_tools(id),
    capability_code VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_tool_capabilities_code_active
    ON platform.creative_tool_capabilities (tool_id, capability_code)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.tool_credit_cost_policies (
    id UUID PRIMARY KEY,
    tool_id UUID NOT NULL REFERENCES platform.creative_tools(id),
    policy_code VARCHAR(120) NOT NULL,
    credit_cost NUMERIC(19, 4) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ,
    effective_until TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_tool_credit_cost_nonnegative CHECK (credit_cost >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_tool_credit_cost_policies_code_active
    ON platform.tool_credit_cost_policies (tool_id, policy_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_tool_credit_cost_policies_tool_enabled
    ON platform.tool_credit_cost_policies (tool_id, enabled)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.provider_routing_policies (
    id UUID PRIMARY KEY,
    policy_code VARCHAR(120) NOT NULL,
    tool_id UUID NOT NULL REFERENCES platform.creative_tools(id),
    quality_mode VARCHAR(60) NOT NULL,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_id UUID REFERENCES platform.ai_models(id),
    fallback_provider_id UUID REFERENCES platform.ai_tool_providers(id),
    fallback_model_id UUID REFERENCES platform.ai_models(id),
    priority_order INTEGER NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    circuit_failure_threshold INTEGER NOT NULL DEFAULT 3,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_provider_routing_priority_positive CHECK (priority_order > 0),
    CONSTRAINT chk_provider_routing_failure_threshold_positive CHECK (circuit_failure_threshold > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_provider_routing_policies_code_active
    ON platform.provider_routing_policies (policy_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_provider_routing_policies_tool_quality
    ON platform.provider_routing_policies (tool_id, quality_mode, enabled, priority_order)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.provider_health_snapshots (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    status VARCHAR(40) NOT NULL,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    circuit_open BOOLEAN NOT NULL DEFAULT FALSE,
    last_checked_at TIMESTAMPTZ NOT NULL,
    failure_reason VARCHAR(240),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_provider_health_failures_nonnegative CHECK (consecutive_failures >= 0)
);

CREATE INDEX IF NOT EXISTS idx_provider_health_snapshots_provider_latest
    ON platform.provider_health_snapshots (provider_id, last_checked_at DESC)
    WHERE is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.day11.ai_provider_tool_registry_revision', '46')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
