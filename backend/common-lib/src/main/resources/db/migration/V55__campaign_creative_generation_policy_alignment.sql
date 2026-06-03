UPDATE platform.plan_feature_policies policy
SET creative_generation_enabled = TRUE,
    asset_upload_enabled = TRUE,
    enabled_creative_tool_codes = CASE
        WHEN policy.enabled_creative_tool_codes ? 'CAMPAIGN_CREATIVE_GENERATOR'
            THEN policy.enabled_creative_tool_codes
        ELSE policy.enabled_creative_tool_codes || '["CAMPAIGN_CREATIVE_GENERATOR"]'::jsonb
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
SELECT
    gen_random_uuid(),
    tool.id,
    'CAMPAIGN_CREATIVE_BASIC_DEFAULT',
    0.0000,
    TRUE,
    NOW(),
    '{"systemDefault":true,"qualityMode":"BASIC"}'::jsonb,
    'system-seed',
    'system-seed'
FROM tool
ON CONFLICT DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.campaign_creative_generation_policy_alignment', '55')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
