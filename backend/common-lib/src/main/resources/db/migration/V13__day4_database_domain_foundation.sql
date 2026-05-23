ALTER TABLE platform.assets
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(128);

UPDATE platform.assets asset
SET checksum = storage_file.hash
FROM platform.storage_files storage_file
WHERE asset.storage_file_id = storage_file.id
  AND (asset.checksum IS NULL OR asset.checksum = '');

UPDATE platform.assets asset
SET checksum = upload_session.hash
FROM platform.upload_sessions upload_session
WHERE asset.upload_session_id = upload_session.id
  AND (asset.checksum IS NULL OR asset.checksum = '');

CREATE INDEX IF NOT EXISTS idx_assets_checksum ON platform.assets (checksum);
CREATE INDEX IF NOT EXISTS idx_assets_workspace_checksum ON platform.assets (workspace_id, checksum);

CREATE TABLE IF NOT EXISTS platform.asset_variants (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES platform.assets (id) ON DELETE CASCADE,
    variant_type VARCHAR(30) NOT NULL,
    storage_key VARCHAR(600) NOT NULL,
    width INTEGER,
    height INTEGER,
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_asset_variants_asset_variant_type
    ON platform.asset_variants (asset_id, variant_type);
CREATE INDEX IF NOT EXISTS idx_asset_variants_workspace_id ON platform.asset_variants (workspace_id);
CREATE INDEX IF NOT EXISTS idx_asset_variants_asset_id ON platform.asset_variants (asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_variants_variant_type ON platform.asset_variants (variant_type);
CREATE INDEX IF NOT EXISTS idx_asset_variants_storage_key ON platform.asset_variants (storage_key);

ALTER TABLE platform.public_share_links
    ADD COLUMN IF NOT EXISTS password_protected BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS access_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID REFERENCES platform.users (id);

UPDATE platform.public_share_links share_link
SET created_by_user_id = generated_version.created_by_user_id
FROM platform.generated_versions generated_version
WHERE share_link.generated_version_id = generated_version.id
  AND share_link.created_by_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_public_share_links_created_by_user_id
    ON platform.public_share_links (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_public_share_links_expires_at
    ON platform.public_share_links (expires_at);

CREATE INDEX IF NOT EXISTS idx_download_logs_workspace_asset_created_at
    ON platform.download_logs (workspace_id, asset_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_download_logs_workspace_generated_version_created_at
    ON platform.download_logs (workspace_id, generated_version_id, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.storage_usage (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL UNIQUE REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    total_bytes_used BIGINT NOT NULL DEFAULT 0,
    total_uploads BIGINT NOT NULL DEFAULT 0,
    total_generated_assets BIGINT NOT NULL DEFAULT 0,
    deleted_asset_count BIGINT NOT NULL DEFAULT 0,
    deleted_bytes_pending_cleanup BIGINT NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_storage_usage_workspace_id ON platform.storage_usage (workspace_id);
CREATE INDEX IF NOT EXISTS idx_storage_usage_total_bytes_used ON platform.storage_usage (total_bytes_used DESC);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '13')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
