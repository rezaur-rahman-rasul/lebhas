CREATE TABLE IF NOT EXISTS platform.prompt_drafts (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces(id),
    project_id UUID NOT NULL REFERENCES platform.project_campaigns(id),
    created_by_user_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    prompt_text TEXT NOT NULL,
    language VARCHAR(30),
    platform VARCHAR(40),
    campaign_objective VARCHAR(40),
    template_id UUID REFERENCES platform.prompt_templates(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_prompt_drafts_workspace_project_updated
    ON platform.prompt_drafts (workspace_id, project_id, updated_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_prompt_drafts_template
    ON platform.prompt_drafts (template_id)
    WHERE template_id IS NOT NULL AND is_deleted = FALSE;
