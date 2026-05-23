ALTER TABLE platform.assets
    ADD COLUMN IF NOT EXISTS brand_id UUID REFERENCES platform.brands (id),
    ADD COLUMN IF NOT EXISTS product_service_id UUID REFERENCES platform.product_services (id),
    ADD COLUMN IF NOT EXISTS asset_type VARCHAR(30) NOT NULL DEFAULT 'RAW',
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS upload_session_id UUID,
    ADD COLUMN IF NOT EXISTS preview_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

ALTER TABLE platform.assets
    DROP CONSTRAINT IF EXISTS assets_project_id_fkey;

ALTER TABLE platform.assets
    ADD CONSTRAINT assets_project_campaign_id_fkey
        FOREIGN KEY (project_id) REFERENCES platform.project_campaigns (id) ON DELETE CASCADE;

ALTER TABLE platform.storage_files
    DROP CONSTRAINT IF EXISTS storage_files_project_id_fkey;

ALTER TABLE platform.storage_files
    ADD CONSTRAINT storage_files_project_campaign_id_fkey
        FOREIGN KEY (project_id) REFERENCES platform.project_campaigns (id) ON DELETE CASCADE;

UPDATE platform.assets asset
SET brand_id = campaign.brand_id,
    product_service_id = campaign.product_service_id
FROM platform.project_campaigns campaign
WHERE asset.project_id = campaign.id
  AND (asset.brand_id IS NULL OR asset.product_service_id IS NULL);

UPDATE platform.assets
SET display_name = COALESCE(NULLIF(display_name, ''), original_file_name)
WHERE display_name IS NULL
   OR display_name = '';

UPDATE platform.assets
SET asset_type = CASE
    WHEN asset_type IS NOT NULL AND asset_type <> '' THEN asset_type
    WHEN asset_category IN ('OTHER') AND COALESCE(storage_key, '') LIKE 'generated/%' THEN 'GENERATED'
    ELSE 'RAW'
END;

UPDATE platform.assets
SET asset_category = CASE asset_category
    WHEN 'RAW_IMAGE' THEN 'REFERENCE_IMAGE'
    WHEN 'RAW_VIDEO' THEN 'REFERENCE_VIDEO'
    WHEN 'GENERATED_IMAGE' THEN 'OTHER'
    WHEN 'GENERATED_VIDEO' THEN 'OTHER'
    WHEN 'THUMBNAIL' THEN 'OTHER'
    ELSE asset_category
END;

UPDATE platform.assets
SET status = CASE status
    WHEN 'PROCESSING' THEN 'UPLOADING'
    WHEN 'ACTIVE' THEN 'READY'
    ELSE status
END;

UPDATE platform.assets
SET processing_status = CASE processing_status
    WHEN 'PROCESSING' THEN 'PROCESSING'
    WHEN 'UPLOADING' THEN 'UPLOADING'
    WHEN 'READY' THEN 'READY'
    WHEN 'FAILED' THEN 'FAILED'
    WHEN 'PENDING' THEN 'PENDING'
    ELSE CASE status
        WHEN 'UPLOADING' THEN 'UPLOADING'
        WHEN 'READY' THEN 'READY'
        WHEN 'FAILED' THEN 'FAILED'
        ELSE 'PENDING'
    END
END;

UPDATE platform.assets
SET preview_status = CASE preview_status
    WHEN 'READY' THEN 'READY'
    WHEN 'FAILED' THEN 'FAILED'
    WHEN 'PROCESSING' THEN 'PROCESSING'
    WHEN 'PENDING' THEN 'PENDING'
    ELSE CASE status
        WHEN 'READY' THEN 'READY'
        WHEN 'FAILED' THEN 'FAILED'
        ELSE 'PENDING'
    END
END;

CREATE INDEX IF NOT EXISTS idx_assets_brand_id ON platform.assets (brand_id);
CREATE INDEX IF NOT EXISTS idx_assets_product_service_id ON platform.assets (product_service_id);
CREATE INDEX IF NOT EXISTS idx_assets_storage_file_id ON platform.assets (storage_file_id);
CREATE INDEX IF NOT EXISTS idx_assets_upload_session_id ON platform.assets (upload_session_id);
CREATE INDEX IF NOT EXISTS idx_assets_asset_type ON platform.assets (asset_type);

CREATE TABLE IF NOT EXISTS platform.upload_sessions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID REFERENCES platform.brands (id) ON DELETE SET NULL,
    product_service_id UUID REFERENCES platform.product_services (id) ON DELETE SET NULL,
    project_id UUID NOT NULL REFERENCES platform.project_campaigns (id) ON DELETE CASCADE,
    asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    uploaded_by UUID NOT NULL REFERENCES platform.users (id),
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120),
    file_size BIGINT NOT NULL DEFAULT 0,
    hash VARCHAR(128),
    status VARCHAR(30) NOT NULL,
    chunk_count INTEGER NOT NULL DEFAULT 1,
    completed_chunk_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_upload_sessions_workspace_id ON platform.upload_sessions (workspace_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_brand_id ON platform.upload_sessions (brand_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_product_service_id ON platform.upload_sessions (product_service_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_project_id ON platform.upload_sessions (project_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_asset_id ON platform.upload_sessions (asset_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_uploaded_by ON platform.upload_sessions (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_hash ON platform.upload_sessions (hash);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_status ON platform.upload_sessions (status);

ALTER TABLE platform.download_logs
    ADD COLUMN IF NOT EXISTS asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL;

ALTER TABLE platform.download_logs
    ALTER COLUMN generated_version_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_download_logs_asset_id ON platform.download_logs (asset_id);
CREATE INDEX IF NOT EXISTS idx_download_logs_downloaded_by ON platform.download_logs (downloaded_by);
CREATE INDEX IF NOT EXISTS idx_download_logs_download_type ON platform.download_logs (download_type);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '12')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
