ALTER TABLE platform.share_links
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(120),
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked_by UUID REFERENCES platform.users (id);

UPDATE platform.share_links
SET token_hash = token
WHERE token_hash IS NULL
  AND token IS NOT NULL;

ALTER TABLE platform.share_links
    ALTER COLUMN token DROP NOT NULL;

ALTER TABLE platform.share_links
    ALTER COLUMN token_hash SET NOT NULL;

DROP INDEX IF EXISTS platform.uk_share_links_token;

CREATE UNIQUE INDEX IF NOT EXISTS uk_share_links_token_hash
    ON platform.share_links (token_hash);

CREATE INDEX IF NOT EXISTS idx_share_links_workspace_generated_version_revoked
    ON platform.share_links (workspace_id, generated_version_id, revoked_at);

CREATE TABLE IF NOT EXISTS platform.generated_version_approval_history (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    action_by UUID NOT NULL REFERENCES platform.users (id),
    comment VARCHAR(2000),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_generated_version_approval_history_version
        FOREIGN KEY (generated_version_id, workspace_id)
        REFERENCES platform.generated_versions (id, workspace_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_generated_version_approval_history_action
        CHECK (action IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES'))
);

CREATE INDEX IF NOT EXISTS idx_generated_version_approval_history_workspace_version_created
    ON platform.generated_version_approval_history (workspace_id, generated_version_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_generated_version_approval_history_action_by
    ON platform.generated_version_approval_history (action_by);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.day8.approval_download_share_revision', '44')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
