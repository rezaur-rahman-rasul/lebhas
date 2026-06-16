ALTER TABLE platform.auth_otp_challenges
    ALTER COLUMN mobile_number DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS challenge_type VARCHAR(20) NOT NULL DEFAULT 'MOBILE',
    ADD COLUMN IF NOT EXISTS email VARCHAR(160);

CREATE INDEX IF NOT EXISTS idx_auth_otp_challenges_email
    ON platform.auth_otp_challenges (email, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.registration_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES platform.users (id),
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    current_step VARCHAR(40) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    pending_email VARCHAR(160),
    new_user BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_registration_sessions_user
    ON platform.registration_sessions (user_id, created_at DESC)
    WHERE is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.registration_sessions_email_otp', '66')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
