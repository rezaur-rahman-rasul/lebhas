CREATE TABLE IF NOT EXISTS platform.approval_requests (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    project_campaign_id UUID NOT NULL REFERENCES platform.project_campaigns (id) ON DELETE CASCADE,
    submitted_by UUID NOT NULL REFERENCES platform.users (id),
    assigned_reviewer_id UUID REFERENCES platform.users (id),
    current_status VARCHAR(40) NOT NULL,
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    latest_comment VARCHAR(2000),
    revision_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.approval_requests
    DROP CONSTRAINT IF EXISTS chk_approval_requests_current_status;

ALTER TABLE platform.approval_requests
    ADD CONSTRAINT chk_approval_requests_current_status
        CHECK (current_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'CANCELLED'));

ALTER TABLE platform.approval_requests
    DROP CONSTRAINT IF EXISTS chk_approval_requests_revision_count;

ALTER TABLE platform.approval_requests
    ADD CONSTRAINT chk_approval_requests_revision_count
        CHECK (revision_count >= 0);

CREATE INDEX IF NOT EXISTS idx_approval_requests_workspace_id
    ON platform.approval_requests (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_requests_generated_version_id
    ON platform.approval_requests (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_approval_requests_project_campaign_id
    ON platform.approval_requests (project_campaign_id);

CREATE INDEX IF NOT EXISTS idx_approval_requests_reviewer_id
    ON platform.approval_requests (assigned_reviewer_id);

CREATE INDEX IF NOT EXISTS idx_approval_requests_status
    ON platform.approval_requests (current_status);

CREATE INDEX IF NOT EXISTS idx_approval_requests_submitted_by
    ON platform.approval_requests (submitted_by);

CREATE INDEX IF NOT EXISTS idx_approval_requests_reviewed_at
    ON platform.approval_requests (reviewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_requests_created_at
    ON platform.approval_requests (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_requests_workspace_status_created_at
    ON platform.approval_requests (workspace_id, current_status, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_requests_active_generated_version
    ON platform.approval_requests (workspace_id, generated_version_id)
    WHERE is_deleted = FALSE
      AND current_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'CHANGES_REQUESTED');

CREATE TABLE IF NOT EXISTS platform.approval_reviews (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    approval_request_id UUID NOT NULL REFERENCES platform.approval_requests (id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES platform.users (id),
    decision VARCHAR(40) NOT NULL,
    feedback VARCHAR(2000),
    review_type VARCHAR(40) NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.approval_reviews
    DROP CONSTRAINT IF EXISTS chk_approval_reviews_decision;

ALTER TABLE platform.approval_reviews
    ADD CONSTRAINT chk_approval_reviews_decision
        CHECK (decision IN ('APPROVED', 'REJECTED', 'CHANGES_REQUESTED'));

ALTER TABLE platform.approval_reviews
    DROP CONSTRAINT IF EXISTS chk_approval_reviews_review_type;

ALTER TABLE platform.approval_reviews
    ADD CONSTRAINT chk_approval_reviews_review_type
        CHECK (review_type IN ('INITIAL', 'RESUBMISSION'));

CREATE INDEX IF NOT EXISTS idx_approval_reviews_workspace_id
    ON platform.approval_reviews (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_reviews_approval_request_id
    ON platform.approval_reviews (approval_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_reviews_reviewer_id
    ON platform.approval_reviews (reviewer_id);

CREATE INDEX IF NOT EXISTS idx_approval_reviews_reviewed_at
    ON platform.approval_reviews (reviewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_reviews_created_at
    ON platform.approval_reviews (created_at DESC);

CREATE TABLE IF NOT EXISTS platform.approval_comments (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    approval_request_id UUID NOT NULL REFERENCES platform.approval_requests (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    commented_by UUID NOT NULL REFERENCES platform.users (id),
    comment_text VARCHAR(2000) NOT NULL,
    internal_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_approval_comments_workspace_id
    ON platform.approval_comments (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_comments_generated_version_id
    ON platform.approval_comments (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_approval_comments_approval_request_id
    ON platform.approval_comments (approval_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_comments_commented_by
    ON platform.approval_comments (commented_by);

CREATE INDEX IF NOT EXISTS idx_approval_comments_created_at
    ON platform.approval_comments (created_at DESC);

CREATE TABLE IF NOT EXISTS platform.approval_assignments (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    approval_request_id UUID NOT NULL REFERENCES platform.approval_requests (id) ON DELETE CASCADE,
    assigned_to UUID NOT NULL REFERENCES platform.users (id),
    assigned_by UUID NOT NULL REFERENCES platform.users (id),
    assigned_at TIMESTAMPTZ NOT NULL,
    assignment_status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.approval_assignments
    DROP CONSTRAINT IF EXISTS chk_approval_assignments_assignment_status;

ALTER TABLE platform.approval_assignments
    ADD CONSTRAINT chk_approval_assignments_assignment_status
        CHECK (assignment_status IN ('ACTIVE', 'COMPLETED', 'REASSIGNED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_approval_assignments_workspace_id
    ON platform.approval_assignments (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_assignments_approval_request_id
    ON platform.approval_assignments (approval_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_assignments_assigned_to
    ON platform.approval_assignments (assigned_to);

CREATE INDEX IF NOT EXISTS idx_approval_assignments_created_at
    ON platform.approval_assignments (created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_assignments_active_request
    ON platform.approval_assignments (approval_request_id)
    WHERE is_deleted = FALSE
      AND assignment_status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS platform.approval_audit_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    approval_request_id UUID NOT NULL REFERENCES platform.approval_requests (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES platform.users (id),
    action VARCHAR(40) NOT NULL,
    previous_status VARCHAR(40),
    new_status VARCHAR(40),
    details VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_action;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_action
        CHECK (action IN ('SUBMITTED', 'REVIEW_STARTED', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'REASSIGNED', 'COMMENT_CREATED', 'RESUBMITTED', 'CANCELLED'));

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_previous_status;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_previous_status
        CHECK (previous_status IS NULL OR previous_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'CANCELLED'));

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_new_status;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_new_status
        CHECK (new_status IS NULL OR new_status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'CANCELLED'));

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_workspace_id
    ON platform.approval_audit_logs (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_generated_version_id
    ON platform.approval_audit_logs (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_approval_request_id
    ON platform.approval_audit_logs (approval_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_actor_id
    ON platform.approval_audit_logs (actor_id);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_created_at
    ON platform.approval_audit_logs (created_at DESC);

INSERT INTO platform.approval_requests (
    id,
    workspace_id,
    generated_version_id,
    project_campaign_id,
    submitted_by,
    assigned_reviewer_id,
    current_status,
    submitted_at,
    reviewed_at,
    due_at,
    latest_comment,
    revision_count,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    approval.id,
    approval.workspace_id,
    approval.creative_output_id,
    approval.project_id,
    approval.submitted_by,
    approval.reviewed_by,
    CASE approval.status
        WHEN 'DRAFT' THEN 'DRAFT'
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'IN_REVIEW' THEN 'IN_REVIEW'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'REGENERATE_REQUESTED' THEN 'CHANGES_REQUESTED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'DRAFT'
    END,
    approval.submitted_at,
    approval.reviewed_at,
    approval.due_at,
    latest_comment.comment_text,
    COALESCE(revision_summary.revision_count, 0),
    approval.created_at,
    approval.updated_at,
    approval.created_by,
    approval.updated_by,
    approval.is_deleted
FROM platform.creative_approvals approval
JOIN platform.generated_versions generated_version
    ON generated_version.id = approval.creative_output_id
LEFT JOIN LATERAL (
    SELECT comment.comment AS comment_text
    FROM platform.creative_review_comments comment
    WHERE comment.approval_id = approval.id
      AND comment.is_deleted = FALSE
    ORDER BY comment.created_at DESC
    LIMIT 1
) latest_comment ON TRUE
LEFT JOIN LATERAL (
    SELECT COUNT(*)::INTEGER AS revision_count
    FROM platform.creative_approval_history history
    WHERE history.approval_id = approval.id
      AND history.action = 'REGENERATE_REQUESTED'
) revision_summary ON TRUE
ON CONFLICT (id) DO NOTHING;

INSERT INTO platform.approval_comments (
    id,
    workspace_id,
    approval_request_id,
    generated_version_id,
    commented_by,
    comment_text,
    internal_only,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    comment.id,
    comment.workspace_id,
    comment.approval_id,
    comment.creative_output_id,
    comment.author_id,
    comment.comment,
    FALSE,
    comment.created_at,
    comment.updated_at,
    comment.created_by,
    comment.updated_by,
    comment.is_deleted
FROM platform.creative_review_comments comment
JOIN platform.approval_requests approval_request
    ON approval_request.id = comment.approval_id
ON CONFLICT (id) DO NOTHING;

INSERT INTO platform.approval_reviews (
    id,
    workspace_id,
    approval_request_id,
    reviewer_id,
    decision,
    feedback,
    review_type,
    reviewed_at,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    approval.id,
    approval.workspace_id,
    approval.id,
    approval.reviewed_by,
    CASE approval.status
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'REGENERATE_REQUESTED' THEN 'CHANGES_REQUESTED'
    END,
    CASE approval.status
        WHEN 'APPROVED' THEN approval.approval_note
        WHEN 'REJECTED' THEN approval.rejection_reason
        WHEN 'REGENERATE_REQUESTED' THEN approval.regenerate_instruction
    END,
    CASE
        WHEN COALESCE(revision_summary.revision_count, 0) > 0 THEN 'RESUBMISSION'
        ELSE 'INITIAL'
    END,
    COALESCE(approval.reviewed_at, approval.updated_at, approval.created_at),
    approval.created_at,
    approval.updated_at,
    approval.created_by,
    approval.updated_by,
    approval.is_deleted
FROM platform.creative_approvals approval
LEFT JOIN LATERAL (
    SELECT COUNT(*)::INTEGER AS revision_count
    FROM platform.creative_approval_history history
    WHERE history.approval_id = approval.id
      AND history.action = 'REGENERATE_REQUESTED'
) revision_summary ON TRUE
WHERE approval.reviewed_by IS NOT NULL
  AND approval.status IN ('APPROVED', 'REJECTED', 'REGENERATE_REQUESTED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO platform.approval_assignments (
    id,
    workspace_id,
    approval_request_id,
    assigned_to,
    assigned_by,
    assigned_at,
    assignment_status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    approval.id,
    approval.workspace_id,
    approval.id,
    approval.reviewed_by,
    approval.submitted_by,
    COALESCE(approval.review_started_at, approval.submitted_at, approval.created_at),
    CASE approval.status
        WHEN 'APPROVED' THEN 'COMPLETED'
        WHEN 'REJECTED' THEN 'COMPLETED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'ACTIVE'
    END,
    approval.created_at,
    approval.updated_at,
    approval.created_by,
    approval.updated_by,
    approval.is_deleted
FROM platform.creative_approvals approval
WHERE approval.reviewed_by IS NOT NULL
ON CONFLICT (id) DO NOTHING;

INSERT INTO platform.approval_audit_logs (
    id,
    workspace_id,
    approval_request_id,
    generated_version_id,
    actor_id,
    action,
    previous_status,
    new_status,
    details,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    history.id,
    history.workspace_id,
    history.approval_id,
    history.creative_output_id,
    history.actor_id,
    CASE
        WHEN history.action = 'SUBMITTED' AND history.previous_status = 'REGENERATE_REQUESTED' THEN 'RESUBMITTED'
        WHEN history.action = 'SUBMITTED' THEN 'SUBMITTED'
        WHEN history.action = 'REVIEW_STARTED' THEN 'REVIEW_STARTED'
        WHEN history.action = 'APPROVED' THEN 'APPROVED'
        WHEN history.action = 'REJECTED' THEN 'REJECTED'
        WHEN history.action = 'REGENERATE_REQUESTED' THEN 'CHANGES_REQUESTED'
        WHEN history.action = 'COMMENT_ADDED' THEN 'COMMENT_CREATED'
        WHEN history.action = 'CANCELLED' THEN 'CANCELLED'
    END,
    CASE history.previous_status
        WHEN 'DRAFT' THEN 'DRAFT'
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'IN_REVIEW' THEN 'IN_REVIEW'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'REGENERATE_REQUESTED' THEN 'CHANGES_REQUESTED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
    END,
    CASE history.new_status
        WHEN 'DRAFT' THEN 'DRAFT'
        WHEN 'SUBMITTED' THEN 'SUBMITTED'
        WHEN 'IN_REVIEW' THEN 'IN_REVIEW'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'REGENERATE_REQUESTED' THEN 'CHANGES_REQUESTED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
    END,
    history.note,
    history.created_at,
    history.created_at,
    NULL,
    NULL,
    FALSE
FROM platform.creative_approval_history history
JOIN platform.approval_requests approval_request
    ON approval_request.id = history.approval_id
WHERE history.action IN ('SUBMITTED', 'REVIEW_STARTED', 'APPROVED', 'REJECTED', 'REGENERATE_REQUESTED', 'COMMENT_ADDED', 'CANCELLED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '16')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
