WITH active_or_default_plan AS (
    SELECT
        workspace.id AS workspace_id,
        COALESCE(active_subscription.pricing_plan_id, default_plan.id) AS pricing_plan_id
    FROM platform.workspaces workspace
    LEFT JOIN LATERAL (
        SELECT subscription.pricing_plan_id
        FROM platform.workspace_subscriptions subscription
        WHERE subscription.workspace_id = workspace.id
          AND subscription.status = 'ACTIVE'
          AND subscription.is_deleted = FALSE
        ORDER BY subscription.updated_at DESC
        LIMIT 1
    ) active_subscription ON TRUE
    LEFT JOIN LATERAL (
        SELECT plan.id
        FROM platform.pricing_plans plan
        WHERE plan.is_default = TRUE
          AND plan.is_active = TRUE
          AND plan.is_deleted = FALSE
        ORDER BY plan.sort_order ASC, plan.created_at ASC
        LIMIT 1
    ) default_plan ON TRUE
    WHERE workspace.is_deleted = FALSE
),
starter_credit AS (
    SELECT
        plan.workspace_id,
        COALESCE(policy.monthly_credit_limit, 25.0000)::NUMERIC(19,4) AS credit_amount
    FROM active_or_default_plan plan
    LEFT JOIN platform.plan_feature_policies policy
      ON policy.pricing_plan_id = plan.pricing_plan_id
     AND policy.is_deleted = FALSE
)
UPDATE platform.credit_wallets wallet
SET balance = starter_credit.credit_amount,
    updated_at = NOW(),
    updated_by = 'system-seed'
FROM starter_credit
WHERE wallet.workspace_id = starter_credit.workspace_id
  AND wallet.is_deleted = FALSE
  AND wallet.balance = 0
  AND wallet.reserved_balance = 0
  AND starter_credit.credit_amount > 0;

WITH tool AS (
    SELECT id
    FROM platform.creative_tools
    WHERE tool_code = 'CAMPAIGN_CREATIVE_GENERATOR'
      AND is_deleted = FALSE
    LIMIT 1
)
UPDATE platform.tool_credit_cost_policies policy
SET credit_cost = 8.0000,
    enabled = TRUE,
    updated_at = NOW(),
    updated_by = 'system-seed',
    metadata = COALESCE(policy.metadata, '{}'::jsonb)
        || '{"systemDefault":true,"qualityMode":"BASIC","source":"campaign_creative_credit_wallet_alignment"}'::jsonb
FROM tool
WHERE policy.tool_id = tool.id
  AND policy.policy_code = 'CAMPAIGN_CREATIVE_BASIC_DEFAULT'
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
    8.0000,
    TRUE,
    NOW(),
    '{"systemDefault":true,"qualityMode":"BASIC","source":"campaign_creative_credit_wallet_alignment"}'::jsonb,
    'system-seed',
    'system-seed'
FROM tool
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.tool_credit_cost_policies policy
    WHERE policy.tool_id = tool.id
      AND policy.policy_code = 'CAMPAIGN_CREATIVE_BASIC_DEFAULT'
      AND policy.is_deleted = FALSE
);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.campaign_creative_credit_wallet_alignment', '56')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
