CREATE TABLE IF NOT EXISTS platform.image_creative_generations (
    id uuid PRIMARY KEY,
    workspace_id uuid NOT NULL,
    project_id uuid NOT NULL,
    creative_request_id uuid,
    brand_id uuid NOT NULL,
    product_service_id uuid,
    product_asset_id uuid,
    tool_code varchar(120) NOT NULL,
    creative_format varchar(40) NOT NULL,
    platform varchar(40) NOT NULL,
    language varchar(20) NOT NULL,
    quality_mode varchar(20) NOT NULL,
    requested_version_count integer NOT NULL,
    credit_cost numeric(19,4) NOT NULL,
    credit_reservation_id uuid,
    status varchar(20) NOT NULL,
    failure_reason varchar(1000),
    generated_version_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    request_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    created_by varchar(120),
    updated_by varchar(120),
    is_deleted boolean NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_image_creative_generations_workspace_project_created
    ON platform.image_creative_generations (workspace_id, project_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_image_creative_generations_creative_request
    ON platform.image_creative_generations (creative_request_id);

CREATE INDEX IF NOT EXISTS idx_image_creative_generations_product_asset
    ON platform.image_creative_generations (product_asset_id);
