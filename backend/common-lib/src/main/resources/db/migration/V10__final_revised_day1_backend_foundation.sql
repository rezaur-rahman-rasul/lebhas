ALTER TABLE platform.users
    ADD COLUMN IF NOT EXISTS is_master BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE platform.users
SET is_master = (role = 'MASTER')
WHERE is_master IS DISTINCT FROM (role = 'MASTER');

ALTER TABLE platform.workspaces
    ADD COLUMN IF NOT EXISTS owner_user_id UUID REFERENCES platform.users (id);

UPDATE platform.workspaces
SET owner_user_id = owner_id
WHERE owner_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_workspaces_owner_user_id
    ON platform.workspaces (owner_user_id);

ALTER TABLE platform.workspace_memberships
    ADD COLUMN IF NOT EXISTS can_download_creative BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS can_edit_creative BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE platform.workspace_memberships membership
SET can_download_creative = TRUE
WHERE membership.role IN ('MASTER', 'ADMIN')
   OR EXISTS (
       SELECT 1
       FROM platform.workspace_membership_permissions permission
       WHERE permission.membership_id = membership.id
         AND permission.permission_code = 'CREATIVE_DOWNLOAD'
   );

UPDATE platform.workspace_memberships membership
SET can_edit_creative = TRUE
WHERE membership.role IN ('MASTER', 'ADMIN')
   OR EXISTS (
       SELECT 1
       FROM platform.workspace_membership_permissions permission
       WHERE permission.membership_id = membership.id
         AND permission.permission_code = 'CREATIVE_EDIT'
   );

INSERT INTO platform.permissions (code, description)
VALUES
    ('BRAND_MANAGE', 'Manage brands'),
    ('PRODUCT_SERVICE_MANAGE', 'Manage products and services'),
    ('PROJECT_CAMPAIGN_MANAGE', 'Manage projects and campaigns'),
    ('CREATIVE_REQUEST_MANAGE', 'Manage creative requests'),
    ('GENERATED_VERSION_MANAGE', 'Manage generated versions'),
    ('SUPPORT_WORKSPACE_ACCESS', 'Enter workspaces in support mode')
ON CONFLICT (code) DO NOTHING;

INSERT INTO platform.role_permissions (role_code, permission_code)
VALUES
    ('MASTER', 'BRAND_MANAGE'),
    ('MASTER', 'PRODUCT_SERVICE_MANAGE'),
    ('MASTER', 'PROJECT_CAMPAIGN_MANAGE'),
    ('MASTER', 'CREATIVE_REQUEST_MANAGE'),
    ('MASTER', 'GENERATED_VERSION_MANAGE'),
    ('MASTER', 'SUPPORT_WORKSPACE_ACCESS'),
    ('ADMIN', 'BRAND_MANAGE'),
    ('ADMIN', 'PRODUCT_SERVICE_MANAGE'),
    ('ADMIN', 'PROJECT_CAMPAIGN_MANAGE'),
    ('ADMIN', 'CREATIVE_REQUEST_MANAGE'),
    ('ADMIN', 'GENERATED_VERSION_MANAGE')
ON CONFLICT (role_code, permission_code) DO NOTHING;

CREATE TABLE IF NOT EXISTS platform.product_services (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID NOT NULL REFERENCES platform.brands (id) ON DELETE CASCADE,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(120),
    target_audience VARCHAR(240),
    selling_points TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_product_services_workspace_id ON platform.product_services (workspace_id);
CREATE INDEX IF NOT EXISTS idx_product_services_brand_id ON platform.product_services (brand_id);
CREATE INDEX IF NOT EXISTS idx_product_services_status ON platform.product_services (status);
CREATE INDEX IF NOT EXISTS idx_product_services_created_by ON platform.product_services (created_by);

INSERT INTO platform.product_services (
    id,
    workspace_id,
    brand_id,
    name,
    description,
    category,
    target_audience,
    selling_points,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    brand.id,
    brand.workspace_id,
    brand.id,
    brand.name || ' Core Offering',
    'Default product/service foundation for ' || brand.name,
    COALESCE(brand.business_type, 'GENERAL'),
    brand.target_audience,
    NULL,
    'ACTIVE',
    brand.created_at,
    brand.updated_at,
    brand.created_by,
    brand.updated_by,
    FALSE
FROM platform.brands brand
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.product_services product_service
    WHERE product_service.id = brand.id
);

CREATE TABLE IF NOT EXISTS platform.project_campaigns (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    brand_id UUID NOT NULL REFERENCES platform.brands (id) ON DELETE CASCADE,
    product_service_id UUID NOT NULL REFERENCES platform.product_services (id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES platform.users (id),
    name VARCHAR(140) NOT NULL,
    description VARCHAR(2000),
    campaign_objective VARCHAR(160),
    target_platform VARCHAR(120),
    campaign_type VARCHAR(120),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_project_campaigns_workspace_id ON platform.project_campaigns (workspace_id);
CREATE INDEX IF NOT EXISTS idx_project_campaigns_brand_id ON platform.project_campaigns (brand_id);
CREATE INDEX IF NOT EXISTS idx_project_campaigns_product_service_id ON platform.project_campaigns (product_service_id);
CREATE INDEX IF NOT EXISTS idx_project_campaigns_status ON platform.project_campaigns (status);
CREATE INDEX IF NOT EXISTS idx_project_campaigns_created_by ON platform.project_campaigns (created_by);
CREATE INDEX IF NOT EXISTS idx_project_campaigns_created_by_user_id ON platform.project_campaigns (created_by_user_id);

INSERT INTO platform.project_campaigns (
    id,
    workspace_id,
    brand_id,
    product_service_id,
    created_by_user_id,
    name,
    description,
    campaign_objective,
    target_platform,
    campaign_type,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    project.id,
    project.workspace_id,
    project.brand_id,
    project.brand_id,
    workspace.owner_user_id,
    project.name,
    project.description,
    project.campaign_objective,
    project.target_platform,
    'CAMPAIGN',
    'ACTIVE',
    project.created_at,
    project.updated_at,
    project.created_by,
    project.updated_by,
    project.is_deleted
FROM platform.projects project
JOIN platform.workspaces workspace ON workspace.id = project.workspace_id
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.project_campaigns campaign
    WHERE campaign.id = project.id
);

CREATE TABLE IF NOT EXISTS platform.creative_requests (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    project_campaign_id UUID NOT NULL REFERENCES platform.project_campaigns (id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES platform.users (id),
    request_name VARCHAR(180) NOT NULL,
    source_prompt TEXT NOT NULL,
    enhanced_prompt TEXT,
    creative_objective VARCHAR(160),
    target_platform VARCHAR(120),
    requested_format VARCHAR(120),
    status VARCHAR(30) NOT NULL,
    credit_reservation_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_creative_requests_workspace_id ON platform.creative_requests (workspace_id);
CREATE INDEX IF NOT EXISTS idx_creative_requests_project_campaign_id ON platform.creative_requests (project_campaign_id);
CREATE INDEX IF NOT EXISTS idx_creative_requests_requested_by ON platform.creative_requests (requested_by);
CREATE INDEX IF NOT EXISTS idx_creative_requests_status ON platform.creative_requests (status);
CREATE INDEX IF NOT EXISTS idx_creative_requests_created_by ON platform.creative_requests (created_by);

INSERT INTO platform.creative_requests (
    id,
    workspace_id,
    project_campaign_id,
    requested_by,
    request_name,
    source_prompt,
    enhanced_prompt,
    creative_objective,
    target_platform,
    requested_format,
    status,
    credit_reservation_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    request.id,
    request.workspace_id,
    request.project_id,
    request.user_id,
    COALESCE(NULLIF(LEFT(TRIM(request.source_prompt), 180), ''), 'Creative Request'),
    request.source_prompt,
    request.enhanced_prompt,
    request.campaign_objective,
    request.platform,
    request.output_format,
    CASE request.status
        WHEN 'PROCESSING' THEN 'PROCESSING'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        WHEN 'FAILED' THEN 'FAILED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'REQUESTED'
    END,
    NULL,
    request.created_at,
    request.updated_at,
    request.created_by,
    request.updated_by,
    request.is_deleted
FROM platform.creative_generation_requests request
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.creative_requests creative_request
    WHERE creative_request.id = request.id
);

CREATE TABLE IF NOT EXISTS platform.generated_versions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    creative_request_id UUID NOT NULL REFERENCES platform.creative_requests (id) ON DELETE CASCADE,
    project_campaign_id UUID NOT NULL REFERENCES platform.project_campaigns (id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    version_name VARCHAR(180),
    storage_file_id UUID REFERENCES platform.storage_files (id),
    generation_status VARCHAR(30) NOT NULL,
    approval_status VARCHAR(30) NOT NULL,
    editable_before_approval BOOLEAN NOT NULL DEFAULT TRUE,
    generated_by_provider VARCHAR(120),
    generated_by_model VARCHAR(160),
    created_by_user_id UUID NOT NULL REFERENCES platform.users (id),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_generated_versions_workspace_id ON platform.generated_versions (workspace_id);
CREATE INDEX IF NOT EXISTS idx_generated_versions_creative_request_id ON platform.generated_versions (creative_request_id);
CREATE INDEX IF NOT EXISTS idx_generated_versions_project_campaign_id ON platform.generated_versions (project_campaign_id);
CREATE INDEX IF NOT EXISTS idx_generated_versions_storage_file_id ON platform.generated_versions (storage_file_id);
CREATE INDEX IF NOT EXISTS idx_generated_versions_status ON platform.generated_versions (status);
CREATE INDEX IF NOT EXISTS idx_generated_versions_created_by ON platform.generated_versions (created_by);

WITH numbered_outputs AS (
    SELECT
        output.*,
        request.user_id,
        ROW_NUMBER() OVER (PARTITION BY output.request_id ORDER BY output.created_at ASC, output.id ASC) AS version_number
    FROM platform.creative_outputs output
    JOIN platform.creative_generation_requests request ON request.id = output.request_id
)
INSERT INTO platform.generated_versions (
    id,
    workspace_id,
    creative_request_id,
    project_campaign_id,
    version_number,
    version_name,
    storage_file_id,
    generation_status,
    approval_status,
    editable_before_approval,
    generated_by_provider,
    generated_by_model,
    created_by_user_id,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    output.id,
    output.workspace_id,
    output.request_id,
    output.project_id,
    output.version_number,
    'Version ' || output.version_number,
    output.storage_file_id,
    CASE output.status
        WHEN 'PROCESSING' THEN 'PROCESSING'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        WHEN 'FAILED' THEN 'FAILED'
        WHEN 'CANCELLED' THEN 'CANCELLED'
        ELSE 'QUEUED'
    END,
    'DRAFT',
    TRUE,
    request.ai_provider,
    request.ai_model,
    output.user_id,
    'ACTIVE',
    output.created_at,
    output.updated_at,
    output.created_by,
    output.updated_by,
    output.is_deleted
FROM numbered_outputs output
JOIN platform.creative_generation_requests request ON request.id = output.request_id
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.generated_versions generated_version
    WHERE generated_version.id = output.id
);

CREATE TABLE IF NOT EXISTS platform.credit_wallets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL UNIQUE REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_credit_wallets_workspace_id ON platform.credit_wallets (workspace_id);

INSERT INTO platform.credit_wallets (
    id,
    workspace_id,
    balance,
    reserved_balance,
    created_at,
    updated_at,
    created_by,
    updated_by,
    is_deleted
)
SELECT
    workspace.id,
    workspace.id,
    0,
    0,
    workspace.created_at,
    workspace.updated_at,
    COALESCE(workspace.created_by, 'system'),
    COALESCE(workspace.updated_by, 'system'),
    FALSE
FROM platform.workspaces workspace
WHERE NOT EXISTS (
    SELECT 1
    FROM platform.credit_wallets wallet
    WHERE wallet.workspace_id = workspace.id
);

CREATE TABLE IF NOT EXISTS platform.credit_transactions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reference_type VARCHAR(80),
    reference_id UUID,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_credit_transactions_workspace_id ON platform.credit_transactions (workspace_id);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_status ON platform.credit_transactions (status);

CREATE TABLE IF NOT EXISTS platform.public_share_links (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_public_share_links_token ON platform.public_share_links (token);
CREATE INDEX IF NOT EXISTS idx_public_share_links_workspace_id ON platform.public_share_links (workspace_id);
CREATE INDEX IF NOT EXISTS idx_public_share_links_generated_version_id ON platform.public_share_links (generated_version_id);

CREATE TABLE IF NOT EXISTS platform.download_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    downloaded_by UUID REFERENCES platform.users (id),
    download_type VARCHAR(60),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_download_logs_workspace_id ON platform.download_logs (workspace_id);
CREATE INDEX IF NOT EXISTS idx_download_logs_generated_version_id ON platform.download_logs (generated_version_id);

CREATE TABLE IF NOT EXISTS platform.usage_billing_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    generated_version_id UUID NOT NULL REFERENCES platform.generated_versions (id) ON DELETE CASCADE,
    usage_type VARCHAR(80),
    credit_transaction_id UUID REFERENCES platform.credit_transactions (id),
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_workspace_id ON platform.usage_billing_logs (workspace_id);
CREATE INDEX IF NOT EXISTS idx_usage_billing_logs_generated_version_id ON platform.usage_billing_logs (generated_version_id);

INSERT INTO platform.foundation_metadata (metadata_key, metadata_value)
VALUES ('schema.foundation.version', '10')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
