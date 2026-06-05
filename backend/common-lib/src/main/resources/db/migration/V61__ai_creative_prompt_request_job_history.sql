CREATE TABLE IF NOT EXISTS platform.creative_prompt_requests (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID NOT NULL,
    product_service_id UUID,
    campaign_id UUID,
    prompt_title VARCHAR(500) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    creative_type VARCHAR(60) NOT NULL,
    aspect_ratio VARCHAR(20),
    size VARCHAR(40),
    language VARCHAR(40),
    tone VARCHAR(60),
    model_quality VARCHAR(60),
    headline VARCHAR(1000),
    subheadline VARCHAR(1000),
    offer_text VARCHAR(1000),
    cta_text VARCHAR(255),
    campaign_idea TEXT,
    campaign_objective TEXT,
    target_audience TEXT,
    product_description TEXT,
    brand_voice_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    product_tone_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    visual_theme_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    generation_mode VARCHAR(60) NOT NULL,
    fixed_rules_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    variable_inputs_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    image_inputs_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    final_prompt TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_creative_prompt_requests_status
        CHECK (status IN ('REQUESTED', 'PLANNING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_creative_prompt_requests_generation_mode
        CHECK (generation_mode IN (
            'TEXT_ONLY_CREATIVE',
            'PRODUCT_IMAGE_CREATIVE',
            'MULTI_REFERENCE_CREATIVE',
            'BACKGROUND_REPLACEMENT',
            'TRANSPARENT_ASSET'
        ))
);

CREATE INDEX IF NOT EXISTS idx_creative_prompt_requests_workspace_brand_created
    ON platform.creative_prompt_requests (workspace_id, brand_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_creative_prompt_requests_product_service
    ON platform.creative_prompt_requests (product_service_id)
    WHERE product_service_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_creative_prompt_requests_campaign
    ON platform.creative_prompt_requests (campaign_id)
    WHERE campaign_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.creative_generation_jobs (
    id UUID PRIMARY KEY,
    prompt_request_id UUID NOT NULL REFERENCES platform.creative_prompt_requests (id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID NOT NULL,
    product_service_id UUID,
    campaign_id UUID,
    prompt_title VARCHAR(500) NOT NULL,
    provider VARCHAR(120) NOT NULL,
    model VARCHAR(160),
    generation_mode VARCHAR(60) NOT NULL,
    execution_plan_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    estimated_credit_cost NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_credit_used NUMERIC(19,4),
    status VARCHAR(30) NOT NULL,
    final_output_asset_id VARCHAR(600),
    file_url VARCHAR(1200),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_creative_generation_jobs_status
        CHECK (status IN ('QUEUED', 'PLANNING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_creative_generation_jobs_generation_mode
        CHECK (generation_mode IN (
            'TEXT_ONLY_CREATIVE',
            'PRODUCT_IMAGE_CREATIVE',
            'MULTI_REFERENCE_CREATIVE',
            'BACKGROUND_REPLACEMENT',
            'TRANSPARENT_ASSET'
        ))
);

CREATE INDEX IF NOT EXISTS idx_creative_generation_jobs_prompt_request
    ON platform.creative_generation_jobs (prompt_request_id);

CREATE INDEX IF NOT EXISTS idx_creative_generation_jobs_workspace_created
    ON platform.creative_generation_jobs (workspace_id, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.creative_generation_job_layers (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES platform.creative_generation_jobs (id) ON DELETE CASCADE,
    sequence_no INTEGER NOT NULL,
    layer_key VARCHAR(120) NOT NULL,
    layer_type VARCHAR(120) NOT NULL,
    provider VARCHAR(120) NOT NULL,
    model VARCHAR(160),
    status VARCHAR(30) NOT NULL,
    input_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    output_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    estimated_cost NUMERIC(19,4) NOT NULL DEFAULT 0,
    actual_cost NUMERIC(19,4),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_creative_generation_job_layers_status
        CHECK (status IN ('PLANNED', 'PROCESSING', 'COMPLETED', 'FAILED', 'SKIPPED', 'FALLBACK_USED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_creative_generation_job_layers_job_sequence
    ON platform.creative_generation_job_layers (job_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_creative_generation_job_layers_job_status
    ON platform.creative_generation_job_layers (job_id, status);

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS prompt_request_id UUID,
    ADD COLUMN IF NOT EXISTS generation_job_id UUID,
    ADD COLUMN IF NOT EXISTS brand_id UUID,
    ADD COLUMN IF NOT EXISTS product_service_id UUID,
    ADD COLUMN IF NOT EXISTS campaign_id UUID,
    ADD COLUMN IF NOT EXISTS prompt_title VARCHAR(500),
    ADD COLUMN IF NOT EXISTS file_url VARCHAR(1200),
    ADD COLUMN IF NOT EXISTS r2_object_key VARCHAR(600),
    ADD COLUMN IF NOT EXISTS credit_used NUMERIC(19,4);

CREATE INDEX IF NOT EXISTS idx_generated_versions_prompt_request_id
    ON platform.generated_versions (prompt_request_id)
    WHERE prompt_request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_generated_versions_generation_job_id
    ON platform.generated_versions (generation_job_id)
    WHERE generation_job_id IS NOT NULL;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.ai_creative_prompt_request.version', '61')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
