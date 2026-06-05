CREATE TABLE IF NOT EXISTS platform.creative_pipeline_runs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    creative_request_id UUID NOT NULL,
    primary_provider_code VARCHAR(80) NOT NULL,
    strategy VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    plan_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    estimated_credit_cost NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_credit_cost NUMERIC(19,4),
    failure_reason VARCHAR(1000),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_creative_pipeline_runs_workspace_request
    ON platform.creative_pipeline_runs (workspace_id, creative_request_id, created_at DESC)
    WHERE is_deleted = false;

CREATE TABLE IF NOT EXISTS platform.creative_pipeline_layer_runs (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL REFERENCES platform.creative_pipeline_runs(id),
    creative_request_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    layer_type VARCHAR(60) NOT NULL,
    provider_code VARCHAR(80) NOT NULL,
    model_code VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    input_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    input_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    output_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    estimated_cost NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_cost NUMERIC(19,4),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_creative_pipeline_layer_runs_run_sequence
    ON platform.creative_pipeline_layer_runs (pipeline_run_id, sequence_number);
