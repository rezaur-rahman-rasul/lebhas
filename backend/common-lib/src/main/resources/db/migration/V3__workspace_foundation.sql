CREATE TABLE IF NOT EXISTS platform.workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    logo_url VARCHAR(300),
    description VARCHAR(1000),
    industry VARCHAR(80),
    timezone VARCHAR(80) NOT NULL,
    language VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    country VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_id UUID NOT NULL REFERENCES platform.users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_workspaces_slug ON platform.workspaces (LOWER(slug));
CREATE INDEX IF NOT EXISTS idx_workspaces_owner_id ON platform.workspaces (owner_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_status ON platform.workspaces (status);

WITH workspace_candidates AS (
    SELECT DISTINCT workspace_id
    FROM platform.workspace_members
    UNION
    SELECT DISTINCT workspace_id
    FROM platform.invitations
),
owner_candidates AS (
    SELECT DISTINCT ON (workspace_id)
        workspace_id,
        user_id AS owner_user_id
    FROM platform.workspace_members
    ORDER BY
        workspace_id,
        CASE WHEN role = 'ADMIN' THEN 0 ELSE 1 END,
        created_at ASC,
        user_id ASC
),
invitation_candidates AS (
    SELECT DISTINCT ON (workspace_id)
        workspace_id,
        invited_by_user_id
    FROM platform.invitations
    ORDER BY workspace_id, created_at ASC, invited_by_user_id ASC
)
INSERT INTO platform.workspaces (
    id,
    name,
    slug,
    logo_url,
    description,
    industry,
    timezone,
    language,
    currency,
    country,
    status,
    owner_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    candidate.workspace_id,
    COALESCE(NULLIF(CONCAT_WS(' ', owner_user.first_name, owner_user.last_name), ''), 'Workspace ' || SUBSTRING(REPLACE(candidate.workspace_id::text, '-', '') FROM 1 FOR 8)),
    LOWER('workspace-' || SUBSTRING(REPLACE(candidate.workspace_id::text, '-', '') FROM 1 FOR 8)),
    NULL,
    NULL,
    NULL,
    'Asia/Dhaka',
    'ENGLISH',
    'BDT',
    'BD',
    'ACTIVE',
    owner_id,
    NOW(),
    NOW(),
    'system',
    'system',
    FALSE
FROM (
    SELECT
        workspace_candidates.workspace_id,
        COALESCE(
            owner_candidates.owner_user_id,
            invitation_candidates.invited_by_user_id,
            (SELECT id FROM platform.users ORDER BY created_at ASC LIMIT 1)
        ) AS owner_id
    FROM workspace_candidates
    LEFT JOIN owner_candidates ON owner_candidates.workspace_id = workspace_candidates.workspace_id
    LEFT JOIN invitation_candidates ON invitation_candidates.workspace_id = workspace_candidates.workspace_id
) candidate
LEFT JOIN platform.users owner_user ON owner_user.id = candidate.owner_id
WHERE candidate.owner_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM platform.workspaces existing
      WHERE existing.id = candidate.workspace_id
  );

ALTER TABLE IF EXISTS platform.workspace_members RENAME TO workspace_memberships;

ALTER TABLE platform.workspace_memberships
    ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ;

UPDATE platform.workspace_memberships
SET joined_at = created_at
WHERE joined_at IS NULL;

ALTER TABLE platform.workspace_memberships
    ALTER COLUMN joined_at SET NOT NULL;

ALTER TABLE platform.workspace_memberships
    ADD COLUMN IF NOT EXISTS invited_by_user_id UUID REFERENCES platform.users (id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'platform'
          AND table_name = 'workspace_memberships'
          AND constraint_name = 'fk_workspace_memberships_workspace_id'
    ) THEN
        ALTER TABLE platform.workspace_memberships
            ADD CONSTRAINT fk_workspace_memberships_workspace_id
            FOREIGN KEY (workspace_id) REFERENCES platform.workspaces (id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS platform.workspace_membership_permissions (
    membership_id UUID NOT NULL REFERENCES platform.workspace_memberships (id) ON DELETE CASCADE,
    permission_code VARCHAR(60) NOT NULL REFERENCES platform.permissions (code),
    PRIMARY KEY (membership_id, permission_code)
);

CREATE TABLE IF NOT EXISTS platform.invitation_permissions (
    invitation_id UUID NOT NULL REFERENCES platform.invitations (id) ON DELETE CASCADE,
    permission_code VARCHAR(60) NOT NULL REFERENCES platform.permissions (code),
    PRIMARY KEY (invitation_id, permission_code)
);

CREATE TABLE IF NOT EXISTS platform.workspace_settings (
    workspace_id UUID PRIMARY KEY REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    allow_crew_download BOOLEAN NOT NULL DEFAULT FALSE,
    allow_crew_publish BOOLEAN NOT NULL DEFAULT FALSE,
    default_language VARCHAR(20) NOT NULL,
    default_timezone VARCHAR(80) NOT NULL,
    notify_crew_invites BOOLEAN NOT NULL DEFAULT TRUE,
    notify_workspace_updates BOOLEAN NOT NULL DEFAULT TRUE,
    notify_security_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    workspace_visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS platform.brand_profiles (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_name VARCHAR(120) NOT NULL,
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
    description VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_brand_profiles_workspace_id UNIQUE (workspace_id)
);

INSERT INTO platform.workspace_settings (
    workspace_id,
    allow_crew_download,
    allow_crew_publish,
    default_language,
    default_timezone,
    notify_crew_invites,
    notify_workspace_updates,
    notify_security_alerts,
    workspace_visibility,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    workspace.id,
    FALSE,
    FALSE,
    workspace.language,
    workspace.timezone,
    TRUE,
    TRUE,
    TRUE,
    'PRIVATE',
    NOW(),
    NOW(),
    'system',
    'system'
FROM platform.workspaces workspace
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.workspace_settings settings
    WHERE settings.workspace_id = workspace.id
);

INSERT INTO platform.brand_profiles (
    id,
    workspace_id,
    brand_name,
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
    description,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    workspace.id,
    workspace.id,
    workspace.name,
    NULL,
    workspace.industry,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW(),
    'system',
    'system',
    FALSE
FROM platform.workspaces workspace
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.brand_profiles brand_profile
    WHERE brand_profile.workspace_id = workspace.id
);

INSERT INTO platform.permissions (code, description)
VALUES
    ('WORKSPACE_CREATE', 'Create workspaces'),
    ('WORKSPACE_UPDATE', 'Update workspace details'),
    ('WORKSPACE_STATUS_UPDATE', 'Update workspace status'),
    ('WORKSPACE_SETTINGS_VIEW', 'View workspace settings'),
    ('WORKSPACE_SETTINGS_UPDATE', 'Update workspace settings'),
    ('BRAND_PROFILE_UPDATE', 'Update workspace brand profile'),
    ('CREW_VIEW', 'View workspace crew members'),
    ('CREW_UPDATE', 'Update workspace crew members'),
    ('CREW_REMOVE', 'Remove workspace crew members'),
    ('CREATIVE_EDIT', 'Edit creatives'),
    ('CREATIVE_DOWNLOAD', 'Download creatives'),
    ('CREATIVE_SUBMIT', 'Submit creatives')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'WORKSPACE_CREATE'),
    ('MASTER', 'WORKSPACE_UPDATE'),
    ('MASTER', 'WORKSPACE_STATUS_UPDATE'),
    ('MASTER', 'WORKSPACE_SETTINGS_VIEW'),
    ('MASTER', 'WORKSPACE_SETTINGS_UPDATE'),
    ('MASTER', 'BRAND_PROFILE_UPDATE'),
    ('MASTER', 'CREW_VIEW'),
    ('MASTER', 'CREW_UPDATE'),
    ('MASTER', 'CREW_REMOVE'),
    ('MASTER', 'CREATIVE_EDIT'),
    ('MASTER', 'CREATIVE_DOWNLOAD'),
    ('MASTER', 'CREATIVE_SUBMIT'),
    ('ADMIN', 'WORKSPACE_CREATE'),
    ('ADMIN', 'WORKSPACE_UPDATE'),
    ('ADMIN', 'WORKSPACE_STATUS_UPDATE'),
    ('ADMIN', 'WORKSPACE_SETTINGS_VIEW'),
    ('ADMIN', 'WORKSPACE_SETTINGS_UPDATE'),
    ('ADMIN', 'BRAND_PROFILE_UPDATE'),
    ('ADMIN', 'CREW_VIEW'),
    ('ADMIN', 'CREW_UPDATE'),
    ('ADMIN', 'CREW_REMOVE'),
    ('ADMIN', 'CREATIVE_EDIT'),
    ('ADMIN', 'CREATIVE_DOWNLOAD'),
    ('ADMIN', 'CREATIVE_SUBMIT')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.workspace_membership_permissions (membership_id, permission_code)
SELECT membership.id, 'WORKSPACE_VIEW'
FROM platform.workspace_memberships membership
WHERE membership.role = 'CREW'
ON CONFLICT (membership_id, permission_code) DO NOTHING;

INSERT INTO platform.workspace_membership_permissions (membership_id, permission_code)
SELECT membership.id, 'CREATIVE_GENERATE'
FROM platform.workspace_memberships membership
WHERE membership.role = 'CREW'
ON CONFLICT (membership_id, permission_code) DO NOTHING;

INSERT INTO platform.invitation_permissions (invitation_id, permission_code)
SELECT invitation.id, 'WORKSPACE_VIEW'
FROM platform.invitations invitation
WHERE invitation.role = 'CREW'
ON CONFLICT (invitation_id, permission_code) DO NOTHING;

INSERT INTO platform.invitation_permissions (invitation_id, permission_code)
SELECT invitation.id, 'CREATIVE_GENERATE'
FROM platform.invitations invitation
WHERE invitation.role = 'CREW'
ON CONFLICT (invitation_id, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '3')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
