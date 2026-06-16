ALTER TABLE platform.registration_sessions
    ADD COLUMN IF NOT EXISTS selected_brand_id UUID,
    ADD COLUMN IF NOT EXISTS selected_product_service_id UUID,
    ADD COLUMN IF NOT EXISTS selected_project_campaign_id UUID;

CREATE INDEX IF NOT EXISTS idx_brands_workspace_lower_name_active
    ON platform.brands (workspace_id, lower(name))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_product_services_workspace_brand_lower_name_active
    ON platform.product_services (workspace_id, brand_id, lower(name))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_project_campaigns_workspace_brand_product_lower_name_active
    ON platform.project_campaigns (workspace_id, brand_id, product_service_id, lower(name))
    WHERE is_deleted = false;
