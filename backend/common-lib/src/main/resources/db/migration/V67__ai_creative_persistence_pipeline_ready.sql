ALTER TABLE platform.creative_generation_jobs
    ADD COLUMN IF NOT EXISTS r2_object_key VARCHAR(600),
    ADD COLUMN IF NOT EXISTS preview_url VARCHAR(1200),
    ADD COLUMN IF NOT EXISTS download_url VARCHAR(1200);

ALTER TABLE platform.creative_generation_jobs
    DROP CONSTRAINT IF EXISTS chk_creative_generation_jobs_status;

ALTER TABLE platform.creative_generation_jobs
    ADD CONSTRAINT chk_creative_generation_jobs_status
        CHECK (status IN (
            'QUEUED',
            'PLANNING',
            'PROCESSING',
            'GENERATING',
            'DOWNLOADING',
            'UPLOADING',
            'READY',
            'COMPLETED',
            'FAILED',
            'CANCELLED'
        ));

ALTER TABLE platform.generated_versions
    ADD COLUMN IF NOT EXISTS preview_url VARCHAR(1200),
    ADD COLUMN IF NOT EXISTS download_url VARCHAR(1200),
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(600),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS generation_metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE platform.generated_versions
    DROP CONSTRAINT IF EXISTS chk_generated_versions_generation_status;

ALTER TABLE platform.generated_versions
    ADD CONSTRAINT chk_generated_versions_generation_status
        CHECK (generation_status IN (
            'QUEUED',
            'PROCESSING',
            'GENERATING',
            'DOWNLOADING',
            'UPLOADING',
            'READY',
            'FAILED',
            'COMPLETED',
            'CANCELLED'
        ));

CREATE INDEX IF NOT EXISTS idx_creative_generation_jobs_workspace_status_created
    ON platform.creative_generation_jobs (workspace_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_ready_asset
    ON platform.generated_versions (workspace_id, generation_status, generated_asset_id)
    WHERE is_deleted = FALSE AND generated_asset_id IS NOT NULL;

UPDATE platform.generated_versions generated_version
SET storage_key = COALESCE(generated_version.storage_key, generated_version.r2_object_key),
    preview_url = COALESCE(generated_version.preview_url, generated_version.file_url),
    download_url = COALESCE(generated_version.download_url, generated_version.file_url)
WHERE generated_version.prompt_request_id IS NOT NULL;

