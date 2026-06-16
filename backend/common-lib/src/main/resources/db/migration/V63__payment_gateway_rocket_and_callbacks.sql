ALTER TABLE platform.payment_providers
    DROP CONSTRAINT IF EXISTS chk_payment_providers_provider_type;

ALTER TABLE platform.payment_providers
    ADD CONSTRAINT chk_payment_providers_provider_type
        CHECK (provider_type IN ('SSLCOMMERZ', 'BKASH', 'NAGAD', 'ROCKET', 'STRIPE', 'MANUAL'));

ALTER TABLE platform.credit_ledger
    DROP CONSTRAINT IF EXISTS chk_credit_ledger_transaction_type;

ALTER TABLE platform.credit_ledger
    ADD CONSTRAINT chk_credit_ledger_transaction_type
        CHECK (transaction_type IN ('PURCHASE', 'CREDIT_PURCHASE', 'FREE_SIGNUP_CREDIT_GRANTED', 'RESERVE', 'FINALIZE', 'REFUND', 'MANUAL_ADJUSTMENT', 'SYSTEM_ADJUSTMENT', 'EXPIRY'));

INSERT INTO platform.payment_providers (
    id, name, code, provider_type, is_enabled, sandbox_enabled, live_enabled, priority, created_at, updated_at
)
VALUES
    (gen_random_uuid(), 'SSLCommerz', 'SSLCOMMERZ', 'SSLCOMMERZ', FALSE, TRUE, TRUE, 10, NOW(), NOW()),
    (gen_random_uuid(), 'bKash', 'BKASH', 'BKASH', FALSE, TRUE, TRUE, 20, NOW(), NOW()),
    (gen_random_uuid(), 'Nagad', 'NAGAD', 'NAGAD', FALSE, TRUE, TRUE, 30, NOW(), NOW()),
    (gen_random_uuid(), 'Rocket', 'ROCKET', 'ROCKET', FALSE, TRUE, TRUE, 40, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.ai_tool_providers (
    id, provider_code, provider_name, description, category, provider_type, status, enabled,
    supports_sandbox, supports_live, default_environment, supported_layers,
    credential_config_key, fallback_eligible, workspace_routing_eligible, plan_routing_eligible,
    cost_metadata, quality_metadata, rate_limit_metadata
)
VALUES
    (gen_random_uuid(), 'ROCKET', 'Rocket', 'Rocket payment gateway provider', 'SEEDED', 'PAYMENT', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '["PAYMENT_COLLECTION","WEBHOOK_CALLBACK"]'::jsonb, 'ROCKET_SECRET', FALSE, FALSE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.payment_gateway_rocket_and_callbacks', '63')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
