ALTER TABLE platform.generated_versions
    ALTER COLUMN creative_request_id DROP NOT NULL;

DROP INDEX IF EXISTS uk_generated_versions_request_version_number;
DROP INDEX IF EXISTS uk_generated_versions_workspace_request_version_number;

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_request_version_number
    ON platform.generated_versions (creative_request_id, version_number)
    WHERE is_deleted = FALSE AND creative_request_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_workspace_request_version_number
    ON platform.generated_versions (workspace_id, creative_request_id, version_number)
    WHERE is_deleted = FALSE AND creative_request_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_generated_versions_prompt_request_version_number
    ON platform.generated_versions (workspace_id, prompt_request_id, version_number)
    WHERE is_deleted = FALSE AND prompt_request_id IS NOT NULL;
