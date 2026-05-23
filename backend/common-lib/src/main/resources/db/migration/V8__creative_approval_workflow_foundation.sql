CREATE TABLE IF NOT EXISTS platform.creative_approvals (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    creative_output_id UUID NOT NULL REFERENCES platform.creative_outputs (id),
    generation_request_id UUID NOT NULL REFERENCES platform.creative_generation_requests (id),
    submitted_by UUID NOT NULL REFERENCES platform.users (id),
    reviewed_by UUID REFERENCES platform.users (id),
    status VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMPTZ,
    review_started_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    approval_note VARCHAR(2000),
    rejection_reason VARCHAR(2000),
    regenerate_instruction VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_approvals_workspace_id
    ON platform.creative_approvals (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_creative_output_id
    ON platform.creative_approvals (creative_output_id);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_generation_request_id
    ON platform.creative_approvals (generation_request_id);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_submitted_by
    ON platform.creative_approvals (submitted_by);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_reviewed_by
    ON platform.creative_approvals (reviewed_by);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_status
    ON platform.creative_approvals (status);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_submitted_at
    ON platform.creative_approvals (submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_reviewed_at
    ON platform.creative_approvals (reviewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_creative_approvals_workspace_status_created_at
    ON platform.creative_approvals (workspace_id, status, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_creative_approvals_active_output
    ON platform.creative_approvals (workspace_id, creative_output_id)
    WHERE is_deleted = FALSE
      AND status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'REGENERATE_REQUESTED');

CREATE TABLE IF NOT EXISTS platform.creative_review_comments (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    approval_id UUID NOT NULL REFERENCES platform.creative_approvals (id) ON DELETE CASCADE,
    creative_output_id UUID NOT NULL REFERENCES platform.creative_outputs (id),
    author_id UUID NOT NULL REFERENCES platform.users (id),
    comment VARCHAR(2000) NOT NULL,
    comment_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_review_comments_workspace_id
    ON platform.creative_review_comments (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_review_comments_approval_id
    ON platform.creative_review_comments (approval_id);
CREATE INDEX IF NOT EXISTS idx_creative_review_comments_creative_output_id
    ON platform.creative_review_comments (creative_output_id);
CREATE INDEX IF NOT EXISTS idx_creative_review_comments_author_id
    ON platform.creative_review_comments (author_id);
CREATE INDEX IF NOT EXISTS idx_creative_review_comments_comment_type
    ON platform.creative_review_comments (comment_type);

CREATE TABLE IF NOT EXISTS platform.creative_approval_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    approval_id UUID NOT NULL REFERENCES platform.creative_approvals (id) ON DELETE CASCADE,
    creative_output_id UUID NOT NULL REFERENCES platform.creative_outputs (id),
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(40),
    new_status VARCHAR(40),
    actor_id UUID NOT NULL REFERENCES platform.users (id),
    note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_creative_approval_history_workspace_id
    ON platform.creative_approval_history (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_approval_history_approval_id
    ON platform.creative_approval_history (approval_id);
CREATE INDEX IF NOT EXISTS idx_creative_approval_history_creative_output_id
    ON platform.creative_approval_history (creative_output_id);
CREATE INDEX IF NOT EXISTS idx_creative_approval_history_actor_id
    ON platform.creative_approval_history (actor_id);
CREATE INDEX IF NOT EXISTS idx_creative_approval_history_action
    ON platform.creative_approval_history (action);
CREATE INDEX IF NOT EXISTS idx_creative_approval_history_created_at
    ON platform.creative_approval_history (created_at ASC);

INSERT INTO platform.permissions (code, description)
VALUES
    ('CREATIVE_SUBMIT', 'Submit creatives for review')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'CREATIVE_SUBMIT'),
    ('ADMIN', 'CREATIVE_SUBMIT')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '8')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
