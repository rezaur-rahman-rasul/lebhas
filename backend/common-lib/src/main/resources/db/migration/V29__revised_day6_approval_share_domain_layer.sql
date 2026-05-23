CREATE UNIQUE INDEX IF NOT EXISTS uk_creative_requests_id_workspace_id
    ON platform.creative_requests (id, workspace_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_id_workspace_id
    ON platform.generated_versions (id, workspace_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_id_workspace_request
    ON platform.generated_versions (id, workspace_id, creative_request_id);

CREATE TABLE IF NOT EXISTS platform.approval_workflows (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    creative_request_id UUID NOT NULL,
    generated_version_id UUID NOT NULL,
    created_by UUID NOT NULL REFERENCES platform.users (id),
    current_status VARCHAR(40) NOT NULL,
    current_reviewer_id UUID REFERENCES platform.users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_approval_workflows_workspace_creative_request
        FOREIGN KEY (creative_request_id, workspace_id)
        REFERENCES platform.creative_requests (id, workspace_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_approval_workflows_workspace_generated_version
        FOREIGN KEY (generated_version_id, workspace_id, creative_request_id)
        REFERENCES platform.generated_versions (id, workspace_id, creative_request_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_approval_workflows_current_status
        CHECK (current_status IN ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'REVISION_REQUESTED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_workflows_workspace_generated_version
    ON platform.approval_workflows (workspace_id, generated_version_id);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_workspace_id
    ON platform.approval_workflows (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_creative_request_id
    ON platform.approval_workflows (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_generated_version_id
    ON platform.approval_workflows (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_workspace_status_created_at
    ON platform.approval_workflows (workspace_id, current_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_workspace_reviewer_created_at
    ON platform.approval_workflows (workspace_id, current_reviewer_id, created_at DESC)
    WHERE current_reviewer_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.approval_history (
    id UUID PRIMARY KEY,
    approval_workflow_id UUID NOT NULL REFERENCES platform.approval_workflows (id) ON DELETE CASCADE,
    action_by UUID NOT NULL REFERENCES platform.users (id),
    action_type VARCHAR(40) NOT NULL,
    comments VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approval_history_action_type
        CHECK (action_type IN ('APPROVE', 'REJECT', 'REQUEST_REVISION'))
);

CREATE INDEX IF NOT EXISTS idx_approval_history_workflow_created_at
    ON platform.approval_history (approval_workflow_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_approval_history_action_by
    ON platform.approval_history (action_by);

CREATE INDEX IF NOT EXISTS idx_approval_history_action_type
    ON platform.approval_history (action_type);

CREATE TABLE IF NOT EXISTS platform.share_links (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL,
    token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    access_count BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES platform.users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_share_links_workspace_generated_version
        FOREIGN KEY (generated_version_id, workspace_id)
        REFERENCES platform.generated_versions (id, workspace_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_share_links_access_count
        CHECK (access_count >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_share_links_token
    ON platform.share_links (token);

CREATE INDEX IF NOT EXISTS idx_share_links_workspace_id
    ON platform.share_links (workspace_id);

CREATE INDEX IF NOT EXISTS idx_share_links_generated_version_id
    ON platform.share_links (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_share_links_workspace_generated_version_created_at
    ON platform.share_links (workspace_id, generated_version_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_share_links_expires_at
    ON platform.share_links (expires_at);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '29')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
