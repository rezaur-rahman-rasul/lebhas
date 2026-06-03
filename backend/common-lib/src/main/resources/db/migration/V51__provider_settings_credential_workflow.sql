ALTER TABLE platform.ai_tool_providers
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS category VARCHAR(80),
    ADD COLUMN IF NOT EXISTS supports_sandbox BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS supports_live BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS default_environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';

ALTER TABLE platform.ai_provider_credentials
    ADD COLUMN IF NOT EXISTS environment VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
    ADD COLUMN IF NOT EXISTS credential_status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONFIGURED',
    ADD COLUMN IF NOT EXISTS webhook_url TEXT,
    ADD COLUMN IF NOT EXISTS last_test_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS last_tested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_test_message TEXT;

UPDATE platform.ai_provider_credentials
SET credential_status = CASE
    WHEN encrypted_secret IS NULL OR encrypted_secret = '' THEN 'NOT_CONFIGURED'
    ELSE 'CONFIGURED'
END
WHERE credential_status = 'NOT_CONFIGURED';

CREATE INDEX IF NOT EXISTS idx_ai_provider_credentials_provider_environment
    ON platform.ai_provider_credentials (provider_id, environment)
    WHERE is_deleted = FALSE;

INSERT INTO platform.ai_tool_providers (
    id, provider_code, provider_name, description, category, provider_type, status, enabled,
    supports_sandbox, supports_live, default_environment, supported_layers,
    credential_config_key, fallback_eligible, workspace_routing_eligible, plan_routing_eligible,
    cost_metadata, quality_metadata, rate_limit_metadata
)
VALUES
    (gen_random_uuid(), 'OPENAI', 'OpenAI', 'OpenAI text and image generation provider', 'SEEDED', 'AI', 'ACTIVE', TRUE, TRUE, TRUE, 'SANDBOX', '["TEXT","IMAGE","MULTIMODAL"]'::jsonb, 'OPENAI_API_KEY', TRUE, TRUE, TRUE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'ANTHROPIC', 'Anthropic', 'Anthropic text generation provider', 'SEEDED', 'AI', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '["TEXT"]'::jsonb, 'ANTHROPIC_API_KEY', TRUE, TRUE, TRUE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'GEMINI', 'Gemini', 'Google Gemini multimodal generation provider', 'SEEDED', 'AI', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '["TEXT","IMAGE","MULTIMODAL"]'::jsonb, 'GEMINI_API_KEY', TRUE, TRUE, TRUE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb),
    (gen_random_uuid(), 'STABILITY', 'Stability', 'Stability image generation provider', 'SEEDED', 'AI', 'INACTIVE', FALSE, TRUE, TRUE, 'SANDBOX', '["IMAGE"]'::jsonb, 'STABILITY_API_KEY', TRUE, TRUE, TRUE, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.provider_settings_credential_workflow', '51')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
