ALTER TABLE platform.onboarding_reward_policies
    ADD COLUMN IF NOT EXISTS enable_signup_free_credits BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS enable_email_reward BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS enable_facebook_reward BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS enable_instagram_reward BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS reward_only_once BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE platform.onboarding_reward_policies
SET enable_profile_rewards = enable_email_reward OR enable_facebook_reward OR enable_instagram_reward
WHERE is_deleted = FALSE;

ALTER TABLE platform.credit_ledger DROP CONSTRAINT IF EXISTS chk_credit_ledger_transaction_type;
ALTER TABLE platform.credit_ledger
    ADD CONSTRAINT chk_credit_ledger_transaction_type CHECK (transaction_type IN (
        'PURCHASE',
        'CREDIT_PURCHASE',
        'FREE_SIGNUP_CREDIT_GRANTED',
        'FREE_SIGNUP_GRANT',
        'PROFILE_REWARD',
        'PROFILE_REWARD_EMAIL',
        'PROFILE_REWARD_FACEBOOK',
        'PROFILE_REWARD_INSTAGRAM',
        'RESERVE',
        'FINALIZE',
        'REFUND',
        'MANUAL_ADJUSTMENT',
        'SYSTEM_ADJUSTMENT',
        'EXPIRY'
    ));

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.onboarding_reward_policy_flags', '65')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
