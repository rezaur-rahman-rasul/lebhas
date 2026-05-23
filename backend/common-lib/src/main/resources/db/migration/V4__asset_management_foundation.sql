CREATE TABLE IF NOT EXISTS platform.asset_folders (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    name VARCHAR(120) NOT NULL,
    parent_folder_id UUID REFERENCES platform.asset_folders (id),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_asset_folders_workspace_id ON platform.asset_folders (workspace_id);
CREATE INDEX IF NOT EXISTS idx_asset_folders_parent_folder_id ON platform.asset_folders (parent_folder_id);
CREATE INDEX IF NOT EXISTS idx_asset_folders_workspace_parent ON platform.asset_folders (workspace_id, parent_folder_id);

CREATE TABLE IF NOT EXISTS platform.assets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id),
    uploaded_by UUID NOT NULL REFERENCES platform.users (id),
    folder_id UUID REFERENCES platform.asset_folders (id),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255),
    file_type VARCHAR(30),
    mime_type VARCHAR(120),
    file_extension VARCHAR(20),
    file_size BIGINT NOT NULL DEFAULT 0,
    storage_provider VARCHAR(30) NOT NULL,
    storage_bucket VARCHAR(120),
    storage_key VARCHAR(600),
    public_url VARCHAR(1000),
    preview_url VARCHAR(1000),
    thumbnail_url VARCHAR(1000),
    asset_category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    width INTEGER,
    height INTEGER,
    duration BIGINT,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_assets_workspace_id ON platform.assets (workspace_id);
CREATE INDEX IF NOT EXISTS idx_assets_folder_id ON platform.assets (folder_id);
CREATE INDEX IF NOT EXISTS idx_assets_uploaded_by ON platform.assets (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_assets_asset_category ON platform.assets (asset_category);
CREATE INDEX IF NOT EXISTS idx_assets_status ON platform.assets (status);
CREATE INDEX IF NOT EXISTS idx_assets_created_at ON platform.assets (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_assets_workspace_status_created_at ON platform.assets (workspace_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.asset_tags (
    asset_id UUID NOT NULL REFERENCES platform.assets (id) ON DELETE CASCADE,
    tag VARCHAR(80) NOT NULL,
    PRIMARY KEY (asset_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_asset_tags_tag ON platform.asset_tags (LOWER(tag));

INSERT INTO platform.permissions (code, description)
VALUES
    ('ASSET_VIEW', 'View workspace assets'),
    ('ASSET_UPLOAD', 'Upload workspace assets'),
    ('ASSET_UPDATE', 'Update workspace assets'),
    ('ASSET_DELETE', 'Delete workspace assets'),
    ('ASSET_FOLDER_MANAGE', 'Manage workspace asset folders')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'ASSET_VIEW'),
    ('MASTER', 'ASSET_UPLOAD'),
    ('MASTER', 'ASSET_UPDATE'),
    ('MASTER', 'ASSET_DELETE'),
    ('MASTER', 'ASSET_FOLDER_MANAGE'),
    ('ADMIN', 'ASSET_VIEW'),
    ('ADMIN', 'ASSET_UPLOAD'),
    ('ADMIN', 'ASSET_UPDATE'),
    ('ADMIN', 'ASSET_DELETE'),
    ('ADMIN', 'ASSET_FOLDER_MANAGE')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.workspace_membership_permissions (membership_id, permission_code)
SELECT membership.id, 'ASSET_VIEW'
FROM platform.workspace_memberships membership
WHERE membership.role = 'CREW'
ON CONFLICT (membership_id, permission_code) DO NOTHING;

INSERT INTO platform.invitation_permissions (invitation_id, permission_code)
SELECT invitation.id, 'ASSET_VIEW'
FROM platform.invitations invitation
WHERE invitation.role = 'CREW'
ON CONFLICT (invitation_id, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '4')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
