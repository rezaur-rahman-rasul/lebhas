WITH campaign_tool AS (
    SELECT id
    FROM platform.creative_tools
    WHERE tool_code = 'CAMPAIGN_CREATIVE_GENERATOR'
      AND is_deleted = FALSE
    LIMIT 1
),
openai_provider AS (
    SELECT id
    FROM platform.ai_tool_providers
    WHERE provider_code = 'OPENAI'
      AND is_deleted = FALSE
    LIMIT 1
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
SELECT
    gen_random_uuid(),
    seed.policy_code,
    campaign_tool.id,
    seed.quality_mode,
    openai_provider.id,
    NULL,
    NULL,
    NULL,
    1,
    TRUE,
    3,
    seed.metadata,
    'system-seed',
    'system-seed'
FROM campaign_tool
CROSS JOIN openai_provider
CROSS JOIN (
    VALUES
        (
            'CAMPAIGN_CREATIVE_BASIC_OPENAI',
            'BASIC',
            '{"systemDefault":true,"toolCode":"CAMPAIGN_CREATIVE_GENERATOR","providerCode":"OPENAI","qualityMode":"BASIC"}'::jsonb
        ),
        (
            'CAMPAIGN_CREATIVE_PREMIUM_OPENAI',
            'PREMIUM',
            '{"systemDefault":true,"toolCode":"CAMPAIGN_CREATIVE_GENERATOR","providerCode":"OPENAI","qualityMode":"PREMIUM"}'::jsonb
        )
) AS seed(policy_code, quality_mode, metadata)
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.provider_routing_policies existing
    WHERE existing.policy_code = seed.policy_code
      AND existing.is_deleted = FALSE
);

UPDATE platform.provider_routing_policies policy
SET enabled = TRUE,
    provider_id = openai_provider.id,
    priority_order = 1,
    circuit_failure_threshold = 3,
    updated_at = NOW(),
    updated_by = 'system-seed'
FROM (
    SELECT id
    FROM platform.ai_tool_providers
    WHERE provider_code = 'OPENAI'
      AND is_deleted = FALSE
    LIMIT 1
) openai_provider
WHERE policy.policy_code IN ('CAMPAIGN_CREATIVE_BASIC_OPENAI', 'CAMPAIGN_CREATIVE_PREMIUM_OPENAI')
  AND policy.is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.campaign_creative_provider_routing_alignment', '57')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
