CREATE TABLE IF NOT EXISTS platform.provider_credit_pools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    currency VARCHAR(12) NOT NULL DEFAULT 'USD',
    provider_balance_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    internal_credit_equivalent NUMERIC(19,4) NOT NULL DEFAULT 0,
    reserved_internal_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    used_internal_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    low_balance_threshold NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_provider_credit_pool_non_negative CHECK (
        provider_balance_amount >= 0
        AND internal_credit_equivalent >= 0
        AND reserved_internal_credits >= 0
        AND used_internal_credits >= 0
        AND low_balance_threshold >= 0
        AND internal_credit_equivalent >= reserved_internal_credits + used_internal_credits
    )
);

CREATE INDEX IF NOT EXISTS idx_provider_credit_pools_provider
    ON platform.provider_credit_pools (provider_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_provider_credit_pools_low_balance
    ON platform.provider_credit_pools (provider_id, low_balance_threshold)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.provider_credit_exchange_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    internal_credit_per_provider_unit NUMERIC(19,4) NOT NULL DEFAULT 1,
    free_signup_credit_percentage NUMERIC(9,4) NOT NULL DEFAULT 2,
    free_signup_credit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_free_signup_credits NUMERIC(19,4) NOT NULL DEFAULT 2000,
    min_provider_balance_required NUMERIC(19,4) NOT NULL DEFAULT 0,
    fallback_free_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_provider_exchange_policy_non_negative CHECK (
        internal_credit_per_provider_unit > 0
        AND free_signup_credit_percentage >= 0
        AND max_free_signup_credits >= 0
        AND min_provider_balance_required >= 0
        AND fallback_free_credits >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_provider_exchange_policy_provider
    ON platform.provider_credit_exchange_policies (provider_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_provider_exchange_policy_active
    ON platform.provider_credit_exchange_policies (provider_id, active)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.provider_credit_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES platform.ai_tool_providers(id),
    transaction_type VARCHAR(60) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    balance_before NUMERIC(19,4) NOT NULL,
    balance_after NUMERIC(19,4) NOT NULL,
    reference_type VARCHAR(80),
    reference_id UUID,
    description VARCHAR(1000),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_provider_credit_ledger_provider_created
    ON platform.provider_credit_ledger (provider_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_provider_credit_ledger_reference
    ON platform.provider_credit_ledger (reference_type, reference_id);

INSERT INTO platform.provider_credit_exchange_policies (
    provider_id,
    internal_credit_per_provider_unit,
    free_signup_credit_percentage,
    free_signup_credit_enabled,
    max_free_signup_credits,
    min_provider_balance_required,
    fallback_free_credits,
    active
)
SELECT provider.id, 1.0000, 2.0000, TRUE, 2000.0000, 0.0000, 0.0000, TRUE
FROM platform.ai_tool_providers provider
WHERE provider.provider_type = 'AI'
  AND provider.is_deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM platform.provider_credit_exchange_policies existing
      WHERE existing.provider_id = provider.id
        AND existing.is_deleted = FALSE
  );

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.provider_credit_exchange_and_free_signup', '59')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
