ALTER TABLE platform.prompt_templates
    ADD COLUMN IF NOT EXISTS category VARCHAR(80);

ALTER TABLE platform.prompt_templates
    DROP CONSTRAINT IF EXISTS chk_prompt_templates_workspace_scope;

ALTER TABLE platform.prompt_templates
    ADD CONSTRAINT chk_prompt_templates_workspace_scope
        CHECK (
            (is_system_default = TRUE AND workspace_id IS NULL)
            OR (is_system_default = FALSE AND workspace_id IS NOT NULL)
        );

CREATE INDEX IF NOT EXISTS idx_prompt_templates_workspace_category_updated_at
    ON platform.prompt_templates (workspace_id, category, updated_at DESC);

ALTER TABLE platform.prompt_history
    ADD COLUMN IF NOT EXISTS project_campaign_id UUID REFERENCES platform.project_campaigns (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS creative_request_id UUID REFERENCES platform.creative_requests (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_prompt_history_workspace_project_campaign_created_at
    ON platform.prompt_history (workspace_id, project_campaign_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_prompt_history_workspace_creative_request_created_at
    ON platform.prompt_history (workspace_id, creative_request_id, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.prompt_suggestions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    project_campaign_id UUID REFERENCES platform.project_campaigns (id) ON DELETE SET NULL,
    suggestion_type VARCHAR(40) NOT NULL,
    suggestion_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_workspace_id
    ON platform.prompt_suggestions (workspace_id);

CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_project_campaign_id
    ON platform.prompt_suggestions (project_campaign_id);

CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_workspace_type_created_at
    ON platform.prompt_suggestions (workspace_id, suggestion_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_prompt_suggestions_workspace_project_campaign_created_at
    ON platform.prompt_suggestions (workspace_id, project_campaign_id, created_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '14')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
