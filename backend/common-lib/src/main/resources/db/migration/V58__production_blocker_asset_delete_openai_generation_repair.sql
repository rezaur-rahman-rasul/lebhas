INSERT INTO platform.permissions (code, description)
VALUES
    ('ASSET_DELETE', 'Delete workspace assets'),
    ('CREATIVE_GENERATE', 'Generate creatives'),
    ('CREATIVE_DOWNLOAD', 'Download generated creatives')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'ASSET_DELETE'),
    ('MASTER', 'CREATIVE_GENERATE'),
    ('MASTER', 'CREATIVE_DOWNLOAD'),
    ('ADMIN', 'ASSET_DELETE'),
    ('ADMIN', 'CREATIVE_GENERATE'),
    ('ADMIN', 'CREATIVE_DOWNLOAD')
ON CONFLICT (role_code, permission_code) DO NOTHING;

UPDATE platform.plan_feature_policies policy
SET creative_generation_enabled = TRUE,
    asset_upload_enabled = TRUE,
    max_generated_versions_per_request = GREATEST(COALESCE(policy.max_generated_versions_per_request, 1), 1),
    enabled_creative_tool_codes = CASE
        WHEN COALESCE(policy.enabled_creative_tool_codes, '[]'::jsonb) ? 'CAMPAIGN_CREATIVE_GENERATOR'
            THEN COALESCE(policy.enabled_creative_tool_codes, '[]'::jsonb)
        ELSE COALESCE(policy.enabled_creative_tool_codes, '[]'::jsonb) || '["CAMPAIGN_CREATIVE_GENERATOR"]'::jsonb
    END,
    updated_at = NOW(),
    updated_by = 'system-seed'
FROM platform.pricing_plans plan
WHERE policy.pricing_plan_id = plan.id
  AND plan.is_active = TRUE
  AND plan.is_deleted = FALSE
  AND policy.is_deleted = FALSE;

WITH tool AS (
    SELECT id
    FROM platform.creative_tools
    WHERE tool_code = 'CAMPAIGN_CREATIVE_GENERATOR'
      AND is_deleted = FALSE
    LIMIT 1
), seed AS (
    SELECT 'CAMPAIGN_CREATIVE_BASIC_DEFAULT'::VARCHAR AS policy_code,
           8.0000::NUMERIC(19,4) AS credit_cost,
           '{"systemDefault":true,"qualityMode":"BASIC","source":"v58_production_blocker_repair"}'::jsonb AS metadata
    UNION ALL
    SELECT 'CAMPAIGN_CREATIVE_PREMIUM_DEFAULT'::VARCHAR,
           18.0000::NUMERIC(19,4),
           '{"systemDefault":true,"qualityMode":"PREMIUM","source":"v58_production_blocker_repair"}'::jsonb
)
INSERT INTO platform.tool_credit_cost_policies (
    id,
    tool_id,
    policy_code,
    credit_cost,
    enabled,
    effective_from,
    metadata,
    created_by,
    updated_by
)
SELECT gen_random_uuid(), tool.id, seed.policy_code, seed.credit_cost, TRUE, NOW(), seed.metadata, 'system-seed', 'system-seed'
FROM tool
CROSS JOIN seed
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.tool_credit_cost_policies existing
    WHERE existing.tool_id = tool.id
      AND existing.policy_code = seed.policy_code
      AND existing.is_deleted = FALSE
);

WITH tool AS (
    SELECT id
    FROM platform.creative_tools
    WHERE tool_code = 'CAMPAIGN_CREATIVE_GENERATOR'
      AND is_deleted = FALSE
    LIMIT 1
)
UPDATE platform.tool_credit_cost_policies policy
SET enabled = TRUE,
    credit_cost = CASE
        WHEN policy.policy_code = 'CAMPAIGN_CREATIVE_BASIC_DEFAULT' THEN 8.0000
        WHEN policy.policy_code = 'CAMPAIGN_CREATIVE_PREMIUM_DEFAULT' THEN 18.0000
        ELSE policy.credit_cost
    END,
    updated_at = NOW(),
    updated_by = 'system-seed'
FROM tool
WHERE policy.tool_id = tool.id
  AND policy.policy_code IN ('CAMPAIGN_CREATIVE_BASIC_DEFAULT', 'CAMPAIGN_CREATIVE_PREMIUM_DEFAULT')
  AND policy.is_deleted = FALSE;

WITH campaign_tool AS (
    SELECT id
    FROM platform.creative_tools
    WHERE tool_code = 'CAMPAIGN_CREATIVE_GENERATOR'
      AND is_deleted = FALSE
    LIMIT 1
), openai_provider AS (
    SELECT id
    FROM platform.ai_tool_providers
    WHERE provider_code = 'OPENAI'
      AND is_deleted = FALSE
    LIMIT 1
), seed AS (
    SELECT 'CAMPAIGN_CREATIVE_BASIC_OPENAI'::VARCHAR AS policy_code,
           'BASIC'::VARCHAR AS quality_mode,
           '{"systemDefault":true,"toolCode":"CAMPAIGN_CREATIVE_GENERATOR","providerCode":"OPENAI","qualityMode":"BASIC","source":"v58_production_blocker_repair"}'::jsonb AS metadata
    UNION ALL
    SELECT 'CAMPAIGN_CREATIVE_PREMIUM_OPENAI'::VARCHAR,
           'PREMIUM'::VARCHAR,
           '{"systemDefault":true,"toolCode":"CAMPAIGN_CREATIVE_GENERATOR","providerCode":"OPENAI","qualityMode":"PREMIUM","source":"v58_production_blocker_repair"}'::jsonb
)
INSERT INTO platform.provider_routing_policies (
    id,
    policy_code,
    tool_id,
    quality_mode,
    provider_id,
    model_id,
    fallback_provider_id,
    fallback_model_id,
    priority_order,
    enabled,
    circuit_failure_threshold,
    metadata,
    created_by,
    updated_by
)
SELECT gen_random_uuid(), seed.policy_code, campaign_tool.id, seed.quality_mode, openai_provider.id,
       NULL, NULL, NULL, 1, TRUE, 3, seed.metadata, 'system-seed', 'system-seed'
FROM campaign_tool
CROSS JOIN openai_provider
CROSS JOIN seed
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.provider_routing_policies existing
    WHERE existing.policy_code = seed.policy_code
      AND existing.is_deleted = FALSE
);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.production_blocker_asset_delete_openai_generation_repair', '58')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
