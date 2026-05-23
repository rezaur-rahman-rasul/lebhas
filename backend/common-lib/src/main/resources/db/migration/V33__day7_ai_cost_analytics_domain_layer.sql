CREATE TABLE IF NOT EXISTS platform.ai_provider_metrics (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_name VARCHAR(160) NOT NULL,
    total_requests BIGINT NOT NULL DEFAULT 0,
    successful_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    avg_latency_ms NUMERIC(19, 4) NOT NULL DEFAULT 0,
    avg_cost_usd NUMERIC(19, 6) NOT NULL DEFAULT 0,
    avg_quality_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    uptime_percentage NUMERIC(8, 4) NOT NULL DEFAULT 0,
    last_failure_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_ai_provider_metrics_total_requests_nonnegative CHECK (total_requests >= 0),
    CONSTRAINT chk_ai_provider_metrics_successful_requests_nonnegative CHECK (successful_requests >= 0),
    CONSTRAINT chk_ai_provider_metrics_failed_requests_nonnegative CHECK (failed_requests >= 0),
    CONSTRAINT chk_ai_provider_metrics_avg_latency_nonnegative CHECK (avg_latency_ms >= 0),
    CONSTRAINT chk_ai_provider_metrics_avg_cost_nonnegative CHECK (avg_cost_usd >= 0),
    CONSTRAINT chk_ai_provider_metrics_avg_quality_nonnegative CHECK (avg_quality_score >= 0),
    CONSTRAINT chk_ai_provider_metrics_uptime_nonnegative CHECK (uptime_percentage >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_provider_metrics_provider_model_active
    ON platform.ai_provider_metrics (provider_id, model_name)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_provider_metrics_provider
    ON platform.ai_provider_metrics (provider_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_provider_metrics_last_failure
    ON platform.ai_provider_metrics (last_failure_at)
    WHERE is_deleted = FALSE AND last_failure_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.ai_layer_analytics (
    id UUID PRIMARY KEY,
    layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_name VARCHAR(160) NOT NULL,
    total_executions BIGINT NOT NULL DEFAULT 0,
    successful_executions BIGINT NOT NULL DEFAULT 0,
    failed_executions BIGINT NOT NULL DEFAULT 0,
    avg_execution_time_ms NUMERIC(19, 4) NOT NULL DEFAULT 0,
    avg_execution_cost_usd NUMERIC(19, 6) NOT NULL DEFAULT 0,
    avg_quality_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_ai_layer_analytics_total_executions_nonnegative CHECK (total_executions >= 0),
    CONSTRAINT chk_ai_layer_analytics_successful_executions_nonnegative CHECK (successful_executions >= 0),
    CONSTRAINT chk_ai_layer_analytics_failed_executions_nonnegative CHECK (failed_executions >= 0),
    CONSTRAINT chk_ai_layer_analytics_avg_time_nonnegative CHECK (avg_execution_time_ms >= 0),
    CONSTRAINT chk_ai_layer_analytics_avg_cost_nonnegative CHECK (avg_execution_cost_usd >= 0),
    CONSTRAINT chk_ai_layer_analytics_avg_quality_nonnegative CHECK (avg_quality_score >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_layer_analytics_layer_provider_model_active
    ON platform.ai_layer_analytics (layer_id, provider_id, model_name)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_layer_analytics_layer
    ON platform.ai_layer_analytics (layer_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_layer_analytics_provider
    ON platform.ai_layer_analytics (provider_id)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.workspace_ai_usage (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces(id) ON DELETE CASCADE,
    total_generation_requests BIGINT NOT NULL DEFAULT 0,
    total_generated_versions BIGINT NOT NULL DEFAULT 0,
    total_credits_consumed NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_estimated_cost_usd NUMERIC(19, 6) NOT NULL DEFAULT 0,
    total_failures BIGINT NOT NULL DEFAULT 0,
    avg_generation_time_ms NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_workspace_ai_usage_generation_requests_nonnegative CHECK (total_generation_requests >= 0),
    CONSTRAINT chk_workspace_ai_usage_generated_versions_nonnegative CHECK (total_generated_versions >= 0),
    CONSTRAINT chk_workspace_ai_usage_credits_nonnegative CHECK (total_credits_consumed >= 0),
    CONSTRAINT chk_workspace_ai_usage_cost_nonnegative CHECK (total_estimated_cost_usd >= 0),
    CONSTRAINT chk_workspace_ai_usage_failures_nonnegative CHECK (total_failures >= 0),
    CONSTRAINT chk_workspace_ai_usage_avg_time_nonnegative CHECK (avg_generation_time_ms >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_workspace_ai_usage_workspace_active
    ON platform.workspace_ai_usage (workspace_id)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.ai_quality_scores (
    id UUID PRIMARY KEY,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces(id) ON DELETE CASCADE,
    overall_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    text_readability_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    product_preservation_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    branding_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    bangla_typography_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    composition_score NUMERIC(8, 4) NOT NULL DEFAULT 0,
    quality_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_ai_quality_scores_overall_nonnegative CHECK (overall_score >= 0),
    CONSTRAINT chk_ai_quality_scores_text_nonnegative CHECK (text_readability_score >= 0),
    CONSTRAINT chk_ai_quality_scores_product_nonnegative CHECK (product_preservation_score >= 0),
    CONSTRAINT chk_ai_quality_scores_branding_nonnegative CHECK (branding_score >= 0),
    CONSTRAINT chk_ai_quality_scores_bangla_nonnegative CHECK (bangla_typography_score >= 0),
    CONSTRAINT chk_ai_quality_scores_composition_nonnegative CHECK (composition_score >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_quality_scores_generated_version_active
    ON platform.ai_quality_scores (generated_version_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_quality_scores_workspace
    ON platform.ai_quality_scores (workspace_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.ai_failure_logs (
    id UUID PRIMARY KEY,
    creative_request_id UUID NOT NULL REFERENCES platform.creative_requests(id) ON DELETE CASCADE,
    layer_id UUID NOT NULL REFERENCES platform.creative_pipeline_layers(id),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    model_name VARCHAR(160) NOT NULL,
    failure_type VARCHAR(40) NOT NULL,
    failure_reason TEXT NOT NULL,
    retry_attempt INTEGER NOT NULL DEFAULT 0,
    fallback_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_ai_failure_logs_type CHECK (failure_type IN (
        'TIMEOUT',
        'RATE_LIMIT',
        'PROVIDER_DOWN',
        'INVALID_RESPONSE',
        'QUALITY_FAILURE',
        'COST_LIMIT_EXCEEDED',
        'UNKNOWN'
    )),
    CONSTRAINT chk_ai_failure_logs_retry_nonnegative CHECK (retry_attempt >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ai_failure_logs_creative_request
    ON platform.ai_failure_logs (creative_request_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_failure_logs_layer
    ON platform.ai_failure_logs (layer_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_failure_logs_provider
    ON platform.ai_failure_logs (provider_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_ai_failure_logs_type
    ON platform.ai_failure_logs (failure_type, created_at DESC)
    WHERE is_deleted = FALSE;
