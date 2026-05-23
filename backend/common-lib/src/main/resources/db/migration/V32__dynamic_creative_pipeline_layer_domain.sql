CREATE TABLE IF NOT EXISTS platform.creative_pipelines (
    id UUID PRIMARY KEY,
    pipeline_code VARCHAR(120) NOT NULL,
    pipeline_name VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    pipeline_version INTEGER NOT NULL DEFAULT 1,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_creative_pipelines_version_positive CHECK (pipeline_version > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_pipelines_code_active
    ON platform.creative_pipelines (pipeline_code)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_pipelines_single_active
    ON platform.creative_pipelines (active)
    WHERE active = TRUE AND status = 'ACTIVE' AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_creative_pipelines_status
    ON platform.creative_pipelines (status, active)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.creative_pipeline_layers (
    id UUID PRIMARY KEY,
    pipeline_id UUID NOT NULL REFERENCES platform.creative_pipelines(id),
    layer_type VARCHAR(60) NOT NULL,
    layer_code VARCHAR(120) NOT NULL,
    layer_name VARCHAR(180) NOT NULL,
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    required_layer BOOLEAN NOT NULL DEFAULT FALSE,
    retryable BOOLEAN NOT NULL DEFAULT TRUE,
    configuration JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_creative_pipeline_layers_sort_positive CHECK (sort_order > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_pipeline_layers_type_active
    ON platform.creative_pipeline_layers (pipeline_id, layer_type)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_pipeline_layers_code_active
    ON platform.creative_pipeline_layers (pipeline_id, layer_code)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_creative_pipeline_layers_order_active
    ON platform.creative_pipeline_layers (pipeline_id, sort_order)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_creative_pipeline_layers_enabled
    ON platform.creative_pipeline_layers (pipeline_id, enabled, sort_order)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.layer_tool_mappings (
    id UUID PRIMARY KEY,
    pipeline_layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_id UUID REFERENCES platform.ai_models(id),
    capability_id UUID REFERENCES platform.ai_tool_capabilities(id),
    mapping_code VARCHAR(120) NOT NULL,
    priority_order INTEGER NOT NULL,
    routing_weight INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fallback_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    routing_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_layer_tool_mappings_priority_positive CHECK (priority_order > 0),
    CONSTRAINT chk_layer_tool_mappings_weight_nonnegative CHECK (routing_weight >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_layer_tool_mappings_code_active
    ON platform.layer_tool_mappings (pipeline_layer_id, mapping_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_tool_mappings_layer_priority
    ON platform.layer_tool_mappings (pipeline_layer_id, enabled, priority_order)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_tool_mappings_provider
    ON platform.layer_tool_mappings (provider_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_tool_mappings_model
    ON platform.layer_tool_mappings (model_id)
    WHERE is_deleted = FALSE AND model_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.layer_routing_policies (
    id UUID PRIMARY KEY,
    pipeline_layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    policy_code VARCHAR(120) NOT NULL,
    routing_strategy VARCHAR(40) NOT NULL,
    priority_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    conditions JSONB NOT NULL DEFAULT '{}'::jsonb,
    rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_layer_routing_policies_priority_positive CHECK (priority_order > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_layer_routing_policies_code_active
    ON platform.layer_routing_policies (pipeline_layer_id, policy_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_routing_policies_layer_priority
    ON platform.layer_routing_policies (pipeline_layer_id, enabled, priority_order)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_routing_policies_conditions
    ON platform.layer_routing_policies USING GIN (conditions);

CREATE TABLE IF NOT EXISTS platform.layer_cost_policies (
    id UUID PRIMARY KEY,
    pipeline_layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    policy_code VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority_order INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL,
    max_cost_per_run NUMERIC(19, 6),
    cost_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    budget_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_layer_cost_policies_priority_positive CHECK (priority_order > 0),
    CONSTRAINT chk_layer_cost_policies_cost_nonnegative CHECK (max_cost_per_run IS NULL OR max_cost_per_run >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_layer_cost_policies_code_active
    ON platform.layer_cost_policies (pipeline_layer_id, policy_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_cost_policies_layer_priority
    ON platform.layer_cost_policies (pipeline_layer_id, enabled, priority_order)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_cost_policies_rules
    ON platform.layer_cost_policies USING GIN (cost_rules);

CREATE TABLE IF NOT EXISTS platform.layer_quality_policies (
    id UUID PRIMARY KEY,
    pipeline_layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    policy_code VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority_order INTEGER NOT NULL,
    min_quality_score NUMERIC(8, 4),
    quality_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    evaluation_metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_layer_quality_policies_priority_positive CHECK (priority_order > 0),
    CONSTRAINT chk_layer_quality_policies_score_range CHECK (min_quality_score IS NULL OR (min_quality_score >= 0 AND min_quality_score <= 1))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_layer_quality_policies_code_active
    ON platform.layer_quality_policies (pipeline_layer_id, policy_code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_quality_policies_layer_priority
    ON platform.layer_quality_policies (pipeline_layer_id, enabled, priority_order)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_layer_quality_policies_rules
    ON platform.layer_quality_policies USING GIN (quality_rules);
