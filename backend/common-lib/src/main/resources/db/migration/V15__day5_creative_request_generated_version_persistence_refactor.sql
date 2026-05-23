ALTER TABLE platform.creative_requests
    ADD COLUMN IF NOT EXISTS selected_asset_ids jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE platform.creative_requests
SET status = 'QUEUED'
WHERE status IN ('REQUESTED', 'RESERVED');

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_status;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_status
        CHECK (status IN ('DRAFT', 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE platform.creative_requests
    DROP CONSTRAINT IF EXISTS chk_creative_requests_selected_asset_ids_json;

ALTER TABLE platform.creative_requests
    ADD CONSTRAINT chk_creative_requests_selected_asset_ids_json
        CHECK (jsonb_typeof(selected_asset_ids) = 'array');

CREATE UNIQUE INDEX IF NOT EXISTS uk_creative_requests_credit_reservation_id
    ON platform.creative_requests (credit_reservation_id)
    WHERE credit_reservation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_creative_requests_workspace_project_campaign_status_created_at
    ON platform.creative_requests (workspace_id, project_campaign_id, status, created_at DESC);

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL;

UPDATE platform.generated_versions generated_version
SET asset_id = creative_output.generated_asset_id
FROM platform.creative_outputs creative_output
WHERE generated_version.id = creative_output.id
  AND generated_version.asset_id IS NULL;

UPDATE platform.generated_versions
SET approval_status = 'NOT_SUBMITTED'
WHERE approval_status = 'DRAFT';

UPDATE platform.generated_versions
SET approval_status = 'SUBMITTED'
WHERE approval_status = 'PENDING';

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_approval_status;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_approval_status
        CHECK (approval_status IN ('NOT_SUBMITTED', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED'));

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_status;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_status
        CHECK (generation_status IN ('QUEUED', 'PROCESSING', 'READY', 'FAILED', 'COMPLETED', 'CANCELLED'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_request_version_number
    ON platform.generated_versions (creative_request_id, version_number)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_versions_asset_id
    ON platform.generated_versions (asset_id);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_request_generation_status
    ON platform.generated_versions (workspace_id, creative_request_id, generation_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_project_campaign_created_at
    ON platform.generated_versions (workspace_id, project_campaign_id, created_at DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '15')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
