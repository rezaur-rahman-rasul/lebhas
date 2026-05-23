DO $$
BEGIN
    IF to_regclass('platform.approval_requests') IS NOT NULL
       AND to_regclass('platform.legacy_approval_requests') IS NULL THEN
        ALTER TABLE platform.approval_requests RENAME TO legacy_approval_requests;
    END IF;

    IF to_regclass('platform.approval_reviews') IS NOT NULL
       AND to_regclass('platform.legacy_approval_reviews') IS NULL THEN
        ALTER TABLE platform.approval_reviews RENAME TO legacy_approval_reviews;
    END IF;

    IF to_regclass('platform.approval_comments') IS NOT NULL
       AND to_regclass('platform.legacy_approval_comments') IS NULL THEN
        ALTER TABLE platform.approval_comments RENAME TO legacy_approval_comments;
    END IF;

    IF to_regclass('platform.approval_assignments') IS NOT NULL
       AND to_regclass('platform.legacy_approval_assignments') IS NULL THEN
        ALTER TABLE platform.approval_assignments RENAME TO legacy_approval_assignments;
    END IF;

    IF to_regclass('platform.approval_audit_logs') IS NOT NULL
       AND to_regclass('platform.legacy_approval_audit_logs') IS NULL THEN
        ALTER TABLE platform.approval_audit_logs RENAME TO legacy_approval_audit_logs;
    END IF;

    IF to_regclass('platform.creative_approvals') IS NOT NULL
       AND to_regclass('platform.legacy_creative_approvals') IS NULL THEN
        ALTER TABLE platform.creative_approvals RENAME TO legacy_creative_approvals;
    END IF;

    IF to_regclass('platform.creative_review_comments') IS NOT NULL
       AND to_regclass('platform.legacy_creative_review_comments') IS NULL THEN
        ALTER TABLE platform.creative_review_comments RENAME TO legacy_creative_review_comments;
    END IF;

    IF to_regclass('platform.creative_approval_history') IS NOT NULL
       AND to_regclass('platform.legacy_creative_approval_history') IS NULL THEN
        ALTER TABLE platform.creative_approval_history RENAME TO legacy_creative_approval_history;
    END IF;

    IF to_regclass('platform.public_share_links') IS NOT NULL
       AND to_regclass('platform.legacy_public_share_links') IS NULL THEN
        ALTER TABLE platform.public_share_links RENAME TO legacy_public_share_links;
    END IF;
END $$;

COMMENT ON TABLE platform.approval_workflows IS
    'Revised Day 6 approval workflow table. Legacy approval request tables are isolated with legacy_ prefixes.';

COMMENT ON TABLE platform.approval_history IS
    'Revised Day 6 approval action history table.';

COMMENT ON TABLE platform.share_links IS
    'Revised Day 6 generated-version share link table. Legacy public share links are isolated as legacy_public_share_links.';

DO $$
BEGIN
    IF to_regclass('platform.legacy_approval_requests') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_approval_requests IS
            'Legacy approval request table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_approval_reviews') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_approval_reviews IS
            'Legacy approval review table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_approval_comments') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_approval_comments IS
            'Legacy approval comment table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_approval_assignments') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_approval_assignments IS
            'Legacy approval assignment table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_approval_audit_logs') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_approval_audit_logs IS
            'Legacy approval audit table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_creative_approvals') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_creative_approvals IS
            'Legacy creative approval table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_creative_review_comments') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_creative_review_comments IS
            'Legacy creative review comment table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_creative_approval_history') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_creative_approval_history IS
            'Legacy creative approval history table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;

    IF to_regclass('platform.legacy_public_share_links') IS NOT NULL THEN
        COMMENT ON TABLE platform.legacy_public_share_links IS
            'Legacy public share link table retained for transitional service compatibility. Merge/delete in cleanup Step 2.';
    END IF;
END $$;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.cleanup.legacy_approval_share_isolated', '30')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
