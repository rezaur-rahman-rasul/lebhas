CREATE TABLE IF NOT EXISTS platform.notifications (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    source_event_id VARCHAR(120) NOT NULL,
    recipient_user_id UUID NOT NULL REFERENCES platform.users (id),
    actor_user_id UUID NOT NULL REFERENCES platform.users (id),
    notification_type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id UUID NOT NULL,
    notification_status VARCHAR(40) NOT NULL DEFAULT 'UNREAD',
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_notification_type;

ALTER TABLE platform.notifications
    ADD CONSTRAINT chk_notifications_notification_type
        CHECK (notification_type IN (
            'APPROVAL_ASSIGNED',
            'APPROVAL_SUBMITTED',
            'APPROVAL_APPROVED',
            'APPROVAL_REJECTED',
            'APPROVAL_CHANGES_REQUESTED',
            'APPROVAL_RESUBMITTED'
        ));

ALTER TABLE platform.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_notification_status;

ALTER TABLE platform.notifications
    ADD CONSTRAINT chk_notifications_notification_status
        CHECK (notification_status IN ('UNREAD', 'READ'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_source_event_id
    ON platform.notifications (source_event_id);

CREATE INDEX IF NOT EXISTS idx_notifications_workspace_id
    ON platform.notifications (workspace_id);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_user_id
    ON platform.notifications (recipient_user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_reference
    ON platform.notifications (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_notifications_status_created_at
    ON platform.notifications (notification_status, created_at DESC);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'platform'
          AND table_name = 'approval_notification_records'
    ) THEN
        INSERT INTO platform.notifications (
            id,
            workspace_id,
            source_event_id,
            recipient_user_id,
            actor_user_id,
            notification_type,
            title,
            message,
            reference_type,
            reference_id,
            notification_status,
            read_at,
            created_at,
            updated_at,
            created_by,
            updated_by,
            is_deleted
        )
        SELECT
            id,
            workspace_id,
            event_id,
            recipient_user_id,
            actor_id,
            CASE
                WHEN event_type LIKE '%approval.assigned' THEN 'APPROVAL_ASSIGNED'
                WHEN event_type LIKE '%approval.request.submitted' THEN 'APPROVAL_SUBMITTED'
                WHEN event_type LIKE '%approval.approved' THEN 'APPROVAL_APPROVED'
                WHEN event_type LIKE '%approval.rejected' THEN 'APPROVAL_REJECTED'
                WHEN event_type LIKE '%approval.changes.requested' THEN 'APPROVAL_CHANGES_REQUESTED'
                WHEN event_type LIKE '%approval.resubmitted' THEN 'APPROVAL_RESUBMITTED'
                ELSE NULL
            END,
            title,
            COALESCE(message, title),
            'APPROVAL_REQUEST',
            approval_request_id,
            CASE
                WHEN read_at IS NULL THEN 'UNREAD'
                ELSE 'READ'
            END,
            read_at,
            created_at,
            updated_at,
            created_by,
            updated_by,
            is_deleted
        FROM platform.approval_notification_records
        WHERE event_type LIKE '%approval.assigned'
           OR event_type LIKE '%approval.request.submitted'
           OR event_type LIKE '%approval.approved'
           OR event_type LIKE '%approval.rejected'
           OR event_type LIKE '%approval.changes.requested'
           OR event_type LIKE '%approval.resubmitted'
        ON CONFLICT (source_event_id) DO NOTHING;
    END IF;
END $$;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '19')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
