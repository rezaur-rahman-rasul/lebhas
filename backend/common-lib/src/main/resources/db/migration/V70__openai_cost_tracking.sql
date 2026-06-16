ALTER TABLE platform.ai_tool_providers
    ADD COLUMN IF NOT EXISTS openai_admin_api_key_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS provider_top_up_amount_usd NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS provider_top_up_date DATE,
    ADD COLUMN IF NOT EXISTS provider_manual_balance_usd NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS last_cost_sync_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS total_cost_spent_usd NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS estimated_remaining_balance_usd NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS cost_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.openai_cost_tracking', '70')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
