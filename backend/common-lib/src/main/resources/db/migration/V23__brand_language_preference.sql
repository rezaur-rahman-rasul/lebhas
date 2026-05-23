ALTER TABLE platform.brands
    ADD COLUMN IF NOT EXISTS language_preference VARCHAR(20);

UPDATE platform.brands
SET language_preference = 'BOTH'
WHERE language_preference IS NULL
   OR BTRIM(language_preference) = '';

ALTER TABLE platform.brands
    ALTER COLUMN language_preference SET DEFAULT 'BOTH';

ALTER TABLE platform.brands
    ALTER COLUMN language_preference SET NOT NULL;

ALTER TABLE platform.brands
    DROP CONSTRAINT IF EXISTS chk_brands_language_preference;

ALTER TABLE platform.brands
    ADD CONSTRAINT chk_brands_language_preference
        CHECK (language_preference IN ('BANGLA', 'ENGLISH', 'BOTH'));

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '23')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
