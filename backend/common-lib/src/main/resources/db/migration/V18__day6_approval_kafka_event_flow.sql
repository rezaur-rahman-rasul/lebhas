ALTER TABLE platform.approval_audit_logs
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_audit_logs_event_id
    ON platform.approval_audit_logs (event_id)
    WHERE event_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.approval_notification_records (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    event_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    approval_request_id UUID NOT NULL REFERENCES platform.approval_requests (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES platform.users (id),
    recipient_user_id UUID REFERENCES platform.users (id) ON DELETE SET NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(2000),
    internal_only BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_notification_records_event_id
    ON platform.approval_notification_records (event_id);

CREATE INDEX IF NOT EXISTS idx_approval_notification_records_workspace_id
    ON platform.approval_notification_records (workspace_id);

CREATE INDEX IF NOT EXISTS idx_approval_notification_records_recipient_user_id
    ON platform.approval_notification_records (recipient_user_id);

CREATE INDEX IF NOT EXISTS idx_approval_notification_records_approval_request_id
    ON platform.approval_notification_records (approval_request_id);

CREATE INDEX IF NOT EXISTS idx_approval_notification_records_occurred_at
    ON platform.approval_notification_records (occurred_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '18')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
