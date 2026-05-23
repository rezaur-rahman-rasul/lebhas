CREATE TABLE IF NOT EXISTS platform.prompt_templates (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES platform.workspaces (id),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    platform VARCHAR(40),
    campaign_objective VARCHAR(40),
    business_type VARCHAR(80),
    language VARCHAR(30),
    template_text TEXT NOT NULL,
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_workspace_id ON platform.prompt_templates (workspace_id);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_platform ON platform.prompt_templates (platform);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_campaign_objective ON platform.prompt_templates (campaign_objective);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_language ON platform.prompt_templates (language);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_status ON platform.prompt_templates (status);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_system_default ON platform.prompt_templates (is_system_default);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_workspace_status_updated_at
    ON platform.prompt_templates (workspace_id, status, updated_at DESC);

CREATE TABLE IF NOT EXISTS platform.prompt_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    user_id UUID NOT NULL REFERENCES platform.users (id),
    source_prompt TEXT NOT NULL,
    enhanced_prompt TEXT,
    language VARCHAR(30),
    platform VARCHAR(40),
    campaign_objective VARCHAR(40),
    business_type VARCHAR(80),
    brand_context_snapshot TEXT,
    suggestion_type VARCHAR(40) NOT NULL,
    ai_provider VARCHAR(60),
    ai_model VARCHAR(120),
    token_usage INTEGER,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_prompt_history_workspace_id ON platform.prompt_history (workspace_id);
CREATE INDEX IF NOT EXISTS idx_prompt_history_user_id ON platform.prompt_history (user_id);
CREATE INDEX IF NOT EXISTS idx_prompt_history_platform ON platform.prompt_history (platform);
CREATE INDEX IF NOT EXISTS idx_prompt_history_campaign_objective ON platform.prompt_history (campaign_objective);
CREATE INDEX IF NOT EXISTS idx_prompt_history_created_at ON platform.prompt_history (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_prompt_history_workspace_created_at
    ON platform.prompt_history (workspace_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_prompt_history_workspace_user_created_at
    ON platform.prompt_history (workspace_id, user_id, created_at DESC);

INSERT INTO platform.permissions (code, description)
VALUES
    ('PROMPT_TEMPLATE_VIEW', 'View prompt templates'),
    ('PROMPT_TEMPLATE_MANAGE', 'Manage prompt templates'),
    ('PROMPT_INTELLIGENCE_USE', 'Use prompt intelligence APIs'),
    ('PROMPT_HISTORY_VIEW', 'View prompt history')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'PROMPT_TEMPLATE_VIEW'),
    ('MASTER', 'PROMPT_TEMPLATE_MANAGE'),
    ('MASTER', 'PROMPT_INTELLIGENCE_USE'),
    ('MASTER', 'PROMPT_HISTORY_VIEW'),
    ('ADMIN', 'PROMPT_TEMPLATE_VIEW'),
    ('ADMIN', 'PROMPT_TEMPLATE_MANAGE'),
    ('ADMIN', 'PROMPT_INTELLIGENCE_USE'),
    ('ADMIN', 'PROMPT_HISTORY_VIEW')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '6')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
