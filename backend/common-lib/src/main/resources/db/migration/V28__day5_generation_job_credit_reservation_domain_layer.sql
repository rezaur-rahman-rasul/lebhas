ALTER TABLE platform.generation_jobs
    ADD COLUMN IF NOT EXISTS creative_request_id UUID,
    ADD COLUMN IF NOT EXISTS provider VARCHAR(120),
    ADD COLUMN IF NOT EXISTS model VARCHAR(160),
    ADD COLUMN IF NOT EXISTS queued_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failure_reason TEXT;

UPDATE platform.generation_jobs
SET status = 'QUEUED'
WHERE status = 'DRAFT';

UPDATE platform.generation_jobs generation_job
SET creative_request_id = creative_request.id
FROM platform.creative_requests creative_request
WHERE generation_job.request_id = creative_request.id
  AND generation_job.creative_request_id IS NULL;

UPDATE platform.generation_jobs generation_job
SET provider = COALESCE(generation_job.provider, NULLIF(generation_request.ai_provider, '')),
    model = COALESCE(generation_job.model, NULLIF(generation_request.ai_model, '')),
    queued_at = COALESCE(generation_job.queued_at, generation_job.created_at),
    processing_started_at = COALESCE(generation_job.processing_started_at, generation_job.started_at),
    retry_count = COALESCE(generation_job.retry_count, GREATEST(generation_job.attempt_count - 1, 0)),
    failure_reason = COALESCE(generation_job.failure_reason, NULLIF(generation_job.error_message, ''))
FROM platform.creative_generation_requests generation_request
WHERE generation_job.request_id = generation_request.id;

UPDATE platform.generation_jobs
SET queued_at = COALESCE(queued_at, created_at),
    retry_count = COALESCE(retry_count, 0);

ALTER TABLE platform.generation_jobs
    ALTER COLUMN creative_request_id SET NOT NULL,
    ALTER COLUMN queued_at SET NOT NULL,
    ALTER COLUMN request_id DROP NOT NULL,
    ALTER COLUMN job_type DROP NOT NULL,
    ALTER COLUMN queue_name DROP NOT NULL;

ALTER TABLE platform.generation_jobs
    DROP CONSTRAINT IF EXISTS fk_generation_jobs_creative_request_id;

ALTER TABLE platform.generation_jobs
    ADD CONSTRAINT fk_generation_jobs_creative_request_id
        FOREIGN KEY (creative_request_id) REFERENCES platform.creative_requests (id) ON DELETE CASCADE;

ALTER TABLE platform.generation_jobs
    DROP CONSTRAINT IF EXISTS chk_generation_jobs_status;

ALTER TABLE platform.generation_jobs
    ADD CONSTRAINT chk_generation_jobs_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'));

ALTER TABLE platform.generation_jobs
    DROP CONSTRAINT IF EXISTS chk_generation_jobs_retry_count;

ALTER TABLE platform.generation_jobs
    ADD CONSTRAINT chk_generation_jobs_retry_count
        CHECK (retry_count >= 0);

CREATE INDEX IF NOT EXISTS idx_generation_jobs_creative_request_id
    ON platform.generation_jobs (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_creative_request_created_at
    ON platform.generation_jobs (workspace_id, creative_request_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_status_queued_at
    ON platform.generation_jobs (workspace_id, status, queued_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_provider_status
    ON platform.generation_jobs (workspace_id, provider, status)
    WHERE is_deleted = FALSE AND provider IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.credit_reservations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    creative_request_id UUID NOT NULL REFERENCES platform.creative_requests (id) ON DELETE CASCADE,
    reserved_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    finalized_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    refunded_credits NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    finalized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_credit_reservations_status
        CHECK (status IN ('RESERVED', 'FINALIZED', 'REFUNDED', 'CANCELLED')),
    CONSTRAINT chk_credit_reservations_reserved_credits
        CHECK (reserved_credits >= 0),
    CONSTRAINT chk_credit_reservations_finalized_credits
        CHECK (finalized_credits >= 0),
    CONSTRAINT chk_credit_reservations_refunded_credits
        CHECK (refunded_credits >= 0),
    CONSTRAINT chk_credit_reservations_credit_totals
        CHECK ((finalized_credits + refunded_credits) <= reserved_credits)
);

CREATE INDEX IF NOT EXISTS idx_credit_reservations_workspace_id
    ON platform.credit_reservations (workspace_id);

CREATE INDEX IF NOT EXISTS idx_credit_reservations_creative_request_id
    ON platform.credit_reservations (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_credit_reservations_workspace_request_created_at
    ON platform.credit_reservations (workspace_id, creative_request_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_credit_reservations_workspace_status_created_at
    ON platform.credit_reservations (workspace_id, status, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_credit_reservations_finalized_at
    ON platform.credit_reservations (finalized_at)
    WHERE finalized_at IS NOT NULL;

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '28')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
