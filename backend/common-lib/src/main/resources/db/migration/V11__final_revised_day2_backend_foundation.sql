ALTER TABLE platform.refresh_tokens
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS token_family_id UUID,
    ADD COLUMN IF NOT EXISTS replaced_by_token_id UUID;

UPDATE platform.refresh_tokens
SET device_id = COALESCE(NULLIF(device_id, ''), 'legacy-device')
WHERE device_id IS NULL
   OR device_id = '';

UPDATE platform.refresh_tokens
SET token_family_id = COALESCE(token_family_id, token_id)
WHERE token_family_id IS NULL;

ALTER TABLE platform.refresh_tokens
    ALTER COLUMN device_id SET NOT NULL,
    ALTER COLUMN token_family_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_device_id
    ON platform.refresh_tokens (device_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_family_id
    ON platform.refresh_tokens (token_family_id);

INSERT INTO platform.permissions (code, description)
VALUES
    ('BRAND_VIEW', 'View brands'),
    ('PRODUCT_VIEW', 'View products and services'),
    ('PRODUCT_MANAGE', 'Manage products and services'),
    ('PROJECT_VIEW', 'View projects and campaigns'),
    ('PROJECT_CREATE', 'Create projects and campaigns'),
    ('PROJECT_UPDATE', 'Update projects and campaigns'),
    ('CREATIVE_REQUEST_CREATE', 'Create creative requests'),
    ('CREATIVE_VERSION_EDIT', 'Edit generated creative versions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'BRAND_VIEW'),
    ('MASTER', 'PRODUCT_VIEW'),
    ('MASTER', 'PRODUCT_MANAGE'),
    ('MASTER', 'PROJECT_VIEW'),
    ('MASTER', 'PROJECT_CREATE'),
    ('MASTER', 'PROJECT_UPDATE'),
    ('MASTER', 'CREATIVE_REQUEST_CREATE'),
    ('MASTER', 'CREATIVE_VERSION_EDIT'),
    ('ADMIN', 'BRAND_VIEW'),
    ('ADMIN', 'PRODUCT_VIEW'),
    ('ADMIN', 'PRODUCT_MANAGE'),
    ('ADMIN', 'PROJECT_VIEW'),
    ('ADMIN', 'PROJECT_CREATE'),
    ('ADMIN', 'PROJECT_UPDATE'),
    ('ADMIN', 'CREATIVE_REQUEST_CREATE'),
    ('ADMIN', 'CREATIVE_VERSION_EDIT')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '11')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
