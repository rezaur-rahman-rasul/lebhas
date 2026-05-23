CREATE TABLE IF NOT EXISTS platform.ai_tool_providers (
    id UUID PRIMARY KEY,
    provider_code VARCHAR(80) NOT NULL,
    provider_name VARCHAR(160) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    supported_layers JSONB NOT NULL DEFAULT '[]'::jsonb,
    credential_config_key VARCHAR(160),
    fallback_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    workspace_routing_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    plan_routing_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    cost_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    quality_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    rate_limit_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_tool_providers_code_active
    ON platform.ai_tool_providers (provider_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_tool_providers_enabled_status
    ON platform.ai_tool_providers (enabled, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_tool_providers_type
    ON platform.ai_tool_providers (provider_type)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_tool_providers_supported_layers
    ON platform.ai_tool_providers USING GIN (supported_layers);

CREATE TABLE IF NOT EXISTS platform.ai_models (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_code VARCHAR(120) NOT NULL,
    model_name VARCHAR(180) NOT NULL,
    status VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_model BOOLEAN NOT NULL DEFAULT FALSE,
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    cost_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    quality_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    rate_limit_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_models_provider_model_active
    ON platform.ai_models (provider_id, model_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_models_provider_enabled
    ON platform.ai_models (provider_id, enabled, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_models_capabilities
    ON platform.ai_models USING GIN (capabilities);

CREATE TABLE IF NOT EXISTS platform.ai_tool_capabilities (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    capability_code VARCHAR(120) NOT NULL,
    layer_code VARCHAR(120) NOT NULL,
    model_code VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_ai_tool_capabilities_provider
    ON platform.ai_tool_capabilities (provider_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_tool_capabilities_layer
    ON platform.ai_tool_capabilities (layer_code, enabled)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_tool_capabilities_model
    ON platform.ai_tool_capabilities (provider_id, model_code)
    WHERE is_deleted = FALSE AND model_code IS NOT NULL;
