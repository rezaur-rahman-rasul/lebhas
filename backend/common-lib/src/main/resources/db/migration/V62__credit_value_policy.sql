CREATE TABLE IF NOT EXISTS platform.credit_value_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    credit_usd_value NUMERIC(18,6) NOT NULL,
    average_provider_cost_per_creative_usd NUMERIC(18,6) NOT NULL,
    provider_cost_multiplier NUMERIC(12,4) NOT NULL,
    free_signup_credit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    free_signup_mode VARCHAR(50) NOT NULL DEFAULT 'FIXED_CREDITS',
    free_signup_credits NUMERIC(18,4) NOT NULL DEFAULT 25,
    free_signup_usd_value NUMERIC(18,6) NOT NULL DEFAULT 0,
    free_signup_percentage NUMERIC(8,4) NOT NULL DEFAULT 2,
    one_time_per_workspace BOOLEAN NOT NULL DEFAULT TRUE,
    minimum_wallet_balance_warning NUMERIC(18,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_credit_value_policy_values CHECK (
        credit_usd_value > 0
        AND average_provider_cost_per_creative_usd >= 0
        AND provider_cost_multiplier >= 1
        AND free_signup_credits >= 0
        AND free_signup_usd_value >= 0
        AND free_signup_percentage >= 0
        AND free_signup_percentage <= 100
        AND minimum_wallet_balance_warning >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_credit_value_policy_one_active
    ON platform.credit_value_policies (active)
    WHERE active = TRUE AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_credit_value_policy_active
    ON platform.credit_value_policies (active)
    WHERE is_deleted = FALSE;

INSERT INTO platform.credit_value_policies (
    currency,
    credit_usd_value,
    average_provider_cost_per_creative_usd,
    provider_cost_multiplier,
    free_signup_credit_enabled,
    free_signup_mode,
    free_signup_credits,
    free_signup_usd_value,
    free_signup_percentage,
    one_time_per_workspace,
    minimum_wallet_balance_warning,
    active,
    effective_from
)
SELECT 'USD', 0.050000, 0.150000, 5.0000, TRUE, 'FIXED_CREDITS', 25.0000, 0.000000, 2.0000, TRUE, 5.0000, TRUE, NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.credit_value_policies
    WHERE is_deleted = FALSE
);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.credit_value_policy', '62')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
