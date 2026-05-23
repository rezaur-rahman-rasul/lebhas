CREATE TABLE IF NOT EXISTS platform.payment_providers (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    provider_type VARCHAR(40) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sandbox_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    live_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payment_providers_code UNIQUE (code),
    CONSTRAINT chk_payment_providers_provider_type
        CHECK (provider_type IN ('SSLCOMMERZ', 'BKASH', 'NAGAD', 'STRIPE', 'MANUAL')),
    CONSTRAINT chk_payment_providers_code_not_blank
        CHECK (LENGTH(BTRIM(code)) > 0),
    CONSTRAINT chk_payment_providers_name_not_blank
        CHECK (LENGTH(BTRIM(name)) > 0),
    CONSTRAINT chk_payment_providers_priority_nonnegative
        CHECK (priority >= 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_providers_provider_type
    ON platform.payment_providers (provider_type);

CREATE INDEX IF NOT EXISTS idx_payment_providers_enabled_priority
    ON platform.payment_providers (is_enabled, priority);

CREATE INDEX IF NOT EXISTS idx_payment_providers_sandbox_enabled
    ON platform.payment_providers (sandbox_enabled);

CREATE INDEX IF NOT EXISTS idx_payment_providers_live_enabled
    ON platform.payment_providers (live_enabled);

CREATE TABLE IF NOT EXISTS platform.payment_provider_configurations (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.payment_providers (id) ON DELETE CASCADE,
    environment_type VARCHAR(20) NOT NULL,
    api_base_url VARCHAR(500),
    merchant_id VARCHAR(255),
    encrypted_api_key VARCHAR(2000),
    encrypted_secret VARCHAR(2000),
    encrypted_webhook_secret VARCHAR(2000),
    success_url VARCHAR(1000),
    failure_url VARCHAR(1000),
    cancel_url VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payment_provider_config_provider_environment UNIQUE (provider_id, environment_type),
    CONSTRAINT chk_payment_provider_config_environment
        CHECK (environment_type IN ('SANDBOX', 'LIVE'))
);

CREATE INDEX IF NOT EXISTS idx_payment_provider_config_provider_id
    ON platform.payment_provider_configurations (provider_id);

CREATE INDEX IF NOT EXISTS idx_payment_provider_config_environment
    ON platform.payment_provider_configurations (environment_type);

CREATE INDEX IF NOT EXISTS idx_payment_provider_config_active
    ON platform.payment_provider_configurations (is_active);

CREATE TABLE IF NOT EXISTS platform.payment_transactions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE RESTRICT,
    provider_id UUID NOT NULL REFERENCES platform.payment_providers (id) ON DELETE RESTRICT,
    payment_purpose VARCHAR(60) NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider_transaction_id VARCHAR(255),
    provider_session_id VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(1000),
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_transactions_purpose
        CHECK (payment_purpose IN ('SUBSCRIPTION_PURCHASE', 'PLAN_UPGRADE', 'PLAN_RENEWAL', 'CREDIT_PURCHASE')),
    CONSTRAINT chk_payment_transactions_status
        CHECK (status IN ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
    CONSTRAINT chk_payment_transactions_amount_nonnegative
        CHECK (amount >= 0),
    CONSTRAINT chk_payment_transactions_currency_length
        CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT chk_payment_transactions_reference_type_not_blank
        CHECK (LENGTH(BTRIM(reference_type)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_workspace_created_at
    ON platform.payment_transactions (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_user_id
    ON platform.payment_transactions (user_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_id
    ON platform.payment_transactions (provider_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_purpose_status
    ON platform.payment_transactions (payment_purpose, status);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_reference
    ON platform.payment_transactions (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_transaction_id
    ON platform.payment_transactions (provider_transaction_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_session_id
    ON platform.payment_transactions (provider_session_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_provider_transaction
    ON platform.payment_transactions (provider_id, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_provider_session
    ON platform.payment_transactions (provider_id, provider_session_id)
    WHERE provider_session_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.subscription_orders (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    pricing_plan_id UUID NOT NULL REFERENCES platform.pricing_plans (id) ON DELETE RESTRICT,
    requested_by UUID NOT NULL REFERENCES platform.users (id) ON DELETE RESTRICT,
    billing_cycle VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_transaction_id UUID REFERENCES platform.payment_transactions (id) ON DELETE SET NULL,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscription_orders_billing_cycle
        CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_subscription_orders_status
        CHECK (status IN ('CREATED', 'PAYMENT_PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_subscription_orders_amount_nonnegative
        CHECK (amount >= 0),
    CONSTRAINT chk_subscription_orders_currency_length
        CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT chk_subscription_orders_dates
        CHECK (expires_at IS NULL OR starts_at IS NULL OR expires_at >= starts_at)
);

CREATE INDEX IF NOT EXISTS idx_subscription_orders_workspace_created_at
    ON platform.subscription_orders (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_orders_pricing_plan_id
    ON platform.subscription_orders (pricing_plan_id);

CREATE INDEX IF NOT EXISTS idx_subscription_orders_requested_by
    ON platform.subscription_orders (requested_by);

CREATE INDEX IF NOT EXISTS idx_subscription_orders_payment_transaction_id
    ON platform.subscription_orders (payment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_subscription_orders_status
    ON platform.subscription_orders (status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_orders_payment_transaction
    ON platform.subscription_orders (payment_transaction_id)
    WHERE payment_transaction_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.credit_packages (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    credits BIGINT NOT NULL,
    bonus_credits BIGINT NOT NULL DEFAULT 0,
    price NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_credit_packages_code UNIQUE (code),
    CONSTRAINT chk_credit_packages_name_not_blank
        CHECK (LENGTH(BTRIM(name)) > 0),
    CONSTRAINT chk_credit_packages_code_not_blank
        CHECK (LENGTH(BTRIM(code)) > 0),
    CONSTRAINT chk_credit_packages_nonnegative
        CHECK (credits >= 0 AND bonus_credits >= 0 AND price >= 0 AND sort_order >= 0),
    CONSTRAINT chk_credit_packages_currency_length
        CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_credit_packages_active_sort
    ON platform.credit_packages (is_active, sort_order);

CREATE INDEX IF NOT EXISTS idx_credit_packages_currency
    ON platform.credit_packages (currency);

CREATE TABLE IF NOT EXISTS platform.credit_purchase_orders (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    credit_package_id UUID NOT NULL REFERENCES platform.credit_packages (id) ON DELETE RESTRICT,
    requested_by UUID NOT NULL REFERENCES platform.users (id) ON DELETE RESTRICT,
    credits BIGINT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_transaction_id UUID REFERENCES platform.payment_transactions (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_credit_purchase_orders_status
        CHECK (status IN ('CREATED', 'PAYMENT_PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_credit_purchase_orders_nonnegative
        CHECK (credits >= 0 AND amount >= 0),
    CONSTRAINT chk_credit_purchase_orders_currency_length
        CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_credit_purchase_orders_workspace_created_at
    ON platform.credit_purchase_orders (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_credit_purchase_orders_credit_package_id
    ON platform.credit_purchase_orders (credit_package_id);

CREATE INDEX IF NOT EXISTS idx_credit_purchase_orders_requested_by
    ON platform.credit_purchase_orders (requested_by);

CREATE INDEX IF NOT EXISTS idx_credit_purchase_orders_payment_transaction_id
    ON platform.credit_purchase_orders (payment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_credit_purchase_orders_status
    ON platform.credit_purchase_orders (status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_credit_purchase_orders_payment_transaction
    ON platform.credit_purchase_orders (payment_transaction_id)
    WHERE payment_transaction_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS platform.invoices (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    payment_transaction_id UUID NOT NULL REFERENCES platform.payment_transactions (id) ON DELETE RESTRICT,
    invoice_number VARCHAR(80) NOT NULL,
    invoice_type VARCHAR(40) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT chk_invoices_invoice_type
        CHECK (invoice_type IN ('SUBSCRIPTION', 'CREDIT_PURCHASE')),
    CONSTRAINT chk_invoices_status
        CHECK (status IN ('ISSUED', 'PAID', 'VOID', 'CANCELLED')),
    CONSTRAINT chk_invoices_amount_nonnegative
        CHECK (amount >= 0),
    CONSTRAINT chk_invoices_currency_length
        CHECK (CHAR_LENGTH(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_invoices_workspace_created_at
    ON platform.invoices (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_invoices_payment_transaction_id
    ON platform.invoices (payment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_invoices_invoice_type_status
    ON platform.invoices (invoice_type, status);

CREATE INDEX IF NOT EXISTS idx_invoices_issued_at
    ON platform.invoices (issued_at DESC);

CREATE TABLE IF NOT EXISTS platform.payment_webhook_logs (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES platform.payment_providers (id) ON DELETE CASCADE,
    provider_transaction_id VARCHAR(255),
    webhook_event_type VARCHAR(120) NOT NULL,
    request_payload TEXT NOT NULL,
    signature_hash VARCHAR(255) NOT NULL,
    verification_status VARCHAR(30) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_payment_webhook_logs_signature UNIQUE (provider_id, signature_hash),
    CONSTRAINT chk_payment_webhook_logs_event_type_not_blank
        CHECK (LENGTH(BTRIM(webhook_event_type)) > 0),
    CONSTRAINT chk_payment_webhook_logs_signature_not_blank
        CHECK (LENGTH(BTRIM(signature_hash)) > 0),
    CONSTRAINT chk_payment_webhook_logs_verification_status
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_provider_created_at
    ON platform.payment_webhook_logs (provider_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_provider_transaction_id
    ON platform.payment_webhook_logs (provider_transaction_id);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_event_type
    ON platform.payment_webhook_logs (webhook_event_type);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_processed
    ON platform.payment_webhook_logs (processed);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_logs_verification_status
    ON platform.payment_webhook_logs (verification_status);
