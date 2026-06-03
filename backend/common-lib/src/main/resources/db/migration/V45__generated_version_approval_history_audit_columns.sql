ALTER TABLE platform.generated_version_approval_history
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE platform.generated_version_approval_history
SET is_deleted = COALESCE(deleted, FALSE)
WHERE is_deleted IS DISTINCT FROM COALESCE(deleted, FALSE);

CREATE INDEX IF NOT EXISTS idx_generated_version_approval_history_is_deleted
    ON platform.generated_version_approval_history (is_deleted);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.day8.generated_version_approval_history_audit_columns', '45')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
