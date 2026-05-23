CREATE TABLE IF NOT EXISTS platform.brands (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES platform.users (id),
    name VARCHAR(120) NOT NULL,
    business_type VARCHAR(80),
    industry VARCHAR(80),
    target_audience VARCHAR(160),
    brand_voice VARCHAR(120),
    preferred_cta VARCHAR(120),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    website VARCHAR(300),
    facebook_url VARCHAR(300),
    instagram_url VARCHAR(300),
    linkedin_url VARCHAR(300),
    tiktok_url VARCHAR(300),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_brands_workspace_id
    ON platform.brands (workspace_id);
CREATE INDEX IF NOT EXISTS idx_brands_owner_user_id
    ON platform.brands (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_brands_status
    ON platform.brands (status);
CREATE INDEX IF NOT EXISTS idx_brands_created_by
    ON platform.brands (created_by);
CREATE UNIQUE INDEX IF NOT EXISTS uk_brands_workspace_name
    ON platform.brands (workspace_id, LOWER(name))
    WHERE is_deleted = FALSE;

INSERT INTO platform.brands (
    id,
    workspace_id,
    owner_user_id,
    name,
    business_type,
    industry,
    target_audience,
    brand_voice,
    preferred_cta,
    primary_color,
    secondary_color,
    website,
    facebook_url,
    instagram_url,
    linkedin_url,
    tiktok_url,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    profile.id,
    profile.workspace_id,
    workspace.owner_id,
    profile.brand_name,
    profile.business_type,
    profile.industry,
    profile.target_audience,
    profile.brand_voice,
    profile.preferred_cta,
    profile.primary_color,
    profile.secondary_color,
    profile.website,
    profile.facebook_url,
    profile.instagram_url,
    profile.linkedin_url,
    profile.tiktok_url,
    'ACTIVE',
    profile.created_at,
    profile.updated_at,
    profile.created_by,
    profile.updated_by,
    profile.is_deleted
FROM platform.brand_profiles profile
JOIN platform.workspaces workspace ON workspace.id = profile.workspace_id
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.brands brand
    WHERE brand.id = profile.id
);

INSERT INTO platform.brands (
    id,
    workspace_id,
    owner_user_id,
    name,
    industry,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    workspace.id,
    workspace.id,
    workspace.owner_id,
    workspace.name,
    workspace.industry,
    'ACTIVE',
    workspace.created_at,
    workspace.updated_at,
    COALESCE(workspace.created_by, 'system'),
    COALESCE(workspace.updated_by, 'system'),
    workspace.is_deleted
FROM platform.workspaces workspace
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.brands brand
    WHERE brand.workspace_id = workspace.id
);

CREATE TABLE IF NOT EXISTS platform.projects (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID NOT NULL REFERENCES platform.brands (id) ON DELETE CASCADE,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(1000),
    campaign_objective VARCHAR(40),
    target_platform VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_projects_workspace_id
    ON platform.projects (workspace_id);
CREATE INDEX IF NOT EXISTS idx_projects_brand_id
    ON platform.projects (brand_id);
CREATE INDEX IF NOT EXISTS idx_projects_status
    ON platform.projects (status);
CREATE INDEX IF NOT EXISTS idx_projects_created_by
    ON platform.projects (created_by);
CREATE UNIQUE INDEX IF NOT EXISTS uk_projects_workspace_brand_name
    ON platform.projects (workspace_id, brand_id, LOWER(name))
    WHERE is_deleted = FALSE;

WITH seeded_projects AS (
    SELECT
        brand.id AS brand_id,
        brand.workspace_id,
        brand.created_at,
        brand.created_by,
        brand.updated_by,
        md5('default-project:' || brand.id::text) AS project_hash
    FROM platform.brands brand
)
INSERT INTO platform.projects (
    id,
    workspace_id,
    brand_id,
    name,
    description,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    (
        SUBSTRING(seed.project_hash FROM 1 FOR 8) || '-' ||
        SUBSTRING(seed.project_hash FROM 9 FOR 4) || '-' ||
        SUBSTRING(seed.project_hash FROM 13 FOR 4) || '-' ||
        SUBSTRING(seed.project_hash FROM 17 FOR 4) || '-' ||
        SUBSTRING(seed.project_hash FROM 21 FOR 12)
    )::uuid,
    seed.workspace_id,
    seed.brand_id,
    brand.name || ' Default Project',
    'Primary project for ' || brand.name,
    'ACTIVE',
    COALESCE(seed.created_at, NOW()),
    NOW(),
    COALESCE(seed.created_by, 'system'),
    COALESCE(seed.updated_by, 'system'),
    FALSE
FROM seeded_projects seed
JOIN platform.brands brand ON brand.id = seed.brand_id
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.projects project
    WHERE project.brand_id = seed.brand_id
);

ALTER TABLE platform.assets
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES platform.projects (id),
    ADD COLUMN IF NOT EXISTS storage_file_id UUID;

WITH default_projects AS (
    SELECT DISTINCT ON (workspace_id)
        workspace_id,
        id AS project_id
    FROM platform.projects
    WHERE is_deleted = FALSE
    ORDER BY workspace_id, created_at ASC, id ASC
)
UPDATE platform.assets asset
SET project_id = default_projects.project_id
FROM default_projects
WHERE asset.workspace_id = default_projects.workspace_id
  AND asset.project_id IS NULL;

ALTER TABLE platform.assets
    ALTER COLUMN project_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_assets_project_id
    ON platform.assets (project_id);

ALTER TABLE platform.prompt_history
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES platform.projects (id);

WITH default_projects AS (
    SELECT DISTINCT ON (workspace_id)
        workspace_id,
        id AS project_id
    FROM platform.projects
    WHERE is_deleted = FALSE
    ORDER BY workspace_id, created_at ASC, id ASC
)
UPDATE platform.prompt_history prompt_history
SET project_id = default_projects.project_id
FROM default_projects
WHERE prompt_history.workspace_id = default_projects.workspace_id
  AND prompt_history.project_id IS NULL;

ALTER TABLE platform.prompt_history
    ALTER COLUMN project_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_prompt_history_project_id
    ON platform.prompt_history (project_id);

ALTER TABLE platform.creative_generation_requests
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES platform.projects (id);

WITH default_projects AS (
    SELECT DISTINCT ON (workspace_id)
        workspace_id,
        id AS project_id
    FROM platform.projects
    WHERE is_deleted = FALSE
    ORDER BY workspace_id, created_at ASC, id ASC
)
UPDATE platform.creative_generation_requests request
SET project_id = default_projects.project_id
FROM default_projects
WHERE request.workspace_id = default_projects.workspace_id
  AND request.project_id IS NULL;

ALTER TABLE platform.creative_generation_requests
    ALTER COLUMN project_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_creative_generation_requests_project_id
    ON platform.creative_generation_requests (project_id);

ALTER TABLE platform.creative_outputs
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES platform.projects (id),
    ADD COLUMN IF NOT EXISTS storage_file_id UUID;

UPDATE platform.creative_outputs output
SET project_id = request.project_id
FROM platform.creative_generation_requests request
WHERE output.request_id = request.id
  AND output.project_id IS NULL;

ALTER TABLE platform.creative_outputs
    ALTER COLUMN project_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_creative_outputs_project_id
    ON platform.creative_outputs (project_id);

ALTER TABLE platform.creative_approvals
    ADD COLUMN IF NOT EXISTS project_id UUID REFERENCES platform.projects (id);

UPDATE platform.creative_approvals approval
SET project_id = request.project_id
FROM platform.creative_generation_requests request
WHERE approval.generation_request_id = request.id
  AND approval.project_id IS NULL;

ALTER TABLE platform.creative_approvals
    ALTER COLUMN project_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_creative_approvals_project_id
    ON platform.creative_approvals (project_id);

CREATE TABLE IF NOT EXISTS platform.storage_files (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES platform.projects (id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    bucket VARCHAR(160) NOT NULL,
    object_key VARCHAR(600) NOT NULL,
    cdn_url VARCHAR(1000),
    mime_type VARCHAR(120),
    file_extension VARCHAR(20),
    file_size BIGINT NOT NULL DEFAULT 0,
    hash VARCHAR(128),
    width INTEGER,
    height INTEGER,
    duration BIGINT,
    storage_class VARCHAR(40) NOT NULL,
    file_purpose VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_storage_files_workspace_id
    ON platform.storage_files (workspace_id);
CREATE INDEX IF NOT EXISTS idx_storage_files_project_id
    ON platform.storage_files (project_id);
CREATE INDEX IF NOT EXISTS idx_storage_files_hash
    ON platform.storage_files (hash);
CREATE INDEX IF NOT EXISTS idx_storage_files_created_by
    ON platform.storage_files (created_by);

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
    COALESCE(asset.storage_bucket, 'creative-saas-assets'),
    COALESCE(asset.storage_key, 'raw/workspaces/' || asset.workspace_id::text || '/projects/' || asset.project_id::text || '/' || asset.id::text),
    COALESCE(asset.public_url, asset.preview_url, asset.thumbnail_url),
    asset.mime_type,
    asset.file_extension,
    asset.file_size,
    NULL,
    asset.width,
    asset.height,
    asset.duration,
    'STANDARD',
    CASE
        WHEN asset.asset_category IN ('GENERATED_IMAGE', 'GENERATED_VIDEO') THEN 'GENERATED'
        ELSE 'RAW'
    END,
    asset.created_at,
    asset.updated_at,
    asset.created_by,
    asset.updated_by,
    asset.is_deleted
FROM platform.assets asset
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.storage_files storage_file
    WHERE storage_file.id = asset.id
);

UPDATE platform.assets
SET storage_file_id = id
WHERE storage_file_id IS NULL;

UPDATE platform.creative_outputs output
SET storage_file_id = asset.storage_file_id
FROM platform.assets asset
WHERE output.generated_asset_id = asset.id
  AND output.storage_file_id IS NULL;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '9')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
