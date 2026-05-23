INSERT INTO platform.pricing_plans (
    id,
    name,
    code,
    description,
    monthly_price,
    yearly_price,
    currency,
    is_default,
    is_active,
    sort_order,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'Free',
        'FREE',
        'Editable example seed plan for starter workspaces.',
        0.0000,
        0.0000,
        'USD',
        TRUE,
        TRUE,
        10,
        NOW(),
        NOW(),
        'system-seed',
        'system-seed',
        FALSE
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'Basic',
        'BASIC',
        'Editable example seed plan for small teams.',
        19.0000,
        190.0000,
        'USD',
        FALSE,
        TRUE,
        20,
        NOW(),
        NOW(),
        'system-seed',
        'system-seed',
        FALSE
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'Pro',
        'PRO',
        'Editable example seed plan for growing teams.',
        49.0000,
        490.0000,
        'USD',
        FALSE,
        TRUE,
        30,
        NOW(),
        NOW(),
        'system-seed',
        'system-seed',
        FALSE
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        'Enterprise',
        'ENTERPRISE',
        'Editable example seed plan for larger organizations.',
        149.0000,
        1490.0000,
        'USD',
        FALSE,
        TRUE,
        40,
        NOW(),
        NOW(),
        'system-seed',
        'system-seed',
        FALSE
    )
ON CONFLICT DO NOTHING;

INSERT INTO platform.plan_feature_policies (
    id,
    pricing_plan_id,
    max_generated_versions_per_request,
    max_brands,
    max_product_services,
    max_projects,
    max_team_members,
    max_storage_gb,
    monthly_credit_limit,
    allow_approval_workflow,
    allow_public_share_links,
    allow_video_generation,
    allow_advanced_prompt_intelligence,
    allow_team_collaboration,
    allow_export_without_watermark,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    seed.id,
    seed.pricing_plan_id,
    seed.max_generated_versions_per_request,
    seed.max_brands,
    seed.max_product_services,
    seed.max_projects,
    seed.max_team_members,
    seed.max_storage_gb,
    seed.monthly_credit_limit,
    seed.allow_approval_workflow,
    seed.allow_public_share_links,
    seed.allow_video_generation,
    seed.allow_advanced_prompt_intelligence,
    seed.allow_team_collaboration,
    seed.allow_export_without_watermark,
    NOW(),
    NOW(),
    'system-seed',
    'system-seed',
    FALSE
FROM (
    VALUES
        (
            'aaaaaaaa-1111-1111-1111-111111111111'::UUID,
            '11111111-1111-1111-1111-111111111111'::UUID,
            4,
            1,
            2,
            2,
            2,
            5.0000::NUMERIC(19,4),
            25.0000::NUMERIC(19,4),
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE,
            FALSE
        ),
        (
            'aaaaaaaa-2222-2222-2222-222222222222'::UUID,
            '22222222-2222-2222-2222-222222222222'::UUID,
            8,
            3,
            10,
            8,
            5,
            25.0000::NUMERIC(19,4),
            150.0000::NUMERIC(19,4),
            TRUE,
            TRUE,
            FALSE,
            FALSE,
            TRUE,
            FALSE
        ),
        (
            'aaaaaaaa-3333-3333-3333-333333333333'::UUID,
            '33333333-3333-3333-3333-333333333333'::UUID,
            16,
            10,
            30,
            20,
            15,
            100.0000::NUMERIC(19,4),
            750.0000::NUMERIC(19,4),
            TRUE,
            TRUE,
            TRUE,
            TRUE,
            TRUE,
            TRUE
        ),
        (
            'aaaaaaaa-4444-4444-4444-444444444444'::UUID,
            '44444444-4444-4444-4444-444444444444'::UUID,
            64,
            100,
            250,
            150,
            100,
            1024.0000::NUMERIC(19,4),
            5000.0000::NUMERIC(19,4),
            TRUE,
            TRUE,
            TRUE,
            TRUE,
            TRUE,
            TRUE
        )
) AS seed(
    id,
    pricing_plan_id,
    max_generated_versions_per_request,
    max_brands,
    max_product_services,
    max_projects,
    max_team_members,
    max_storage_gb,
    monthly_credit_limit,
    allow_approval_workflow,
    allow_public_share_links,
    allow_video_generation,
    allow_advanced_prompt_intelligence,
    allow_team_collaboration,
    allow_export_without_watermark
)
WHERE EXISTS (
    SELECT 1
    FROM platform.pricing_plans pricing_plan
    WHERE pricing_plan.id = seed.pricing_plan_id
)
  AND NOT EXISTS (
    SELECT 1
    FROM platform.plan_feature_policies policy
    WHERE policy.pricing_plan_id = seed.pricing_plan_id
      AND policy.is_deleted = FALSE
);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '22')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
