CREATE TABLE IF NOT EXISTS platform.creative_templates (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(1000),
    platform VARCHAR(40),
    language VARCHAR(40),
    campaign_objective VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    master_template BOOLEAN NOT NULL DEFAULT FALSE,
    template_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_templates_workspace ON platform.creative_templates(workspace_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_creative_templates_master ON platform.creative_templates(master_template, active, is_deleted);
CREATE INDEX IF NOT EXISTS idx_creative_templates_category ON platform.creative_templates(category);

CREATE TABLE IF NOT EXISTS platform.campaign_packages (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    project_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(40) NOT NULL,
    r2_object_key VARCHAR(600),
    export_url VARCHAR(1000),
    export_url_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_campaign_packages_workspace_project ON platform.campaign_packages(workspace_id, project_id, is_deleted);

CREATE TABLE IF NOT EXISTS platform.campaign_package_items (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    campaign_package_id UUID NOT NULL,
    project_id UUID NOT NULL,
    item_type VARCHAR(40) NOT NULL,
    item_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_campaign_package_items_package ON platform.campaign_package_items(workspace_id, campaign_package_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_campaign_package_items_item ON platform.campaign_package_items(item_type, item_id);

CREATE TABLE IF NOT EXISTS platform.bulk_generation_jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    project_id UUID NOT NULL,
    generation_type VARCHAR(40) NOT NULL,
    platform VARCHAR(40),
    language VARCHAR(40),
    item_count INTEGER NOT NULL,
    estimated_credits NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    request_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_bulk_generation_jobs_workspace ON platform.bulk_generation_jobs(workspace_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_bulk_generation_jobs_project ON platform.bulk_generation_jobs(workspace_id, project_id, is_deleted);

CREATE TABLE IF NOT EXISTS platform.bulk_generation_items (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    job_id UUID NOT NULL,
    project_id UUID NOT NULL,
    source_id UUID,
    status VARCHAR(40) NOT NULL,
    item_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_bulk_generation_items_job ON platform.bulk_generation_items(workspace_id, job_id, is_deleted);
