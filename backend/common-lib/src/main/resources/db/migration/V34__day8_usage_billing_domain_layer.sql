ALTER TABLE platform.usage_billing_logs
    ALTER COLUMN generated_version_id DROP NOT NULL;

ALTER TABLE platform.usage_billing_logs
    ALTER COLUMN amount DROP NOT NULL;

ALTER TABLE platform.usage_billing_logs
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS reference_id UUID,
    ADD COLUMN IF NOT EXISTS credits_charged NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS estimated_cost_usd NUMERIC(19,6),
    ADD COLUMN IF NOT EXISTS pricing_plan_id UUID,
    ADD COLUMN IF NOT EXISTS plan_feature_policy_id UUID;

UPDATE platform.usage_billing_logs
SET reference_type = COALESCE(reference_type, 'GENERATED_VERSION'),
    reference_id = COALESCE(reference_id, generated_version_id),
    credits_charged = COALESCE(credits_charged, amount, 0)
WHERE reference_id IS NULL
   OR credits_charged = 0;

ALTER TABLE platform.usage_billing_logs
    ALTER COLUMN credits_charged DROP DEFAULT;

ALTER TABLE platform.usage_billing_logs
    ADD CONSTRAINT fk_usage_billing_logs_pricing_plan
        FOREIGN KEY (pricing_plan_id) REFERENCES platform.pricing_plans (id);

ALTER TABLE platform.usage_billing_logs
    ADD CONSTRAINT fk_usage_billing_logs_plan_feature_policy
        FOREIGN KEY (plan_feature_policy_id) REFERENCES platform.plan_feature_policies (id);

ALTER TABLE platform.usage_billing_logs
    ADD CONSTRAINT chk_usage_billing_logs_credits_charged_nonnegative
        CHECK (credits_charged >= 0);

ALTER TABLE platform.usage_billing_logs
    ADD CONSTRAINT chk_usage_billing_logs_estimated_cost_nonnegative
        CHECK (estimated_cost_usd IS NULL OR estimated_cost_usd >= 0);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_workspace_created_at
    ON platform.usage_billing_logs (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_usage_type_created_at
    ON platform.usage_billing_logs (usage_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_reference
    ON platform.usage_billing_logs (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_pricing_plan_id
    ON platform.usage_billing_logs (pricing_plan_id);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_plan_feature_policy_id
    ON platform.usage_billing_logs (plan_feature_policy_id);

CREATE TABLE IF NOT EXISTS platform.credit_ledger (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    creative_request_id UUID REFERENCES platform.creative_requests (id) ON DELETE SET NULL,
    generated_version_id UUID REFERENCES platform.generated_versions (id) ON DELETE SET NULL,
    generation_job_id UUID REFERENCES platform.generation_jobs (id) ON DELETE SET NULL,
    transaction_type VARCHAR(40) NOT NULL,
    credits_amount NUMERIC(19,4) NOT NULL,
    balance_before_transaction NUMERIC(19,4) NOT NULL,
    balance_after_transaction NUMERIC(19,4) NOT NULL,
    reference_type VARCHAR(80),
    reference_id UUID,
    description VARCHAR(1000),
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_credit_ledger_transaction_type
        CHECK (transaction_type IN ('RESERVE', 'FINALIZE', 'REFUND', 'MANUAL_ADJUSTMENT', 'SYSTEM_ADJUSTMENT', 'EXPIRY'))
);

CREATE INDEX IF NOT EXISTS idx_credit_ledger_workspace_created_at
    ON platform.credit_ledger (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_credit_ledger_creative_request_id
    ON platform.credit_ledger (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_credit_ledger_generated_version_id
    ON platform.credit_ledger (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_credit_ledger_generation_job_id
    ON platform.credit_ledger (generation_job_id);

CREATE INDEX IF NOT EXISTS idx_credit_ledger_reference
    ON platform.credit_ledger (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS platform.workspace_usage_summaries (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    usage_month DATE NOT NULL,
    used_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    reserved_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    refunded_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_creative_requests BIGINT NOT NULL DEFAULT 0,
    total_generated_versions BIGINT NOT NULL DEFAULT 0,
    total_layer_executions BIGINT NOT NULL DEFAULT 0,
    total_ai_cost_usd NUMERIC(19,6) NOT NULL DEFAULT 0,
    total_uploads BIGINT NOT NULL DEFAULT 0,
    total_storage_bytes BIGINT NOT NULL DEFAULT 0,
    total_downloads BIGINT NOT NULL DEFAULT 0,
    total_public_shares BIGINT NOT NULL DEFAULT 0,
    total_prompt_enhancements BIGINT NOT NULL DEFAULT 0,
    total_generation_failures BIGINT NOT NULL DEFAULT 0,
    total_api_calls BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_workspace_usage_summaries_workspace_month UNIQUE (workspace_id, usage_month),
    CONSTRAINT chk_workspace_usage_summaries_nonnegative
        CHECK (
            used_credits >= 0
            AND reserved_credits >= 0
            AND refunded_credits >= 0
            AND total_creative_requests >= 0
            AND total_generated_versions >= 0
            AND total_layer_executions >= 0
            AND total_ai_cost_usd >= 0
            AND total_uploads >= 0
            AND total_storage_bytes >= 0
            AND total_downloads >= 0
            AND total_public_shares >= 0
            AND total_prompt_enhancements >= 0
            AND total_generation_failures >= 0
            AND total_api_calls >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_workspace_usage_summaries_workspace_id
    ON platform.workspace_usage_summaries (workspace_id);

CREATE INDEX IF NOT EXISTS idx_workspace_usage_summaries_usage_month
    ON platform.workspace_usage_summaries (usage_month DESC);

CREATE TABLE IF NOT EXISTS platform.download_usage_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID REFERENCES platform.generated_versions (id) ON DELETE SET NULL,
    asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    downloaded_by UUID,
    download_type VARCHAR(60),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_download_usage_logs_workspace_created_at
    ON platform.download_usage_logs (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_download_usage_logs_generated_version_id
    ON platform.download_usage_logs (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_download_usage_logs_asset_id
    ON platform.download_usage_logs (asset_id);

CREATE INDEX IF NOT EXISTS idx_download_usage_logs_downloaded_by
    ON platform.download_usage_logs (downloaded_by);

CREATE TABLE IF NOT EXISTS platform.share_usage_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    share_link_id UUID NOT NULL REFERENCES platform.share_links (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    accessed_by_user_id UUID,
    access_ip VARCHAR(80),
    user_agent VARCHAR(500),
    referrer VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_share_usage_logs_workspace_created_at
    ON platform.share_usage_logs (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_share_usage_logs_share_link_id
    ON platform.share_usage_logs (share_link_id);

CREATE INDEX IF NOT EXISTS idx_share_usage_logs_generated_version_id
    ON platform.share_usage_logs (generated_version_id);

CREATE INDEX IF NOT EXISTS idx_share_usage_logs_accessed_by_user_id
    ON platform.share_usage_logs (accessed_by_user_id);

CREATE TABLE IF NOT EXISTS platform.monthly_usage_snapshots (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    usage_month DATE NOT NULL,
    pricing_plan_id UUID REFERENCES platform.pricing_plans (id) ON DELETE SET NULL,
    subscription_id UUID REFERENCES platform.workspace_subscriptions (id) ON DELETE SET NULL,
    used_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    generated_versions BIGINT NOT NULL DEFAULT 0,
    creative_requests BIGINT NOT NULL DEFAULT 0,
    ai_cost_usd NUMERIC(19,6) NOT NULL DEFAULT 0,
    storage_bytes BIGINT NOT NULL DEFAULT 0,
    downloads BIGINT NOT NULL DEFAULT 0,
    public_shares BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_monthly_usage_snapshots_workspace_month UNIQUE (workspace_id, usage_month),
    CONSTRAINT chk_monthly_usage_snapshots_nonnegative
        CHECK (
            used_credits >= 0
            AND generated_versions >= 0
            AND creative_requests >= 0
            AND ai_cost_usd >= 0
            AND storage_bytes >= 0
            AND downloads >= 0
            AND public_shares >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_monthly_usage_snapshots_workspace_id
    ON platform.monthly_usage_snapshots (workspace_id);

CREATE INDEX IF NOT EXISTS idx_monthly_usage_snapshots_usage_month
    ON platform.monthly_usage_snapshots (usage_month DESC);

CREATE INDEX IF NOT EXISTS idx_monthly_usage_snapshots_pricing_plan_id
    ON platform.monthly_usage_snapshots (pricing_plan_id);

CREATE INDEX IF NOT EXISTS idx_monthly_usage_snapshots_subscription_id
    ON platform.monthly_usage_snapshots (subscription_id);
