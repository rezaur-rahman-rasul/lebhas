ALTER TABLE platform.users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE platform.users
    ADD COLUMN IF NOT EXISTS last_failed_login_at TIMESTAMPTZ;

ALTER TABLE platform.users
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_locked_until
    ON platform.users (locked_until)
    WHERE locked_until IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.access_token_revocations (
    id UUID PRIMARY KEY,
    token_id VARCHAR(80) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_access_token_revocations_token_id
    ON platform.access_token_revocations (token_id);

CREATE INDEX IF NOT EXISTS idx_access_token_revocations_expires_at
    ON platform.access_token_revocations (expires_at);

CREATE INDEX IF NOT EXISTS idx_assets_workspace_created_at_id
    ON platform.assets (workspace_id, created_at DESC, id);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '5')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
