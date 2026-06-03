ALTER TABLE platform.plan_feature_policies
    ADD COLUMN IF NOT EXISTS max_assets INTEGER,
    ADD COLUMN IF NOT EXISTS max_creative_requests INTEGER,
    ADD COLUMN IF NOT EXISTS max_generated_versions_per_creative_request INTEGER,
    ADD COLUMN IF NOT EXISTS max_storage_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS prompt_enhancement_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS creative_generation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS download_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS share_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS asset_upload_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS premium_quality_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS voiceover_generation_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_assets;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_assets
        CHECK (max_assets IS NULL OR max_assets >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_creative_requests;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_creative_requests
        CHECK (max_creative_requests IS NULL OR max_creative_requests >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_generated_versions_per_creative_request;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_generated_versions_per_creative_request
        CHECK (max_generated_versions_per_creative_request IS NULL OR max_generated_versions_per_creative_request >= 0);

ALTER TABLE platform.plan_feature_policies
    DROP CONSTRAINT IF EXISTS chk_plan_feature_policies_max_storage_bytes;

ALTER TABLE platform.plan_feature_policies
    ADD CONSTRAINT chk_plan_feature_policies_max_storage_bytes
        CHECK (max_storage_bytes IS NULL OR max_storage_bytes >= 0);

CREATE OR REPLACE VIEW platform.credit_accounts AS
SELECT
    id,
    workspace_id,
    balance AS available_credits,
    reserved_balance AS reserved_credits,
    GREATEST(balance - reserved_balance, 0) AS usable_credits,
    updated_at
FROM platform.credit_wallets
WHERE is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '41')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
