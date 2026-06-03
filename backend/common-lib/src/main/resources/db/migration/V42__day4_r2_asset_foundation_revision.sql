ALTER TABLE platform.assets
    ALTER COLUMN project_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS asset_type VARCHAR(30);

UPDATE platform.assets
SET asset_type = CASE
    WHEN asset_type IS NOT NULL THEN asset_type
    WHEN asset_category = 'PRODUCT_IMAGE' THEN 'PRODUCT_IMAGE'
    WHEN asset_category = 'REFERENCE_IMAGE' THEN 'REFERENCE_ASSET'
    WHEN asset_category = 'BRAND_LOGO' THEN 'BRAND_LOGO'
    ELSE 'RAW_IMAGE'
END;

ALTER TABLE platform.assets
    ALTER COLUMN asset_type SET NOT NULL;

ALTER TABLE platform.upload_sessions
    ALTER COLUMN project_id DROP NOT NULL;

ALTER TABLE platform.storage_files
    ALTER COLUMN project_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_assets_workspace_project_created_at
    ON platform.assets (workspace_id, project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_assets_workspace_source_type
    ON platform.assets (workspace_id, source_type);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '42')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
