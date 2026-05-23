ALTER TABLE platform.prompt_templates
    ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE platform.prompt_templates
SET is_public = TRUE
WHERE is_system_default = TRUE;

CREATE INDEX IF NOT EXISTS idx_prompt_templates_is_public
    ON platform.prompt_templates (is_public);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_workspace_is_public_updated_at
    ON platform.prompt_templates (workspace_id, is_public, updated_at DESC);

CREATE TABLE IF NOT EXISTS platform.prompt_enhancement_history (
    id UUID PRIMARY KEY,
    creative_request_id UUID NOT NULL REFERENCES platform.creative_requests (id) ON DELETE CASCADE,
    original_prompt TEXT NOT NULL,
    enhanced_prompt TEXT NOT NULL,
    enhancement_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.prompt_enhancement_history
    DROP CONSTRAINT IF EXISTS chk_prompt_enhancement_history_enhancement_type;

ALTER TABLE platform.prompt_enhancement_history
    ADD CONSTRAINT chk_prompt_enhancement_history_enhancement_type
        CHECK (enhancement_type IN ('ENHANCE', 'OPTIMIZE', 'TRANSLATE'));

CREATE INDEX IF NOT EXISTS idx_prompt_enhancement_history_creative_request_id
    ON platform.prompt_enhancement_history (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_prompt_enhancement_history_created_at
    ON platform.prompt_enhancement_history (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_prompt_enhancement_history_request_created_at
    ON platform.prompt_enhancement_history (creative_request_id, created_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '26')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
