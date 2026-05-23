ALTER TABLE platform.creative_requests
    ADD COLUMN IF NOT EXISTS brand_id UUID REFERENCES platform.brands (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS product_service_id UUID REFERENCES platform.product_services (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS language_preference VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    ADD COLUMN IF NOT EXISTS creative_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS campaign_tone VARCHAR(160),
    ADD COLUMN IF NOT EXISTS target_audience VARCHAR(240),
    ADD COLUMN IF NOT EXISTS cta_preference VARCHAR(160),
    ADD COLUMN IF NOT EXISTS requested_versions INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS generated_version_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generation_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS generation_completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;

UPDATE platform.creative_requests creative_request
SET brand_id = project_campaign.brand_id,
    product_service_id = project_campaign.product_service_id
FROM platform.project_campaigns project_campaign
WHERE creative_request.project_campaign_id = project_campaign.id
  AND (creative_request.brand_id IS NULL OR creative_request.product_service_id IS NULL);

UPDATE platform.creative_requests creative_request
SET language_preference = COALESCE(brand.language_preference, 'BOTH')
FROM platform.brands brand
WHERE creative_request.brand_id = brand.id
  AND (creative_request.language_preference IS NULL OR creative_request.language_preference = '');

UPDATE platform.creative_requests
SET language_preference = 'BOTH'
WHERE language_preference IS NULL OR language_preference = '';

UPDATE platform.creative_requests
SET target_platform = NULL
WHERE target_platform IS NOT NULL
  AND target_platform NOT IN ('FACEBOOK', 'INSTAGRAM', 'TIKTOK', 'LINKEDIN');

UPDATE platform.creative_requests
SET creative_objective = NULL
WHERE creative_objective IS NOT NULL
  AND creative_objective NOT IN ('AWARENESS', 'TRAFFIC', 'ENGAGEMENT', 'LEADS', 'SALES', 'APP_PROMOTION', 'BRAND_PROMOTION');

UPDATE platform.creative_requests
SET creative_type = CASE
    WHEN requested_format IN ('PNG', 'JPG', 'WEBP') THEN 'STATIC_IMAGE'
    WHEN requested_format IN ('MP4', 'MOV') THEN 'SHORT_VIDEO'
    ELSE creative_type
END
WHERE creative_type IS NULL;

UPDATE platform.creative_requests creative_request
SET generated_version_count = generated_version_counts.generated_version_count
FROM (
    SELECT creative_request_id, COUNT(*)::INTEGER AS generated_version_count
    FROM platform.generated_versions
    WHERE is_deleted = FALSE
    GROUP BY creative_request_id
) generated_version_counts
WHERE creative_request.id = generated_version_counts.creative_request_id;

UPDATE platform.creative_requests
SET requested_versions = CASE
    WHEN generated_version_count > 0 THEN generated_version_count
    ELSE 1
END
WHERE requested_versions < 1;

UPDATE platform.creative_requests
SET generation_started_at = created_at
WHERE generation_started_at IS NULL
  AND status IN ('PROCESSING', 'COMPLETED', 'FAILED');

UPDATE platform.creative_requests
SET generation_completed_at = updated_at
WHERE generation_completed_at IS NULL
  AND status IN ('COMPLETED', 'FAILED');

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_target_platform;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_target_platform
        CHECK (target_platform IN ('FACEBOOK', 'INSTAGRAM', 'TIKTOK', 'LINKEDIN') OR target_platform IS NULL);

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_creative_objective;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_creative_objective
        CHECK (creative_objective IN ('AWARENESS', 'TRAFFIC', 'ENGAGEMENT', 'LEADS', 'SALES', 'APP_PROMOTION', 'BRAND_PROMOTION') OR creative_objective IS NULL);

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_language_preference;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_language_preference
        CHECK (language_preference IN ('BANGLA', 'ENGLISH', 'BOTH'));

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_creative_type;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_creative_type
        CHECK (creative_type IN ('STATIC_IMAGE', 'CAROUSEL_IMAGE', 'SHORT_VIDEO', 'PRODUCT_PROMO_VIDEO', 'STORY_CREATIVE', 'MOTION_GRAPHIC') OR creative_type IS NULL);

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_requested_versions;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_requested_versions
        CHECK (requested_versions >= 1);

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_generated_version_count;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_generated_version_count
        CHECK (generated_version_count >= 0);

CREATE INDEX IF NOT EXISTS idx_creative_requests_brand_id
    ON platform.creative_requests (brand_id);

CREATE INDEX IF NOT EXISTS idx_creative_requests_product_service_id
    ON platform.creative_requests (product_service_id);

CREATE INDEX IF NOT EXISTS idx_creative_requests_creative_type
    ON platform.creative_requests (creative_type);

CREATE INDEX IF NOT EXISTS idx_creative_requests_workspace_status_created_at
    ON platform.creative_requests (workspace_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_creative_requests_workspace_brand_created_at
    ON platform.creative_requests (workspace_id, brand_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_creative_requests_workspace_product_service_created_at
    ON platform.creative_requests (workspace_id, product_service_id, created_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '25')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
