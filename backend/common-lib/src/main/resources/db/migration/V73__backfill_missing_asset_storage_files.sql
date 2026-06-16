INSERT INTO platform.storage_files (
    id,
    workspace_id,
    project_id,
    provider,
    bucket,
    object_key,
    cdn_url,
    mime_type,
    file_extension,
    file_size,
    hash,
    width,
    height,
    duration,
    storage_class,
    file_purpose,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    asset.id,
    asset.workspace_id,
    asset.project_id,
    asset.storage_provider,
    COALESCE(NULLIF(asset.storage_bucket, ''), 'creative-saas-assets'),
    asset.storage_key,
    COALESCE(NULLIF(asset.public_url, ''), NULLIF(asset.preview_url, ''), NULLIF(asset.thumbnail_url, '')),
    asset.mime_type,
    asset.file_extension,
    asset.file_size,
    asset.checksum,
    asset.width,
    asset.height,
    asset.duration,
    'STANDARD',
    CASE
        WHEN asset.asset_type IN ('GENERATED', 'GENERATED_CREATIVE')
          OR asset.asset_category IN ('GENERATED_CREATIVE', 'EXPORT_IMAGE', 'EXPORT_VIDEO')
          OR asset.source_type IN ('mock-generation', 'AI_CREATIVE', 'IMAGE_CREATIVE')
          OR asset.storage_key LIKE '%generated%'
        THEN 'GENERATED'
        ELSE 'RAW'
    END,
    asset.created_at,
    NOW(),
    asset.created_by,
    asset.updated_by,
    asset.is_deleted
FROM platform.assets asset
WHERE asset.storage_file_id IS NULL
  AND NULLIF(asset.storage_key, '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM platform.storage_files storage_file
      WHERE storage_file.id = asset.id
         OR (
             storage_file.workspace_id = asset.workspace_id
             AND storage_file.object_key = asset.storage_key
             AND storage_file.is_deleted = FALSE
         )
  );

UPDATE platform.assets asset
SET storage_file_id = storage_file.id,
    updated_at = NOW()
FROM platform.storage_files storage_file
WHERE asset.storage_file_id IS NULL
  AND NULLIF(asset.storage_key, '') IS NOT NULL
  AND storage_file.workspace_id = asset.workspace_id
  AND storage_file.object_key = asset.storage_key
  AND storage_file.is_deleted = FALSE;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '73')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
