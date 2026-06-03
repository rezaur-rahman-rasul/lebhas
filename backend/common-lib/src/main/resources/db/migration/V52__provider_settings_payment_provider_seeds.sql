INSERT INTO platform.ai_tool_providers (
    id, provider_code, provider_name, description, category, provider_type, status, enabled,
    supports_sandbox, supports_live, default_environment, supported_layers,
    credential_config_key, fallback_eligible, workspace_routing_eligible, plan_routing_eligible,
    cost_metadata, quality_metadata, rate_limit_metadata
)
VALUES
    (gen_random_uuid(), 'SSLCOMMERZ', 'SSLCOMMERZ', 'SSLCOMMERZ payment gateway provider', 'SEEDED', 'PAYMENT', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '[]'::jsonb, 'SSLCOMMERZ_SECRET', FALSE, FALSE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'BKASH', 'bKash', 'bKash payment gateway provider', 'SEEDED', 'PAYMENT', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '[]'::jsonb, 'BKASH_SECRET', FALSE, FALSE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'NAGAD', 'Nagad', 'Nagad payment gateway provider', 'SEEDED', 'PAYMENT', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '[]'::jsonb, 'NAGAD_SECRET', FALSE, FALSE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'STRIPE', 'Stripe', 'Stripe payment gateway provider', 'SEEDED', 'PAYMENT', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '[]'::jsonb, 'STRIPE_SECRET', FALSE, FALSE, FALSE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.provider_settings_payment_provider_seeds', '52')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
