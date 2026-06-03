ALTER TABLE platform.plan_feature_policies
    ADD COLUMN IF NOT EXISTS enabled_creative_tool_codes jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE TABLE IF NOT EXISTS platform.creative_text_tool_outputs (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    project_id uuid NOT NULL,
    brand_id uuid NOT NULL,
    product_service_id uuid,
    tool_type varchar(40) NOT NULL,
    tool_code varchar(120) NOT NULL,
    quality_mode varchar(20) NOT NULL,
    platform varchar(40) NOT NULL,
    language varchar(20) NOT NULL,
    tone varchar(120),
    campaign_objective varchar(40),
    source_idea varchar(2000),
    provider_id uuid,
    model_id uuid,
    credit_cost numeric(19,4) NOT NULL,
    credit_reservation_id uuid,
    selected_asset_ids jsonb NOT NULL DEFAULT '{}'::jsonb,
    output_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    created_by varchar(120),
    updated_by varchar(120),
    is_deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS platform.creative_text_tool_history (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    project_id uuid NOT NULL,
    text_tool_output_id uuid,
    tool_type varchar(40) NOT NULL,
    tool_code varchar(120) NOT NULL,
    status varchar(20) NOT NULL,
    credit_cost numeric(19,4) NOT NULL,
    failure_reason varchar(1000),
    request_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    response_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    created_by varchar(120),
    updated_by varchar(120),
    is_deleted boolean NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_text_tool_outputs_workspace_project_created
    ON platform.creative_text_tool_outputs (workspace_id, project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_text_tool_outputs_workspace_tool_created
    ON platform.creative_text_tool_outputs (workspace_id, tool_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_text_tool_history_workspace_project_created
    ON platform.creative_text_tool_history (workspace_id, project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_text_tool_history_output_id
    ON platform.creative_text_tool_history (text_tool_output_id);
