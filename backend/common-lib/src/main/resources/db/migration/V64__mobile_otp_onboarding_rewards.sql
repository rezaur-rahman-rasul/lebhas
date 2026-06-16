ALTER TABLE platform.users
    ADD COLUMN IF NOT EXISTS mobile_verified_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_phone_not_null
    ON platform.users (phone)
    WHERE phone IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_verified_mobile_not_null
    ON platform.users (phone)
    WHERE phone IS NOT NULL AND mobile_verified_at IS NOT NULL AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.auth_otp_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    otp_token_hash VARCHAR(128) NOT NULL UNIQUE,
    mobile_number VARCHAR(30) NOT NULL,
    user_id UUID NOT NULL REFERENCES platform.users (id),
    otp_hash VARCHAR(120) NOT NULL,
    is_new_user BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMPTZ NOT NULL,
    resend_available_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_auth_otp_challenges_mobile
    ON platform.auth_otp_challenges (mobile_number, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.onboarding_reward_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    signup_free_credits NUMERIC(18,4) NOT NULL DEFAULT 15,
    email_reward_credits NUMERIC(18,4) NOT NULL DEFAULT 5,
    facebook_reward_credits NUMERIC(18,4) NOT NULL DEFAULT 5,
    instagram_reward_credits NUMERIC(18,4) NOT NULL DEFAULT 5,
    enable_profile_rewards BOOLEAN NOT NULL DEFAULT TRUE,
    enable_mobile_otp_login BOOLEAN NOT NULL DEFAULT TRUE,
    otp_expiry_minutes INT NOT NULL DEFAULT 5,
    otp_resend_cooldown_seconds INT NOT NULL DEFAULT 60,
    max_otp_attempts INT NOT NULL DEFAULT 5,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_onboarding_reward_policy_values CHECK (
        signup_free_credits >= 0
        AND email_reward_credits >= 0
        AND facebook_reward_credits >= 0
        AND instagram_reward_credits >= 0
        AND otp_expiry_minutes > 0
        AND otp_resend_cooldown_seconds > 0
        AND max_otp_attempts > 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_onboarding_reward_policy_one_active
    ON platform.onboarding_reward_policies (active)
    WHERE active = TRUE AND is_deleted = FALSE;

INSERT INTO platform.onboarding_reward_policies (
    active,
    signup_free_credits,
    email_reward_credits,
    facebook_reward_credits,
    instagram_reward_credits,
    enable_profile_rewards,
    enable_mobile_otp_login,
    otp_expiry_minutes,
    otp_resend_cooldown_seconds,
    max_otp_attempts
)
SELECT TRUE, 15.0000, 5.0000, 5.0000, 5.0000, TRUE, TRUE, 5, 60, 5
WHERE NOT EXISTS (
    SELECT 1 FROM platform.onboarding_reward_policies WHERE is_deleted = FALSE
);

CREATE TABLE IF NOT EXISTS platform.profile_social_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES platform.users (id),
    provider VARCHAR(30) NOT NULL,
    profile_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_profile_social_connection_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE IF NOT EXISTS platform.profile_reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    user_id UUID NOT NULL REFERENCES platform.users (id),
    reward_type VARCHAR(30) NOT NULL,
    credits_amount NUMERIC(18,4) NOT NULL,
    ledger_entry_id UUID REFERENCES platform.credit_ledger (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_profile_reward_claim_workspace_user_type UNIQUE (workspace_id, user_id, reward_type)
);

ALTER TABLE platform.credit_ledger DROP CONSTRAINT IF EXISTS chk_credit_ledger_transaction_type;
ALTER TABLE platform.credit_ledger
    ADD CONSTRAINT chk_credit_ledger_transaction_type CHECK (transaction_type IN (
        'PURCHASE',
        'CREDIT_PURCHASE',
        'FREE_SIGNUP_CREDIT_GRANTED',
        'FREE_SIGNUP_GRANT',
        'PROFILE_REWARD',
        'RESERVE',
        'FINALIZE',
        'REFUND',
        'MANUAL_ADJUSTMENT',
        'SYSTEM_ADJUSTMENT',
        'EXPIRY'
    ));

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.mobile_otp_onboarding', '64')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
