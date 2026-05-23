UPDATE platform.approval_requests
SET current_status = 'NOT_SUBMITTED'
WHERE current_status = 'DRAFT';

UPDATE platform.approval_audit_logs
SET previous_status = 'NOT_SUBMITTED'
WHERE previous_status = 'DRAFT';

UPDATE platform.approval_audit_logs
SET new_status = 'NOT_SUBMITTED'
WHERE new_status = 'DRAFT';

ALTER TABLE platform.approval_requests
    DROP CONSTRAINT IF EXISTS chk_approval_requests_current_status;

ALTER TABLE platform.approval_requests
    ADD CONSTRAINT chk_approval_requests_current_status
        CHECK (current_status IN (
            'NOT_SUBMITTED',
            'SUBMITTED',
            'IN_REVIEW',
            'RESUBMITTED',
            'APPROVED',
            'REJECTED',
            'CHANGES_REQUESTED',
            'CANCELLED'
        ));

DROP INDEX IF EXISTS platform.uk_approval_requests_active_generated_version;

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_requests_active_generated_version
    ON platform.approval_requests (workspace_id, generated_version_id)
    WHERE is_deleted = FALSE
      AND current_status IN ('SUBMITTED', 'IN_REVIEW', 'RESUBMITTED', 'CHANGES_REQUESTED');

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_action;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_action
        CHECK (action IN (
            'SUBMITTED',
            'ASSIGNED',
            'REVIEW_STARTED',
            'APPROVED',
            'REJECTED',
            'CHANGES_REQUESTED',
            'REASSIGNED',
            'COMMENT_CREATED',
            'RESUBMITTED',
            'CANCELLED'
        ));

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_previous_status;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_previous_status
        CHECK (previous_status IS NULL OR previous_status IN (
            'NOT_SUBMITTED',
            'SUBMITTED',
            'IN_REVIEW',
            'RESUBMITTED',
            'APPROVED',
            'REJECTED',
            'CHANGES_REQUESTED',
            'CANCELLED'
        ));

ALTER TABLE platform.approval_audit_logs
    DROP CONSTRAINT IF EXISTS chk_approval_audit_logs_new_status;

ALTER TABLE platform.approval_audit_logs
    ADD CONSTRAINT chk_approval_audit_logs_new_status
        CHECK (new_status IS NULL OR new_status IN (
            'NOT_SUBMITTED',
            'SUBMITTED',
            'IN_REVIEW',
            'RESUBMITTED',
            'APPROVED',
            'REJECTED',
            'CHANGES_REQUESTED',
            'CANCELLED'
        ));

CREATE INDEX IF NOT EXISTS idx_approval_requests_workspace_reviewer_created_at
    ON platform.approval_requests (workspace_id, assigned_reviewer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_requests_workspace_submitted_by_created_at
    ON platform.approval_requests (workspace_id, submitted_by, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_requests_workspace_submitted_at
    ON platform.approval_requests (workspace_id, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_workspace_request_created_at
    ON platform.approval_audit_logs (workspace_id, approval_request_id, created_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '20')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
