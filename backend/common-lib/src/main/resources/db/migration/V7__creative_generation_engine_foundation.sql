CREATE TABLE IF NOT EXISTS platform.creative_generation_requests (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    user_id UUID NOT NULL REFERENCES platform.users (id),
    prompt_history_id UUID REFERENCES platform.prompt_history (id),
    source_prompt TEXT NOT NULL,
    enhanced_prompt TEXT,
    platform VARCHAR(40) NOT NULL,
    campaign_objective VARCHAR(40) NOT NULL,
    creative_type VARCHAR(40) NOT NULL,
    output_format VARCHAR(20) NOT NULL,
    language VARCHAR(30) NOT NULL,
    brand_context_snapshot TEXT,
    asset_context_snapshot TEXT,
    generation_config TEXT,
    status VARCHAR(30) NOT NULL,
    ai_provider VARCHAR(60),
    ai_model VARCHAR(120),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_workspace_id
    ON platform.creative_generation_requests (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_user_id
    ON platform.creative_generation_requests (user_id);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_status
    ON platform.creative_generation_requests (status);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_creative_type
    ON platform.creative_generation_requests (creative_type);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_platform
    ON platform.creative_generation_requests (platform);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_created_at
    ON platform.creative_generation_requests (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_workspace_status_created_at
    ON platform.creative_generation_requests (workspace_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_workspace_user_created_at
    ON platform.creative_generation_requests (workspace_id, user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_prompt_history_id
    ON platform.creative_generation_requests (prompt_history_id);

CREATE TABLE IF NOT EXISTS platform.generation_jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    request_id UUID NOT NULL REFERENCES platform.creative_generation_requests (id) ON DELETE CASCADE,
    job_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_job_id VARCHAR(160),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    queue_name VARCHAR(160) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_id
    ON platform.generation_jobs (workspace_id);
CREATE INDEX IF NOT EXISTS idx_generation_jobs_request_id
    ON platform.generation_jobs (request_id);
CREATE INDEX IF NOT EXISTS idx_generation_jobs_status
    ON platform.generation_jobs (status);
CREATE INDEX IF NOT EXISTS idx_generation_jobs_job_type
    ON platform.generation_jobs (job_type);
CREATE INDEX IF NOT EXISTS idx_generation_jobs_created_at
    ON platform.generation_jobs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_status_created_at
    ON platform.generation_jobs (workspace_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.creative_outputs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    request_id UUID NOT NULL REFERENCES platform.creative_generation_requests (id) ON DELETE CASCADE,
    generated_asset_id UUID REFERENCES platform.assets (id),
    creative_type VARCHAR(40) NOT NULL,
    platform VARCHAR(40) NOT NULL,
    output_format VARCHAR(20) NOT NULL,
    width INTEGER,
    height INTEGER,
    duration BIGINT,
    file_size BIGINT,
    preview_url VARCHAR(1000),
    download_url VARCHAR(1000),
    caption VARCHAR(500),
    headline VARCHAR(240),
    cta_text VARCHAR(120),
    metadata TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_outputs_workspace_id
    ON platform.creative_outputs (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_request_id
    ON platform.creative_outputs (request_id);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_generated_asset_id
    ON platform.creative_outputs (generated_asset_id);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_status
    ON platform.creative_outputs (status);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_creative_type
    ON platform.creative_outputs (creative_type);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_platform
    ON platform.creative_outputs (platform);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_created_at
    ON platform.creative_outputs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_outputs_workspace_request_created_at
    ON platform.creative_outputs (workspace_id, request_id, created_at ASC);

INSERT INTO platform.permissions (code, description)
VALUES
    ('CREATIVE_GENERATE', 'Generate creatives'),
    ('CREATIVE_DOWNLOAD', 'Download generated creatives')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'CREATIVE_GENERATE'),
    ('MASTER', 'CREATIVE_DOWNLOAD'),
    ('ADMIN', 'CREATIVE_GENERATE'),
    ('ADMIN', 'CREATIVE_DOWNLOAD')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.workspace_membership_permissions (membership_id, permission_code)
SELECT membership.id, 'CREATIVE_GENERATE'
FROM platform.workspace_memberships membership
WHERE membership.role = 'CREW'
ON CONFLICT (membership_id, permission_code) DO NOTHING;

INSERT INTO platform.invitation_permissions (invitation_id, permission_code)
SELECT invitation.id, 'CREATIVE_GENERATE'
FROM platform.invitations invitation
WHERE invitation.role = 'CREW'
ON CONFLICT (invitation_id, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '7')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
