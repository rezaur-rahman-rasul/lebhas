ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS submitted_for_approval_at TIMESTAMPTZ;

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS latest_approval_comment VARCHAR(2000);

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS latest_reviewer_id UUID REFERENCES platform.users (id) ON DELETE SET NULL;

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS revision_number INTEGER NOT NULL DEFAULT 0;

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS approval_completed_at TIMESTAMPTZ;

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_approval_status;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_approval_status
        CHECK (approval_status IN (
            'NOT_SUBMITTED',
            'SUBMITTED',
            'IN_REVIEW',
            'RESUBMITTED',
            'APPROVED',
            'REJECTED',
            'CHANGES_REQUESTED'
        ));

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_revision_number;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_revision_number
        CHECK (revision_number >= 0);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_approval_status_created_at
    ON platform.generated_versions (workspace_id, approval_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_generated_versions_latest_reviewer_id
    ON platform.generated_versions (latest_reviewer_id);

UPDATE platform.generated_versions generated_version
SET submitted_for_approval_at = approval_request.submitted_at,
    latest_approval_comment = COALESCE(latest_comment.comment_text, approval_request.latest_comment),
    latest_reviewer_id = COALESCE(latest_review.reviewer_id, approval_request.assigned_reviewer_id),
    revision_number = COALESCE(approval_request.revision_count, 0),
    approval_completed_at = CASE
        WHEN approval_request.current_status IN ('APPROVED', 'REJECTED') THEN approval_request.reviewed_at
        ELSE NULL
    END,
    approval_status = CASE approval_request.current_status
        WHEN 'SUBMITTED' THEN CASE
            WHEN COALESCE(approval_request.revision_count, 0) > 0 THEN 'RESUBMITTED'
            ELSE 'SUBMITTED'
        END
        WHEN 'IN_REVIEW' THEN 'IN_REVIEW'
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'CHANGES_REQUESTED' THEN 'CHANGES_REQUESTED'
        WHEN 'CANCELLED' THEN 'NOT_SUBMITTED'
        ELSE generated_version.approval_status
    END
FROM (
    SELECT DISTINCT ON (generated_version_id)
           id,
           generated_version_id,
           assigned_reviewer_id,
           current_status,
           submitted_at,
           reviewed_at,
           latest_comment,
           revision_count,
           created_at
    FROM platform.approval_requests
    WHERE is_deleted = FALSE
    ORDER BY generated_version_id, created_at DESC
) approval_request
LEFT JOIN LATERAL (
    SELECT approval_review.reviewer_id
    FROM platform.approval_reviews approval_review
    WHERE approval_review.approval_request_id = approval_request.id
      AND approval_review.is_deleted = FALSE
    ORDER BY approval_review.reviewed_at DESC, approval_review.created_at DESC
    LIMIT 1
) latest_review ON TRUE
LEFT JOIN LATERAL (
    SELECT approval_comment.comment_text
    FROM platform.approval_comments approval_comment
    WHERE approval_comment.approval_request_id = approval_request.id
      AND approval_comment.is_deleted = FALSE
    ORDER BY approval_comment.created_at DESC
    LIMIT 1
) latest_comment ON TRUE
WHERE generated_version.id = approval_request.generated_version_id;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '17')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
