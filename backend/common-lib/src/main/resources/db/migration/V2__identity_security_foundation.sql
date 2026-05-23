CREATE TABLE IF NOT EXISTS platform.roles (
    code VARCHAR(30) PRIMARY KEY,
    description VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform.permissions (
    code VARCHAR(60) PRIMARY KEY,
    description VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform.role_permissions (
    role_code VARCHAR(30) NOT NULL REFERENCES platform.roles (code),
    permission_code VARCHAR(60) NOT NULL REFERENCES platform.permissions (code),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE IF NOT EXISTS platform.users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(30),
    password VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON platform.users (LOWER(email));

CREATE TABLE IF NOT EXISTS platform.workspace_members (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES platform.users (id),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_workspace_members_workspace_id ON platform.workspace_members (workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_id ON platform.workspace_members (user_id);

CREATE TABLE IF NOT EXISTS platform.refresh_tokens (
    id UUID PRIMARY KEY,
    token_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES platform.users (id),
    workspace_id UUID,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    client_ip VARCHAR(60),
    user_agent VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_tokens_token_id ON platform.refresh_tokens (token_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON platform.refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_workspace_id ON platform.refresh_tokens (workspace_id);

CREATE TABLE IF NOT EXISTS platform.invitations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    token_id UUID NOT NULL,
    email VARCHAR(160) NOT NULL,
    role VARCHAR(20) NOT NULL,
    invited_by_user_id UUID NOT NULL REFERENCES platform.users (id),
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_invitations_token_id ON platform.invitations (token_id);
CREATE INDEX IF NOT EXISTS idx_invitations_workspace_id ON platform.invitations (workspace_id);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON platform.invitations (LOWER(email));

INSERT INTO platform.roles (code, description)
VALUES
    ('MASTER', 'Platform-level unrestricted administrator'),
    ('ADMIN', 'Workspace administrator'),
    ('CREW', 'Workspace crew member')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.permissions (code, description)
VALUES
    ('USER_VIEW', 'View workspace users'),
    ('USER_CREATE', 'Create workspace users'),
    ('USER_UPDATE', 'Update workspace users'),
    ('USER_STATUS_UPDATE', 'Update user status'),
    ('CREW_INVITE', 'Invite crew members'),
    ('WORKSPACE_VIEW', 'View workspace data'),
    ('CREATIVE_GENERATE', 'Generate creatives'),
    ('SESSION_MANAGE', 'Manage user sessions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'USER_VIEW'),
    ('MASTER', 'USER_CREATE'),
    ('MASTER', 'USER_UPDATE'),
    ('MASTER', 'USER_STATUS_UPDATE'),
    ('MASTER', 'CREW_INVITE'),
    ('MASTER', 'WORKSPACE_VIEW'),
    ('MASTER', 'CREATIVE_GENERATE'),
    ('MASTER', 'SESSION_MANAGE'),
    ('ADMIN', 'USER_VIEW'),
    ('ADMIN', 'USER_CREATE'),
    ('ADMIN', 'USER_UPDATE'),
    ('ADMIN', 'USER_STATUS_UPDATE'),
    ('ADMIN', 'CREW_INVITE'),
    ('ADMIN', 'WORKSPACE_VIEW'),
    ('ADMIN', 'CREATIVE_GENERATE'),
    ('ADMIN', 'SESSION_MANAGE'),
    ('CREW', 'WORKSPACE_VIEW'),
    ('CREW', 'CREATIVE_GENERATE')
ON CONFLICT (role_code, permission_code) DO NOTHING;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '2')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
