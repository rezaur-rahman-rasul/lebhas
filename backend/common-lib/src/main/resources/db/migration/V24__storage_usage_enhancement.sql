ALTER TABLE platform.storage_usage
    ADD COLUMN IF NOT EXISTS raw_asset_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generated_asset_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS variant_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deleted_bytes BIGINT NOT NULL DEFAULT 0;

UPDATE platform.storage_usage
SET raw_asset_bytes = CASE
                          WHEN raw_asset_bytes = 0 AND generated_asset_bytes = 0 AND variant_bytes = 0
                              THEN GREATEST(total_bytes_used, 0)
                          ELSE raw_asset_bytes
    END,
    deleted_bytes = CASE
                        WHEN deleted_bytes = 0
                            THEN GREATEST(deleted_bytes_pending_cleanup, 0)
                        ELSE deleted_bytes
        END;

ALTER TABLE platform.storage_usage
    DROP CONSTRAINT IF EXISTS ck_storage_usage_non_negative;

ALTER TABLE platform.storage_usage
    ADD CONSTRAINT ck_storage_usage_non_negative
        CHECK (
            total_bytes_used >= 0
                AND raw_asset_bytes >= 0
                AND generated_asset_bytes >= 0
                AND variant_bytes >= 0
                AND deleted_bytes >= 0
        );

CREATE INDEX IF NOT EXISTS idx_storage_usage_last_calculated_at
    ON platform.storage_usage (last_calculated_at DESC);
