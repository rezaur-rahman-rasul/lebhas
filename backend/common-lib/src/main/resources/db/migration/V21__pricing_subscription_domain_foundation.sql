CREATE TABLE IF NOT EXISTS platform.pricing_plans (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(60) NOT NULL,
    description VARCHAR(1000),
    monthly_price NUMERIC(19,4) NOT NULL,
    yearly_price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.pricing_plans
    DROP CONSTRAINT IF EXISTS chk_pricing_plans_monthly_price;

ALTER TABLE platform.pricing_plans
    ADD CONSTRAINT chk_pricing_plans_monthly_price
        CHECK (monthly_price >= 0);

ALTER TABLE platform.pricing_plans
    DROP CONSTRAINT IF EXISTS chk_pricing_plans_yearly_price;

ALTER TABLE platform.pricing_plans
    ADD CONSTRAINT chk_pricing_plans_yearly_price
        CHECK (yearly_price >= 0);

ALTER TABLE platform.pricing_plans
    DROP CONSTRAINT IF EXISTS chk_pricing_plans_sort_order;

ALTER TABLE platform.pricing_plans
    ADD CONSTRAINT chk_pricing_plans_sort_order
        CHECK (sort_order >= 0);

ALTER TABLE platform.pricing_plans
    DROP CONSTRAINT IF EXISTS chk_pricing_plans_code_not_blank;

ALTER TABLE platform.pricing_plans
    ADD CONSTRAINT chk_pricing_plans_code_not_blank
        CHECK (LENGTH(BTRIM(code)) > 0);

ALTER TABLE platform.pricing_plans
    DROP CONSTRAINT IF EXISTS chk_pricing_plans_currency_length;

ALTER TABLE platform.pricing_plans
    ADD CONSTRAINT chk_pricing_plans_currency_length
        CHECK (CHAR_LENGTH(currency) = 3);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pricing_plans_code
    ON platform.pricing_plans (code)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_pricing_plans_is_active
    ON platform.pricing_plans (is_active);

CREATE INDEX IF NOT EXISTS idx_pricing_plans_sort_order
    ON platform.pricing_plans (sort_order, name);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pricing_plans_default
    ON platform.pricing_plans (is_default)
    WHERE is_default = TRUE
      AND is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.plan_feature_policies (
    id UUID PRIMARY KEY,
    pricing_plan_id UUID NOT NULL REFERENCES platform.pricing_plans (id) ON DELETE CASCADE,
    max_generated_versions_per_request INTEGER,
    max_brands INTEGER,
    max_product_services INTEGER,
    max_projects INTEGER,
    max_team_members INTEGER,
    max_storage_gb NUMERIC(19,4),
    monthly_credit_limit NUMERIC(19,4),
    allow_approval_workflow BOOLEAN NOT NULL DEFAULT FALSE,
    allow_public_share_links BOOLEAN NOT NULL DEFAULT FALSE,
    allow_video_generation BOOLEAN NOT NULL DEFAULT FALSE,
    allow_advanced_prompt_intelligence BOOLEAN NOT NULL DEFAULT FALSE,
    allow_team_collaboration BOOLEAN NOT NULL DEFAULT FALSE,
    allow_export_without_watermark BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_generated_versions_per_request;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_generated_versions_per_request
        CHECK (max_generated_versions_per_request IS NULL OR max_generated_versions_per_request >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_brands;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_brands
        CHECK (max_brands IS NULL OR max_brands >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_product_services;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_product_services
        CHECK (max_product_services IS NULL OR max_product_services >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_projects;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_projects
        CHECK (max_projects IS NULL OR max_projects >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_team_members;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_team_members
        CHECK (max_team_members IS NULL OR max_team_members >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_storage_gb;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_storage_gb
        CHECK (max_storage_gb IS NULL OR max_storage_gb >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_monthly_credit_limit;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_monthly_credit_limit
        CHECK (monthly_credit_limit IS NULL OR monthly_credit_limit >= 0);

CREATE UNIQUE INDEX IF NOT EXISTS uk_plan_feature_policies_pricing_plan_id
    ON platform.plan_feature_policies (pricing_plan_id)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS platform.workspace_subscriptions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    pricing_plan_id UUID NOT NULL REFERENCES platform.pricing_plans (id) ON DELETE RESTRICT,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    trial_ends_at TIMESTAMPTZ,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE platform.workspace_subscriptions
    DROP CONSTRAINT IF EXISTS chk_workspace_subscriptions_status;

ALTER TABLE platform.workspace_subscriptions
    ADD CONSTRAINT chk_workspace_subscriptions_status
        CHECK (status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED'));

ALTER TABLE platform.workspace_subscriptions
    DROP CONSTRAINT IF EXISTS chk_workspace_subscriptions_expires_at;

ALTER TABLE platform.workspace_subscriptions
    ADD CONSTRAINT chk_workspace_subscriptions_expires_at
        CHECK (expires_at IS NULL OR expires_at >= started_at);

ALTER TABLE platform.workspace_subscriptions
    DROP CONSTRAINT IF EXISTS chk_workspace_subscriptions_trial_ends_at;

ALTER TABLE platform.workspace_subscriptions
    ADD CONSTRAINT chk_workspace_subscriptions_trial_ends_at
        CHECK (trial_ends_at IS NULL OR trial_ends_at >= started_at);

CREATE INDEX IF NOT EXISTS idx_workspace_subscriptions_pricing_plan_id
    ON platform.workspace_subscriptions (pricing_plan_id);

CREATE INDEX IF NOT EXISTS idx_workspace_subscriptions_workspace_id
    ON platform.workspace_subscriptions (workspace_id);

CREATE INDEX IF NOT EXISTS idx_workspace_subscriptions_status
    ON platform.workspace_subscriptions (status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workspace_subscriptions_workspace_id
    ON platform.workspace_subscriptions (workspace_id)
    WHERE is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '21')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
