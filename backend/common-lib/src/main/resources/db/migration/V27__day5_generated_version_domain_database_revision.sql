ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS generation_provider VARCHAR(120),
    ADD COLUMN IF NOT EXISTS generation_model VARCHAR(160),
    ADD COLUMN IF NOT EXISTS prompt_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS generated_asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS preview_asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS thumbnail_asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS generation_duration_ms BIGINT,
    ADD COLUMN IF NOT EXISTS generation_cost_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generation_cost_usd NUMERIC(19,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS width INTEGER,
    ADD COLUMN IF NOT EXISTS height INTEGER,
    ADD COLUMN IF NOT EXISTS format VARCHAR(40),
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;

ALTER TABLE platform.generated_versions
    ALTER COLUMN project_campaign_id DROP NOT NULL,
    ALTER COLUMN created_by_user_id DROP NOT NULL;

UPDATE platform.generated_versions generated_version
SET generation_provider = COALESCE(generated_version.generation_provider, NULLIF(generated_version.generated_by_provider, '')),
    generation_model = COALESCE(generated_version.generation_model, NULLIF(generated_version.generated_by_model, '')),
    generated_asset_id = COALESCE(generated_version.generated_asset_id, generated_version.asset_id),
    generation_cost_credits = COALESCE(generated_version.generation_cost_credits, 0),
    generation_cost_usd = COALESCE(generated_version.generation_cost_usd, 0),
    retry_count = COALESCE(generated_version.retry_count, 0);

UPDATE platform.generated_versions generated_version
SET generated_asset_id = COALESCE(generated_version.generated_asset_id, creative_output.generated_asset_id),
    width = COALESCE(generated_version.width, creative_output.width),
    height = COALESCE(generated_version.height, creative_output.height),
    format = COALESCE(generated_version.format, NULLIF(creative_output.output_format, ''))
FROM platform.creative_outputs creative_output
WHERE generated_version.id = creative_output.id;

UPDATE platform.generated_versions generated_version
SET prompt_snapshot = COALESCE(
        generated_version.prompt_snapshot,
        NULLIF(creative_request.enhanced_prompt, ''),
        creative_request.source_prompt
    ),
    format = COALESCE(generated_version.format, NULLIF(creative_request.requested_format, ''))
FROM platform.creative_requests creative_request
WHERE generated_version.creative_request_id = creative_request.id;

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_status;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_status
        CHECK (generation_status IN ('QUEUED', 'PROCESSING', 'READY', 'FAILED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_version_number;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_version_number
        CHECK (version_number >= 1);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_retry_count;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_retry_count
        CHECK (retry_count >= 0);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_duration_ms;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_duration_ms
        CHECK (generation_duration_ms IS NULL OR generation_duration_ms >= 0);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_cost_credits;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_cost_credits
        CHECK (generation_cost_credits >= 0);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_cost_usd;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_cost_usd
        CHECK (generation_cost_usd >= 0);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_width;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_width
        CHECK (width IS NULL OR width > 0);

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_height;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_height
        CHECK (height IS NULL OR height > 0);

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_workspace_request_version_number
    ON platform.generated_versions (workspace_id, creative_request_id, version_number)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_request_created_at
    ON platform.generated_versions (workspace_id, creative_request_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_request_status_created_at
    ON platform.generated_versions (workspace_id, creative_request_id, generation_status, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_generation_status_created_at
    ON platform.generated_versions (workspace_id, generation_status, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_versions_generated_asset_id
    ON platform.generated_versions (generated_asset_id);

CREATE INDEX IF NOT EXISTS idx_generated_versions_preview_asset_id
    ON platform.generated_versions (preview_asset_id);

CREATE INDEX IF NOT EXISTS idx_generated_versions_thumbnail_asset_id
    ON platform.generated_versions (thumbnail_asset_id);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_generated_asset_id
    ON platform.generated_versions (workspace_id, generated_asset_id)
    WHERE is_deleted = FALSE AND generated_asset_id IS NOT NULL;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '27')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
