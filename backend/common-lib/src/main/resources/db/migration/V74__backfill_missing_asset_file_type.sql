UPDATE platform.assets
SET file_type = CASE
    WHEN LOWER(COALESCE(mime_type, '')) = 'image/svg+xml'
      OR LOWER(COALESCE(file_extension, '')) = 'svg'
    THEN 'VECTOR_IMAGE'
    WHEN LOWER(COALESCE(mime_type, '')) LIKE 'video/%'
      OR LOWER(COALESCE(file_extension, '')) IN ('mp4', 'mov', 'm4v', 'webm')
    THEN 'VIDEO'
    WHEN LOWER(COALESCE(mime_type, '')) LIKE 'image/%'
      OR LOWER(COALESCE(file_extension, '')) IN ('jpg', 'jpeg', 'png', 'webp', 'gif', 'avif')
    THEN 'IMAGE'
    ELSE file_type
END,
updated_at = NOW()
WHERE file_type IS NULL
  AND (
      NULLIF(mime_type, '') IS NOT NULL
      OR NULLIF(file_extension, '') IS NOT NULL
  );

UPDATE platform.storage_files
SET mime_type = CASE
    WHEN NULLIF(mime_type, '') IS NOT NULL THEN mime_type
    WHEN LOWER(COALESCE(file_extension, '')) IN ('jpg', 'jpeg') THEN 'image/jpeg'
    WHEN LOWER(COALESCE(file_extension, '')) = 'png' THEN 'image/png'
    WHEN LOWER(COALESCE(file_extension, '')) = 'webp' THEN 'image/webp'
    WHEN LOWER(COALESCE(file_extension, '')) = 'svg' THEN 'image/svg+xml'
    WHEN LOWER(COALESCE(file_extension, '')) = 'mp4' THEN 'video/mp4'
    WHEN LOWER(COALESCE(file_extension, '')) = 'mov' THEN 'video/quicktime'
    ELSE mime_type
END,
updated_at = NOW()
WHERE NULLIF(mime_type, '') IS NULL
  AND NULLIF(file_extension, '') IS NOT NULL;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '74')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
